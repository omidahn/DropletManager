package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.data.model.Project
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn

@Composable
fun ProjectSelectorDialog(
    projects: List<Project>,
    loading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSelect: (Project) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Project") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        text = {
            when {
                loading -> CircularProgressIndicator()
                // Error is handled by a global overlay in the host; keep dialog minimal
                error != null -> Column {}
                else -> {
                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(scroll)) {
                        projects.forEach { p ->
                            Text(
                                text = p.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(p) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}
