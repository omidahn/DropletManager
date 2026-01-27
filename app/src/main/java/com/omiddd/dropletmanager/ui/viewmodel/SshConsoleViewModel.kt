package com.omiddd.dropletmanager.ui.viewmodel

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.omiddd.dropletmanager.utils.KnownHostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class ConsoleUiState(
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val output: String = "",
    val error: String? = null,
    val hostKeyPrompt: HostKeyPrompt? = null
)

data class HostKeyPrompt(
    val host: String,
    val port: Int,
    val algorithm: String,
    val fingerprint: String,
    val keyBase64: String
)

class SshConsoleViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ConsoleUiState())
    val state: StateFlow<ConsoleUiState> = _state

    private var session: Session? = null
    private var channel: ChannelShell? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val outputBuffer = StringBuilder()
    private val knownHosts = KnownHostRepository(application)

    private data class ConnectParams(
        val host: String,
        val port: Int,
        val username: String,
        val password: String?,
        val privateKey: ByteArray?,
        val passphrase: String?
    )

    private var pendingParams: ConnectParams? = null

    companion object {
        private const val TAG = "SshConsoleViewModel"
        private const val MAX_OUTPUT_CHARS = 100_000
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    fun connect(
        host: String,
        port: Int,
        username: String,
        password: String?,
        privateKey: ByteArray?,
        passphrase: String?
    ) {
        val params = ConnectParams(host, port, username, password, privateKey, passphrase)
        pendingParams = params
        if (state.value.connecting || state.value.connected) return
        connectWithParams(params, force = false)
    }

    fun acceptHostKey(prompt: HostKeyPrompt) {
        knownHosts.saveHostKey(
            host = prompt.host,
            port = prompt.port,
            algorithm = prompt.algorithm,
            keyBytes = Base64.decode(prompt.keyBase64, Base64.NO_WRAP),
            fingerprint = prompt.fingerprint
        )
        val params = pendingParams ?: return
        connectWithParams(params, force = true)
    }

    fun rejectHostKey() {
        _state.value = ConsoleUiState(error = "Host key not trusted; connection aborted")
        pendingParams = null
        disconnect()
    }

    private fun connectWithParams(params: ConnectParams, force: Boolean) {
        if (!force && (state.value.connecting || state.value.connected)) return

        outputBuffer.clear()
        _state.value = ConsoleUiState(connecting = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hostVerificationResult = probeAndVerifyHostKey(params)
                when (hostVerificationResult) {
                    is HostVerificationResult.Trusted -> { /* continue */ }
                    is HostVerificationResult.Unknown -> {
                        withContext(Dispatchers.Main) {
                            _state.value = ConsoleUiState(hostKeyPrompt = hostVerificationResult.prompt)
                        }
                        return@launch
                    }
                    is HostVerificationResult.Mismatch -> {
                        withContext(Dispatchers.Main) {
                            _state.value = ConsoleUiState(error = hostVerificationResult.message)
                        }
                        return@launch
                    }
                }

                val jsch = JSch().apply {
                    setHostKeyRepository(AppHostKeyRepository(knownHosts))
                    if (params.privateKey != null && params.privateKey.isNotEmpty()) {
                        addIdentity("key", params.privateKey, null, params.passphrase?.toByteArray())
                    }
                }

                session = jsch.getSession(params.username, params.host, params.port).also {
                    params.password?.let { pass -> it.setPassword(pass) }
                    it.setHostKeyAlias(hostKeyAlias(params.host, params.port))
                    it.setConfig("StrictHostKeyChecking", "yes")
                    it.connect(15000)
                }

                channel = (session?.openChannel("shell") as? ChannelShell)?.also {
                    it.setPty(true)
                    it.setPtyType("vt100")
                    input = it.inputStream
                    output = it.outputStream
                    it.connect(5000)
                }

                if (channel?.isConnected == true) {
                    val ts = java.time.ZonedDateTime.now().format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    appendOutput("\n[Connected to ${params.username}@${params.host} at $ts]\n")
                    output?.write("\r\n".toByteArray())
                    output?.flush()
                    withContext(Dispatchers.Main) {
                        pendingParams = null
                        _state.value = ConsoleUiState(connected = true)
                    }
                    readFromConsole()
                } else {
                    throw Exception("Failed to connect shell channel.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                withContext(Dispatchers.Main) {
                    _state.value = ConsoleUiState(error = e.message ?: "Connection error")
                }
                closeCurrentSession()
            }
        }
    }

    private sealed interface HostVerificationResult {
        object Trusted : HostVerificationResult
        data class Unknown(val prompt: HostKeyPrompt) : HostVerificationResult
        data class Mismatch(val message: String) : HostVerificationResult
    }

    private fun probeAndVerifyHostKey(params: ConnectParams): HostVerificationResult {
        val probeJsch = JSch()
        val probeSession = probeJsch.getSession(params.username, params.host, params.port).also {
            it.setHostKeyAlias(hostKeyAlias(params.host, params.port))
            it.setConfig("StrictHostKeyChecking", "no")
            it.setConfig("PreferredAuthentications", "none")
        }

        val hostKey = try {
            runCatching { probeSession.connect(15000) }
            probeSession.hostKey
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to read host key for ${params.host}:${params.port}", t)
            null
        } finally {
            runCatching { probeSession.disconnect() }
        } ?: return HostVerificationResult.Trusted

        val fingerprint = runCatching { hostKey.getFingerPrint(probeJsch) }.getOrNull()
        val keyBytes = extractKeyBytes(hostKey)
        if (keyBytes == null || fingerprint == null) {
            return HostVerificationResult.Trusted
        }

        val stored = knownHosts.getHostKey(params.host, params.port)
        val keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)

        return when {
            stored == null -> HostVerificationResult.Unknown(
                HostKeyPrompt(
                    host = params.host,
                    port = params.port,
                    algorithm = hostKey.type,
                    fingerprint = fingerprint,
                    keyBase64 = keyBase64
                )
            )
            stored.matches(hostKey.type, keyBase64) -> HostVerificationResult.Trusted
            else -> HostVerificationResult.Mismatch(
                "Host key mismatch for ${params.host}:${params.port}. Expected ${stored.fingerprint}, but received $fingerprint"
            )
        }
    }

    private fun extractKeyBytes(hostKey: HostKey): ByteArray? {
        return runCatching {
            val method = hostKey.javaClass.getMethod("getKey")
            method.invoke(hostKey) as? ByteArray
        }.getOrNull() ?: runCatching {
            val method = hostKey.javaClass.getMethod("getHostKey")
            method.invoke(hostKey) as? ByteArray
        }.getOrNull()
    }

    private fun closeCurrentSession() {
        runCatching { output?.close() }
        runCatching { input?.close() }
        runCatching { channel?.disconnect() }
        runCatching { session?.disconnect() }
        output = null
        input = null
        channel = null
        session = null
    }

    private suspend fun readFromConsole() {
        val buffer = ByteArray(8192)
        try {
            while (currentCoroutineContext().isActive && channel?.isConnected == true) {
                val bytesRead = withContext(Dispatchers.IO) {
                    input?.read(buffer)
                } ?: -1
                
                if (bytesRead <= 0) break

                val outputText = String(buffer, 0, bytesRead, Charsets.UTF_8)
                appendOutput(outputText)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during SSH read loop", e)
        } finally {
            withContext(Dispatchers.Main) {
                if (state.value.connected) {
                    appendOutput("\n[Disconnected]\n")
                }
                disconnect()
            }
        }
    }

    fun sendCommand(command: String) {
        val out = output ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                out.write((command + "\n").toByteArray(Charsets.UTF_8))
                out.flush()
            }.onFailure { e ->
                Log.e(TAG, "Failed to send command", e)
            }
        }
    }

    fun sendCtrlC() {
        val out = output ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                out.write(byteArrayOf(3)) // ETX for Ctrl+C
                out.flush()
            }.onFailure { e ->
                Log.e(TAG, "Failed to send Ctrl+C", e)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            closeCurrentSession()

            withContext(Dispatchers.Main) {
                if (state.value.connected || state.value.connecting) {
                    _state.value = ConsoleUiState()
                }
            }
        }
    }

    private fun appendOutput(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            if (text.isEmpty()) return@launch
            outputBuffer.append(text)
            if (outputBuffer.length > MAX_OUTPUT_CHARS) {
                val removeCount = outputBuffer.length - MAX_OUTPUT_CHARS
                outputBuffer.delete(0, removeCount)
            }
            _state.value = _state.value.copy(output = outputBuffer.toString())
        }
    }

    private fun hostKeyAlias(host: String, port: Int): String {
        return if (host.contains(":")) "[${host}]:$port" else "$host:$port"
    }

    private class AppHostKeyRepository(
        private val knownHosts: KnownHostRepository
    ) : HostKeyRepository {
        override fun check(host: String, key: ByteArray): Int {
            val (parsedHost, parsedPort) = parseHostAndPort(host)
            val stored = knownHosts.getHostKey(parsedHost, parsedPort) ?: return HostKeyRepository.NOT_INCLUDED
            val keyBase64 = Base64.encodeToString(key, Base64.NO_WRAP)
            return if (stored.matchesKey(keyBase64)) HostKeyRepository.OK else HostKeyRepository.CHANGED
        }

        override fun add(hostkey: HostKey, userinfo: com.jcraft.jsch.UserInfo?) {
            val (parsedHost, parsedPort) = parseHostAndPort(hostkey.host)
            val keyBytes = extractHostKeyBytes(hostkey) ?: return
            knownHosts.saveHostKey(
                host = parsedHost,
                port = parsedPort,
                algorithm = hostkey.type,
                keyBytes = keyBytes,
                fingerprint = runCatching { hostkey.getFingerPrint(JSch()) }.getOrNull() ?: ""
            )
        }

        override fun remove(host: String, type: String) {
            val (parsedHost, parsedPort) = parseHostAndPort(host)
            knownHosts.removeHostKey(parsedHost, parsedPort)
        }

        override fun remove(host: String, type: String, key: ByteArray) {
            val (parsedHost, parsedPort) = parseHostAndPort(host)
            knownHosts.removeHostKey(parsedHost, parsedPort)
        }

        override fun getHostKey(): Array<HostKey> = emptyArray()

        override fun getHostKey(host: String, type: String): Array<HostKey> = emptyArray()

        override fun getKnownHostsRepositoryID(): String = "app-known-hosts"

        private fun parseHostAndPort(host: String): Pair<String, Int> {
            val trimmed = host.trim()
            if (trimmed.startsWith("[") && trimmed.contains("]:")) {
                val closing = trimmed.indexOf("]:")
                val h = trimmed.substring(1, closing)
                val p = trimmed.substring(closing + 2).toIntOrNull() ?: 22
                return h to p
            }
            val lastColon = trimmed.lastIndexOf(':')
            if (lastColon > 0 && lastColon < trimmed.length - 1) {
                val port = trimmed.substring(lastColon + 1).toIntOrNull()
                if (port != null) {
                    return trimmed.substring(0, lastColon) to port
                }
            }
            return trimmed to 22
        }

        private fun extractHostKeyBytes(hostkey: HostKey): ByteArray? {
            return runCatching {
                val method = hostkey.javaClass.getMethod("getKey")
                method.invoke(hostkey) as? ByteArray
            }.getOrNull()
        }
    }
}
