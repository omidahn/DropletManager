package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletAction
import com.omiddd.dropletmanager.ui.viewmodel.CostSummary
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun DropletListScreen(
    droplets: List<Droplet>,
    isLoading: Boolean,
    error: String?,
    costSummary: CostSummary,
    onRefresh: () -> Unit,
    onDropletClicked: (Droplet) -> Unit,
    onActionClicked: (Droplet, DropletAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenConsole: (Droplet) -> Unit = {},
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    statusOptions: List<String> = emptyList(),
    selectedStatus: String? = null,
    onStatusChange: (String?) -> Unit = {},
    regionOptions: List<String> = emptyList(),
    selectedRegion: String? = null,
    onRegionChange: (String?) -> Unit = {},
    onSwitchProject: () -> Unit = {}
) {
    Surface(modifier = modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search & filters
                    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                label = { Text(stringResource(R.string.search)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh)) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterRow(
                            statusOptions = statusOptions,
                            selectedStatus = selectedStatus,
                            onStatusChange = onStatusChange,
                            regionOptions = regionOptions,
                            selectedRegion = selectedRegion,
                            onRegionChange = onRegionChange,
                            onSwitchProject = onSwitchProject
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusSummary(droplets)
                        Spacer(modifier = Modifier.height(8.dp))
                        CostSummary(summary = costSummary)
                    }

                    if (droplets.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text(stringResource(R.string.no_droplets_message))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
                        }
                    } else {
                        val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .lazyListScrollbar(
                                    state = listState,
                                    color = scrollbarColor
                                )
                        ) {
                            items(droplets) { droplet ->
                                DropletRow(
                                    droplet = droplet,
                                    onClick = { onDropletClicked(droplet) },
                                    onActionClicked = onActionClicked,
                                    onOpenConsole = onOpenConsole
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.lazyListScrollbar(
    state: LazyListState,
    color: Color,
    thickness: Dp = 4.dp,
    padding: Dp = 2.dp,
    minThumbLength: Dp = 24.dp
): Modifier = drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo
    if (totalItems == 0 || visibleItems.isEmpty()) return@drawWithContent

    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    if (viewportHeight <= 0) return@drawWithContent

    val avgItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    val totalHeight = avgItemSize * totalItems
    if (totalHeight <= 0f) return@drawWithContent

    val scrollOffset = (state.firstVisibleItemIndex * avgItemSize) + state.firstVisibleItemScrollOffset
    val rawThumb = (viewportHeight / totalHeight) * viewportHeight
    val thumbHeight = rawThumb.coerceIn(minThumbLength.toPx(), viewportHeight.toFloat())
    val maxThumbOffset = (viewportHeight - thumbHeight).coerceAtLeast(0f)
    val thumbOffset = (scrollOffset / totalHeight).coerceIn(0f, 1f) * maxThumbOffset

    val thicknessPx = thickness.toPx()
    val paddingPx = padding.toPx()
    val x = size.width - thicknessPx - paddingPx
    val y = thumbOffset + paddingPx
    val height = (thumbHeight - paddingPx * 2).coerceAtLeast(thicknessPx)

    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(thicknessPx, height),
        cornerRadius = CornerRadius(thicknessPx, thicknessPx)
    )
}

@Composable
private fun DropletRow(
    droplet: Droplet,
    onClick: () -> Unit,
    onActionClicked: (Droplet, DropletAction) -> Unit,
    onOpenConsole: (Droplet) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(text = droplet.name, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        val ipV4 = droplet.networks.v4.firstOrNull { it.type == "public" }?.ip_address ?: stringResource(R.string.no_public_ip)
        Text(text = "${droplet.region.slug} • ${droplet.status} • $ipV4", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (!isDropletRunning(droplet.status)) {
                AssistChip(
                    onClick = { onActionClicked(droplet, DropletAction.PowerOn) },
                    label = { Text(stringResource(R.string.start)) },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors()
                )
            } else {
                AssistChip(
                    onClick = { onActionClicked(droplet, DropletAction.PowerOff) },
                    label = { Text(stringResource(R.string.stop)) },
                    leadingIcon = { Icon(Icons.Filled.PowerSettingsNew, contentDescription = null) }
                )
                AssistChip(
                    onClick = { onActionClicked(droplet, DropletAction.Reboot) },
                    label = { Text(stringResource(R.string.reboot)) },
                    leadingIcon = { Icon(Icons.Filled.RestartAlt, contentDescription = null) }
                )
            }
            AssistChip(
                onClick = { onOpenConsole(droplet) },
                label = { Text(stringResource(R.string.console)) },
                leadingIcon = { Icon(Icons.Filled.Code, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun FilterRow(
    statusOptions: List<String>,
    selectedStatus: String?,
    onStatusChange: (String?) -> Unit,
    regionOptions: List<String>,
    selectedRegion: String?,
    onRegionChange: (String?) -> Unit,
    onSwitchProject: () -> Unit
) {
    val allString = stringResource(R.string.all)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dropdown(
            label = stringResource(R.string.status),
            options = listOf(allString) + statusOptions,
            selected = selectedStatus ?: allString,
            onSelected = { onStatusChange(if (it == allString) null else it) }
        )
        Dropdown(
            label = stringResource(R.string.region),
            options = listOf(allString) + regionOptions,
            selected = selectedRegion ?: allString,
            onSelected = { onRegionChange(if (it == allString) null else it) }
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onSwitchProject) { Text(stringResource(R.string.switch_project)) }
    }
}

@Composable
private fun Dropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(modifier = Modifier
            .width(IntrinsicSize.Min)
            .clickable { expanded = true }
            .padding(4.dp)) {
            Text("$label: $selected")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { expanded = false; onSelected(opt) }
                )
            }
        }
    }
}

@Composable
private fun StatusSummary(droplets: List<Droplet>) {
    if (droplets.isEmpty()) return
    val counts = remember(droplets) {
        droplets.groupBy { it.status.lowercase() }.mapValues { it.value.size }
    }
    val total = droplets.size
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(title = stringResource(R.string.total), value = total.toString())
        counts.forEach { (status, count) ->
            val label = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            SummaryCard(title = label, value = count.toString(), status = status)
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, status: String? = null) {
    val bg = when (status) {
        "active" -> MaterialTheme.colorScheme.primaryContainer
        "off", "inactive" -> MaterialTheme.colorScheme.secondaryContainer
        "error" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val onBg = when (status) {
        "active" -> MaterialTheme.colorScheme.onPrimaryContainer
        "off", "inactive" -> MaterialTheme.colorScheme.onSecondaryContainer
        "error" -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = bg, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = onBg)
            Text(value, style = MaterialTheme.typography.titleMedium, color = onBg)
        }
    }
}

@Composable
private fun CostSummary(summary: CostSummary) {
    if (summary.totalMonthly == 0.0) return

    Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(R.string.estimated_cost), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.monthly_base_cost, summary.totalMonthly), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.monthly_with_backups_cost, summary.totalMonthlyWithBackups), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.accrued_this_month_est, summary.accruedThisMonth), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// Helper keeps status checks consistent and accepts common variants returned by APIs.
private fun isDropletRunning(status: String?): Boolean {
    if (status == null) return false
    return when (status.trim().lowercase()) {
        "active", "on", "running", "online", "up" -> true
        else -> false
    }
}
