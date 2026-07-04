package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.data.model.Project
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.omiddd.dropletmanager.R

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
        title = { Text(stringResource(R.string.select_project)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
        text = {
            when {
                loading -> Text(stringResource(R.string.loading_projects))
                error != null -> Text(error)
                projects.isEmpty() -> Text(stringResource(R.string.no_projects_available))
                else -> {
                    val scroll = rememberScrollState()
                    Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(scroll)) {
                        projects.forEach { p ->
                            Text(
                                text = if (p.isDefault) stringResource(R.string.current_project_label, p.name) else p.name,
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
