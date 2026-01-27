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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.data.model.DropletCreationRequest
import com.omiddd.dropletmanager.ui.viewmodel.CreateDropletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDropletScreen(
    viewModel: CreateDropletViewModel,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadOptions() }

    var name by remember { mutableStateOf("") }
    var regionExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }
    var imageExpanded by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf<String?>(null) }
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<String?>(null) }
    var enableBackups by remember { mutableStateOf(false) }
    var enableIpv6 by remember { mutableStateOf(true) }
    var enableMonitoring by remember { mutableStateOf(true) }
    var sshExpanded by remember { mutableStateOf(false) }
    var tagsText by remember { mutableStateOf("") }
    var userData by remember { mutableStateOf("") }
    val selectedSsh = remember { mutableStateListOf<Int>() }

    var errorDialog by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.error) { if (state.error != null) errorDialog = state.error }
    // Auto-dismiss non-permission errors after a short delay
    LaunchedEffect(errorDialog) {
        val msg = errorDialog
        if (msg != null && !msg.startsWith("Permission denied", ignoreCase = true)) {
            kotlinx.coroutines.delay(3000)
            if (errorDialog == msg) errorDialog = null
        }
    }
    val scope = rememberCoroutineScope()
    Scaffold { padding ->
        val scroll = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create Droplet", style = MaterialTheme.typography.titleLarge)
            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Region
        ExposedDropdownMenuBox(expanded = regionExpanded, onExpandedChange = { regionExpanded = !regionExpanded }) {
            OutlinedTextField(
                readOnly = true,
                value = selectedRegion ?: "Select Region",
                onValueChange = {},
                label = { Text("Region") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                state.regions.forEach { r ->
                    DropdownMenuItem(text = { Text("${r.slug} • ${r.name}") }, onClick = { selectedRegion = r.slug; regionExpanded = false })
                }
            }
        }
        // Size
        ExposedDropdownMenuBox(expanded = sizeExpanded, onExpandedChange = { sizeExpanded = !sizeExpanded }) {
            OutlinedTextField(
                readOnly = true,
                value = selectedSize ?: "Select Size",
                onValueChange = {},
                label = { Text("Size") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                state.sizes.forEach { s ->
                    DropdownMenuItem(text = { Text("${s.slug} • ${s.memory}MB ${s.vcpus}vCPU ${s.diskSize}GB") }, onClick = { selectedSize = s.slug; sizeExpanded = false })
                }
            }
        }
        // Image
        val selectedImageDisplay = selectedImage?.let { value ->
            state.images.firstOrNull { img -> img.slug == value || img.id.toString() == value }?.let { img ->
                img.slug ?: "${img.distribution} ${img.name}"
            }
        }

        ExposedDropdownMenuBox(expanded = imageExpanded, onExpandedChange = { imageExpanded = !imageExpanded }) {
            OutlinedTextField(
                readOnly = true,
                value = selectedImageDisplay ?: selectedImage ?: "Select Image",
                onValueChange = {},
                label = { Text("Image") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = imageExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
            )
            ExposedDropdownMenu(expanded = imageExpanded, onDismissRequest = { imageExpanded = false }) {
                state.images.forEach { img ->
                    val label = img.slug ?: "${img.distribution} ${img.name}"
                    val value = img.slug ?: img.id.toString()
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        selectedImage = value
                        imageExpanded = false
                    })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(selected = enableBackups, onClick = { enableBackups = !enableBackups }, label = { Text("Backups") })
            FilterChip(selected = enableIpv6, onClick = { enableIpv6 = !enableIpv6 }, label = { Text("IPv6") })
            FilterChip(selected = enableMonitoring, onClick = { enableMonitoring = !enableMonitoring }, label = { Text("Monitoring") })
        }

        // SSH Keys selection (dropdown, multi-select)
        if (state.sshKeys.isNotEmpty()) {
            val selectedNames = state.sshKeys
                .filter { selectedSsh.contains(it.id) }
                .joinToString(", ") { it.name }
            val summary = if (selectedNames.isBlank()) "None selected" else selectedNames

            ExposedDropdownMenuBox(expanded = sshExpanded, onExpandedChange = { sshExpanded = !sshExpanded }) {
                OutlinedTextField(
                    readOnly = true,
                    value = summary,
                    onValueChange = {},
                    label = { Text("SSH Keys") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sshExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(expanded = sshExpanded, onDismissRequest = { sshExpanded = false }) {
                    // Quick clear option
                    DropdownMenuItem(
                        text = { Text("Clear selection") },
                        onClick = {
                            selectedSsh.clear()
                            sshExpanded = false
                        }
                    )
                    state.sshKeys.forEach { key ->
                        val checked = selectedSsh.contains(key.id)
                        DropdownMenuItem(
                            text = {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Text("${key.name} (${key.fingerprint.take(16)}…)")
                                }
                            },
                            onClick = {
                                if (checked) {
                                    selectedSsh.remove(key.id)
                                } else {
                                    selectedSsh.add(key.id)
                                }
                            }
                        )
                    }
                }
            }
        }

        // VPC selection removed as requested; droplets will use default VPC per region

        // Tags and User Data
        OutlinedTextField(value = tagsText, onValueChange = { tagsText = it }, label = { Text("Tags (comma separated)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = userData, onValueChange = { userData = it }, label = { Text("cloud-init user data (optional)") }, modifier = Modifier.fillMaxWidth().height(120.dp))

        val canCreate = name.isNotBlank() && selectedRegion != null && selectedSize != null && selectedImage != null
        Button(
            onClick = {
                val req = DropletCreationRequest(
                    name = name.trim(),
                    region = selectedRegion!!,
                    size = selectedSize!!,
                    imageId = selectedImage!!,
                    sshKeys = selectedSsh.toList(),
                    backups = enableBackups,
                    ipv6 = enableIpv6,
                    monitoring = enableMonitoring,
                    tags = tagsText.split(',').mapNotNull { it.trim().ifBlank { null } }.ifEmpty { null },
                    userData = userData.ifBlank { null }
                )
                viewModel.create(req) { success, msg ->
                    if (success) onCreated() else if (msg != null) {
                        errorDialog = msg
                    }
                }
            }, enabled = canCreate && !state.loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.loading) "Creating..." else "Create") }
        // Animated, centered error overlay with optional auto-dismiss
        AnimatedVisibility(
            visible = errorDialog != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 10 })
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(tonalElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Error", style = MaterialTheme.typography.titleLarge)
                        Text(errorDialog ?: "")
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { errorDialog = null }) { Text("OK") }
                        }
                    }
                }
            }
        }
        }
    }
}
