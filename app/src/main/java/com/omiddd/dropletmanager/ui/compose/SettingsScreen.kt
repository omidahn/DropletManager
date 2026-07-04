package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.ui.activity.SshSettingsActivity

@Composable
fun SettingsScreen(
    currentTokenMasked: String,
    onChangeToken: (String) -> Unit,
    onLogout: () -> Unit,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    permissionHint: String? = null,
    modifier: Modifier = Modifier
) {
    var newToken by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Manage access, appearance, and app-level utilities.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (permissionHint != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, tonalElevation = 0.dp) {
                Text(
                    text = permissionHint,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("API Access", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "Current token: $currentTokenMasked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = newToken,
                    onValueChange = { newToken = it },
                    label = { Text("New API Token") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_token")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { if (newToken.isNotBlank()) onChangeToken(newToken.trim()) },
                        enabled = newToken.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save Token") }
                    OutlinedButton(onClick = onLogout, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }
        }

        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text("Appearance", style = MaterialTheme.typography.titleLarge)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dark mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Switch the app between light and dark presentation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = darkMode,
                        onCheckedChange = onDarkModeChange,
                        modifier = Modifier.testTag("settings_dark_mode")
                    )
                }
            }
        }

        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text("Utilities", style = MaterialTheme.typography.titleLarge)
                }
                Button(
                    onClick = { context.startActivity(Intent(context, SshSettingsActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SSH Console Settings")
                }
                HorizontalDivider()
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy Policy")
                }
            }
        }
    }
}
