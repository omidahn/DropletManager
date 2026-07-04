package com.omiddd.dropletmanager.ui.activity

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.data.model.CreateSshKeyRequest
import com.omiddd.dropletmanager.data.repository.DropletRepository
import com.omiddd.dropletmanager.data.repository.Result
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.utils.SshKeyGenerator
import com.omiddd.dropletmanager.utils.SshKeyManager
import com.omiddd.dropletmanager.utils.ThemePreferences
import com.omiddd.dropletmanager.utils.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class SshSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DropletManagerTheme(useDarkTheme = ThemePreferences.isDark(this)) {
                SshSettingsContent(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SshSettingsContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val keyManager = remember { SshKeyManager(context) }
    val tokenManager = remember { TokenManager(context) }
    val repository = remember { DropletRepository() }
    val scope = rememberCoroutineScope()

    var pem by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var sshUser by remember { mutableStateOf(keyManager.getUsername() ?: "root") }
    var hasKey by remember { mutableStateOf(keyManager.hasKey()) }
    var isAppManaged by remember { mutableStateOf(keyManager.isAppManagedKey()) }
    var remoteKeyName by remember { mutableStateOf(keyManager.getRemoteKeyName()) }
    var remoteFingerprint by remember { mutableStateOf(keyManager.getRemoteFingerprint()) }
    var isBusy by remember { mutableStateOf(false) }

    fun refreshStoredState() {
        hasKey = keyManager.hasKey()
        isAppManaged = keyManager.isAppManagedKey()
        remoteKeyName = keyManager.getRemoteKeyName()
        remoteFingerprint = keyManager.getRemoteFingerprint()
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SSH Console Settings") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("App-managed SSH key", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        if (hasKey && isAppManaged) {
                            "A device-local private key is ready for one-tap console access."
                        } else {
                            "Generate and store a private key inside the app, then upload its public key to DigitalOcean."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (remoteKeyName != null) {
                        Text(
                            "DigitalOcean key: $remoteKeyName",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (!remoteFingerprint.isNullOrBlank()) {
                        Text(
                            "Fingerprint: $remoteFingerprint",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val comment = "droplet-manager@android"
                                keyManager.saveGeneratedKey(SshKeyGenerator.generate(comment))
                                refreshStoredState()
                                toast("App-managed SSH key generated")
                            },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (hasKey && isAppManaged) "Regenerate" else "Generate")
                        }
                        FilledTonalButton(
                            onClick = {
                                val publicKey = keyManager.getPublicKey()
                                val token = tokenManager.getToken()
                                if (publicKey.isNullOrBlank()) {
                                    toast("Generate an app-managed key first")
                                    return@FilledTonalButton
                                }
                                if (token.isNullOrBlank()) {
                                    toast("Save an API token first")
                                    return@FilledTonalButton
                                }
                                scope.launch {
                                    isBusy = true
                                    val keyName = buildManagedKeyName()
                                    when (val keysResult = repository.listSshKeys(token)) {
                                        is Result.Success -> {
                                            val existing = keysResult.data.firstOrNull { it.publicKey == publicKey }
                                            val savedKey = if (existing != null) {
                                                existing
                                            } else {
                                                when (val createResult = repository.createSshKey(token, CreateSshKeyRequest(keyName, publicKey))) {
                                                    is Result.Success -> createResult.data
                                                    is Result.Error -> {
                                                        toast(createResult.message)
                                                        isBusy = false
                                                        return@launch
                                                    }
                                                    is Result.Loading -> {
                                                        isBusy = false
                                                        return@launch
                                                    }
                                                }
                                            }
                                            keyManager.saveRemoteKeyMetadata(savedKey.id, savedKey.name, savedKey.fingerprint)
                                            refreshStoredState()
                                            toast(if (existing != null) "Reused existing DigitalOcean SSH key" else "Uploaded SSH public key")
                                        }
                                        is Result.Error -> toast(keysResult.message)
                                        is Result.Loading -> Unit
                                    }
                                    isBusy = false
                                }
                            },
                            enabled = !isBusy && hasKey && isAppManaged,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload")
                        }
                    }
                }
            }

            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Import existing key", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "If you already have a PEM private key, paste it here for console authentication.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pem,
                        onValueChange = { pem = it },
                        label = { Text("Private Key (PEM)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Passphrase (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            keyManager.savePrivateKey(pem.trim(), passphrase.ifBlank { null })
                            pem = ""
                            passphrase = ""
                            refreshStoredState()
                            toast("SSH key saved")
                        },
                        enabled = pem.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save imported key")
                    }
                }
            }

            ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Defaults", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = sshUser,
                        onValueChange = { sshUser = it },
                        label = { Text("Default SSH Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (sshUser.isNotBlank()) {
                                keyManager.saveUsername(sshUser.trim())
                                toast("SSH username saved")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save username")
                    }
                    OutlinedButton(
                        onClick = {
                            keyManager.clear()
                            refreshStoredState()
                            toast("Saved SSH key cleared")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear saved key")
                    }
                }
            }
        }
    }
}

private fun buildManagedKeyName(): String {
    val model = Build.MODEL
        .replace(Regex("[^A-Za-z0-9]+"), "-")
        .trim('-')
        .take(24)
        .ifBlank { "android" }
    return "DropletManager-$model"
}
