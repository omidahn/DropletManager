package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletAction
import com.omiddd.dropletmanager.ui.viewmodel.CostSummary

@OptIn(ExperimentalLayoutApi::class)
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Fleet overview", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${droplets.size} droplets",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = onQueryChange,
                                    label = { Text(stringResource(R.string.search)) },
                                    placeholder = { Text("Name or IP") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(onClick = onRefresh) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.refresh))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            FilterRow(
                                statusOptions = statusOptions,
                                selectedStatus = selectedStatus,
                                onStatusChange = onStatusChange,
                                regionOptions = regionOptions,
                                selectedRegion = selectedRegion,
                                onRegionChange = onRegionChange,
                                onSwitchProject = onSwitchProject
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            StatusSummary(droplets)
                            Spacer(modifier = Modifier.height(10.dp))
                            CostSummary(summary = costSummary)
                        }
                    }

                    if (droplets.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.no_droplets_message),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a droplet or refresh to sync the latest infrastructure state.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
                        }
                    } else {
                        val scrollbarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .lazyListScrollbar(state = listState, color = scrollbarColor)
                        ) {
                            items(droplets) { droplet ->
                                DropletRow(
                                    droplet = droplet,
                                    onClick = { onDropletClicked(droplet) },
                                    onActionClicked = onActionClicked,
                                    onOpenConsole = onOpenConsole
                                )
                                Spacer(modifier = Modifier.height(10.dp))
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun DropletRow(
    droplet: Droplet,
    onClick: () -> Unit,
    onActionClicked: (Droplet, DropletAction) -> Unit,
    onOpenConsole: (Droplet) -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = droplet.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${droplet.size.slug} | ${droplet.memory} MB | ${droplet.vcpus} vCPU",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(status = droplet.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            val ipV4 = droplet.networks.v4.firstOrNull { it.type == "public" }?.ip_address
                ?: stringResource(R.string.no_public_ip)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TravelExplore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${droplet.region.slug.uppercase()} | $ipV4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isDropletRunning(droplet.status)) {
                    AssistChip(
                        onClick = { onActionClicked(droplet, DropletAction.PowerOn) },
                        label = { Text(stringResource(R.string.start)) },
                        leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
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
}

@OptIn(ExperimentalLayoutApi::class)
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterControl(
            label = stringResource(R.string.status),
            options = listOf(allString) + statusOptions,
            selected = selectedStatus ?: allString,
            onSelected = { onStatusChange(if (it == allString) null else it) }
        )
        FilterControl(
            label = stringResource(R.string.region),
            options = listOf(allString) + regionOptions,
            selected = selectedRegion ?: allString,
            onSelected = { onRegionChange(if (it == allString) null else it) }
        )
        FilledTonalButton(onClick = onSwitchProject) {
            Text(stringResource(R.string.switch_project))
        }
    }
}

@Composable
private fun FilterControl(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(IntrinsicSize.Min)
        ) {
            Text(selected)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelected(opt)
                    }
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
private fun StatusPill(status: String) {
    val normalized = status.lowercase()
    val container = when (normalized) {
        "active", "on", "running" -> MaterialTheme.colorScheme.secondaryContainer
        "archive", "archived" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (normalized) {
        "active", "on", "running" -> MaterialTheme.colorScheme.onSecondaryContainer
        "archive", "archived" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    InputChip(
        selected = true,
        onClick = {},
        enabled = false,
        label = {
            Text(
                normalized.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                style = MaterialTheme.typography.labelLarge
            )
        },
        colors = InputChipDefaults.inputChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content
        )
    )
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
    Surface(color = bg, shape = MaterialTheme.shapes.large) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = onBg)
            Text(value, style = MaterialTheme.typography.titleMedium, color = onBg)
        }
    }
}

@Composable
private fun CostSummary(summary: CostSummary) {
    if (summary.totalMonthly == 0.0) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(stringResource(R.string.estimated_cost), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.monthly_base_cost, summary.totalMonthly), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.monthly_with_backups_cost, summary.totalMonthlyWithBackups), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.accrued_this_month_est, summary.accruedThisMonth), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
