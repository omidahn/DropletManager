package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings")
        if (permissionHint != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, tonalElevation = 0.dp) {
                Text(
                    text = permissionHint,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                )
            }
        }
        Text("Current token: $currentTokenMasked")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Dark mode")
            androidx.compose.material3.Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
        }
        OutlinedTextField(
            value = newToken,
            onValueChange = { newToken = it },
            label = { Text("New API Token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { if (newToken.isNotBlank()) onChangeToken(newToken.trim()) },
                enabled = newToken.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Token") }
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { context.startActivity(Intent(context, SshSettingsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) {
            Text("SSH Console Settings")
        }
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Privacy Policy") }
    }
}
