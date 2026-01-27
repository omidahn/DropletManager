package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun SshDetailsDialog(
    ipAddress: String?,
    username: String = "root",
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val sshCommand = if (ipAddress.isNullOrEmpty()) null else "ssh $username@$ipAddress"
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("SSH Access") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val ip = ipAddress ?: "No Public IP"
                Text(text = "IP Address: $ip", style = MaterialTheme.typography.bodyMedium)
                if (sshCommand != null) {
                    Text(text = "Command: $sshCommand", modifier = Modifier.padding(top = 8.dp))
                    Button(
                        onClick = { clipboard.setText(AnnotatedString(sshCommand)) },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("Copy") }
                }
            }
        }
    )
}
