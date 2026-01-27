package com.omiddd.dropletmanager.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.utils.SshKeyManager

@OptIn(ExperimentalMaterial3Api::class)
class SshSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DropletManagerTheme(useDarkTheme = com.omiddd.dropletmanager.utils.ThemePreferences.isDark(this)) {
                val mgr = remember { SshKeyManager(this) }
                val ctx = LocalContext.current
                var pem by remember { mutableStateOf("") }
                var pass by remember { mutableStateOf("") }
                var sshUser by remember { mutableStateOf(mgr.getUsername() ?: "root") }
                var hasKey by remember { mutableStateOf(mgr.hasKey()) }
                val scroll = rememberScrollState()

                Scaffold(topBar = {
                    TopAppBar(title = { Text("SSH Console Settings") }, navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    })
                }) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Used for one-click Console connection to droplets.")
                        Text(if (hasKey) "A private key is saved for auto-connect." else "No SSH private key saved.")

                        OutlinedTextField(
                            value = pem,
                            onValueChange = { pem = it },
                            label = { Text("Private Key (PEM)") },
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                        OutlinedTextField(
                            value = pass,
                            onValueChange = { pass = it },
                            label = { Text("Passphrase (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sshUser,
                            onValueChange = { sshUser = it },
                            label = { Text("Default SSH Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (pem.isNotBlank()) {
                                    mgr.savePrivateKey(pem.trim(), pass.ifBlank { null })
                                    pem = ""; pass = ""
                                    hasKey = mgr.hasKey()
                                    Toast.makeText(ctx, "SSH key saved", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = pem.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save SSH Key") }

                        OutlinedButton(onClick = { mgr.clear(); hasKey = mgr.hasKey(); Toast.makeText(ctx, "SSH key cleared", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Clear SSH Key") }
                        Button(onClick = { if (sshUser.isNotBlank()) { mgr.saveUsername(sshUser.trim()); Toast.makeText(ctx, "SSH username saved", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth()) { Text("Save Username") }
                    }
                }
            }
        }
    }
}
