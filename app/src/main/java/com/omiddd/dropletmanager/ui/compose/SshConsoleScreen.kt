package com.omiddd.dropletmanager.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
    LaunchedEffect(state.error) {
        if (state.error != null) errorOverlay = state.error
    }
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
                    IconButton(
                        onClick = {
                            if (state.connected) viewModel.disconnect()
                            onClose()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.connected) {
            SshConnectForm(
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

    AnimatedVisibility(
        visible = errorOverlay != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 10 })
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(tonalElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Error", style = MaterialTheme.typography.titleLarge)
                    Text(errorOverlay ?: "")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = { errorOverlay = null }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    state.hostKeyPrompt?.let { prompt ->
        HostKeyDialog(
            prompt = prompt,
            onTrust = { viewModel.acceptHostKey(prompt) },
            onCancel = { viewModel.rejectHostKey() }
        )
    }
}

@Composable
internal fun SshConnectForm(
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
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("SSH Console", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Connect directly to a droplet with a password or a private key. Host and user can be prefilled from the droplet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Connection Details", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host/IP") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ssh_host")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.35f)
                            .testTag("ssh_port")
                    )
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("ssh_user")
                    )
                }
            }
        }

        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (authMethod == "Password") Icons.Default.Lock else Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text("Authentication", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = authMethod == "Password",
                        onClick = { authMethod = "Password" },
                        label = { Text("Password") },
                        modifier = Modifier.testTag("ssh_auth_password")
                    )
                    FilterChip(
                        selected = authMethod == "Key",
                        onClick = { authMethod = "Key" },
                        label = { Text("Private Key") },
                        modifier = Modifier.testTag("ssh_auth_key")
                    )
                }
                Text(
                    text = if (authMethod == "Password") {
                        "Use the droplet's SSH password."
                    } else {
                        "Paste a PEM key or rely on the saved key in SSH Console Settings."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (authMethod == "Password") {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ssh_password")
                    )
                } else {
                    OutlinedTextField(
                        value = keyText,
                        onValueChange = { keyText = it },
                        label = { Text("Private Key (PEM)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .testTag("ssh_key")
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ssh_passphrase")
                    )
                }
            }
        }

        if (inlineError != null) {
            Text(
                text = inlineError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                val parsedPort = port.toIntOrNull() ?: 22
                val keyBytes = if (authMethod == "Key" && keyText.isNotBlank()) keyText.toByteArray() else null
                onConnect(
                    host.trim(),
                    parsedPort,
                    user.trim(),
                    if (authMethod == "Password") password else null,
                    keyBytes,
                    passphrase.ifBlank { null }
                )
            },
            enabled = !connecting && host.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ssh_connect")
        ) {
            Icon(Icons.Default.Terminal, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (connecting) "Connecting..." else "Connect")
        }
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
        scroll.scrollTo(scroll.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text(
            text = output,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (input.isNotBlank()) {
                                    onSend(input)
                                    input = ""
                                }
                            }
                        )
                    )
                    Button(
                        onClick = {
                            if (input.isNotBlank()) {
                                onSend(input)
                                input = ""
                            }
                        }
                    ) {
                        Text("Send")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCtrlC) {
                        Text("Ctrl+C")
                    }
                }
            }
        }
    }
}
