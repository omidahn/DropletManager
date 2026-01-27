package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.ui.viewmodel.HostKeyPrompt
import com.omiddd.dropletmanager.ui.viewmodel.SshConsoleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshConsoleScreen(
    viewModel: SshConsoleViewModel,
    defaultHost: String? = null,
    defaultUser: String? = "root",
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    var errorOverlay by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.error) { if (state.error != null) errorOverlay = state.error }
    LaunchedEffect(errorOverlay) {
        val msg = errorOverlay
        if (msg != null && !msg.startsWith("Permission denied", ignoreCase = true)) {
            kotlinx.coroutines.delay(3000)
            if (errorOverlay == msg) errorOverlay = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Console") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.connected) {
                            viewModel.disconnect()
                        }
                        onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.connected) {
            ConnectForm(
                connecting = state.connecting,
                inlineError = null,
                defaultHost = defaultHost,
                defaultUser = defaultUser,
                modifier = Modifier.padding(padding)
            ) { host, port, user, password, key, passphrase ->
                viewModel.connect(host, port, user, password, key, passphrase)
            }
        } else {
            ConsoleView(
                output = state.output,
                onSend = { viewModel.sendCommand(it) },
                onCtrlC = { viewModel.sendCtrlC() },
                modifier = Modifier.padding(padding)
            )
        }
    }

    // Centered overlay for connection errors
    AnimatedVisibility(
        visible = errorOverlay != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 10 })
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(tonalElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Error", style = MaterialTheme.typography.titleLarge)
                    Text(errorOverlay ?: "")
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { errorOverlay = null }) { Text("OK") }
                    }
                }
            }
        }
    }

    state.hostKeyPrompt?.let { prompt ->
        HostKeyDialog(prompt = prompt, onTrust = { viewModel.acceptHostKey(prompt) }, onCancel = { viewModel.rejectHostKey() })
    }
}

@Composable
private fun ConnectForm(
    connecting: Boolean,
    inlineError: String?,
    defaultHost: String?,
    defaultUser: String?,
    modifier: Modifier = Modifier,
    onConnect: (host: String, port: Int, user: String, password: String?, key: ByteArray?, passphrase: String?) -> Unit
) {
    var host by remember { mutableStateOf(defaultHost ?: "") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf(defaultUser ?: "root") }
    var authMethod by remember { mutableStateOf("Password") }
    var password by remember { mutableStateOf("") }
    var keyText by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("SSH Console", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host/IP") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = authMethod == "Password",
                onClick = { authMethod = "Password" },
                label = { Text("Password") }
            )
            FilterChip(
                selected = authMethod == "Key",
                onClick = { authMethod = "Key" },
                label = { Text("Private Key") }
            )
        }
        if (authMethod == "Password") {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = { Text("Private Key (PEM)") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            onClick = {
                val p = port.toIntOrNull() ?: 22
                val keyBytes = if (authMethod == "Key" && keyText.isNotBlank()) keyText.toByteArray() else null
                onConnect(host.trim(), p, user.trim(), if (authMethod == "Password") password else null, keyBytes, passphrase.ifBlank { null })
            }, enabled = !connecting && host.isNotBlank()
        ) { Text(if (connecting) "Connecting..." else "Connect") }
    }
}

@Composable
private fun HostKeyDialog(
    prompt: HostKeyPrompt,
    onTrust: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Untrusted Host Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Host: ${prompt.host}")
                Text("Algorithm: ${prompt.algorithm}")
                Text("Fingerprint: ${prompt.fingerprint}")
                Text("Confirm the fingerprint with your server before proceeding.")
            }
        },
        confirmButton = {
            TextButton(onClick = onTrust) {
                Text("Trust & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConsoleView(
    output: String,
    onSend: (String) -> Unit,
    onCtrlC: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    val scroll = rememberScrollState()

    LaunchedEffect(output) {
        // Auto scroll to bottom on new output
        scroll.scrollTo(scroll.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            text = output,
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll),
            style = TextStyle(fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onBackground)
        )
        Spacer(Modifier.height(8.dp))
        // Bottom action area sticks above the keyboard
        Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().imePadding()) {
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (input.isNotBlank()) { onSend(input); input = "" }
                        })
                    )
                    Button(onClick = { if (input.isNotBlank()) { onSend(input); input = "" } }) { Text("Send") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCtrlC) { Text("Ctrl+C") }
                }
            }
        }
    }
}
