package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletAction
import kotlin.collections.buildList

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DropletDetailsScreen(
    droplet: Droplet,
    onActionClicked: (DropletAction) -> Unit,
    onDelete: () -> Unit = {},
    onOpenConsole: () -> Unit = {},
    onOpenUsage: () -> Unit = {},
    accruedCost: Double?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val ipV4 = droplet.networks.v4.firstOrNull { it.type == "public" }?.ip_address
        ?: context.getString(R.string.no_public_ip)
    val detailItems = remember(droplet, accruedCost) {
        buildList<Pair<String, String>> {
            add(context.getString(R.string.region_label) to droplet.region.name)
            add(context.getString(R.string.image_label) to "${droplet.image.distribution} ${droplet.image.name}")
            add(context.getString(R.string.detail_label_vcpus) to droplet.vcpus.toString())
            add(context.getString(R.string.detail_label_memory) to "${droplet.memory} MB")
            add(context.getString(R.string.detail_label_disk) to "${droplet.disk} GB")
            add(context.getString(R.string.ip_address_label) to ipV4)
            val monthly = droplet.size.priceMonthly
            val hourly = droplet.size.priceHourly
            val costSummary = if (hourly != null) {
                context.getString(R.string.cost_monthly_hourly_format, monthly, hourly)
            } else {
                context.getString(R.string.cost_monthly_format, monthly)
            }
            add(context.getString(R.string.cost_label) to costSummary)
            if (accruedCost != null) {
                add(
                    context.getString(R.string.accrued_this_month) to
                        context.getString(R.string.accrued_this_month_format, accruedCost)
                )
            }
            if (droplet.features.isNotEmpty()) {
                add(context.getString(R.string.features) to droplet.features.joinToString(", "))
            }
            if (droplet.tags.isNotEmpty()) {
                add(context.getString(R.string.tags) to droplet.tags.joinToString(", "))
            }
            add(context.getString(R.string.created_label) to droplet.createdAt)
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            droplet.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${droplet.size.slug} | ${droplet.region.slug.uppercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Public IP", style = MaterialTheme.typography.labelLarge)
                            Text(ipV4, style = MaterialTheme.typography.titleMedium)
                        }
                        DetailsStatusPill(status = droplet.status)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Details", style = MaterialTheme.typography.titleLarge)
                                detailItems.forEach { (label, value) ->
                                    DetailRow(label, value)
                                }
                            }
                        }
                    }

                    item {
                        ElevatedCard(shape = MaterialTheme.shapes.extraLarge) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Actions", style = MaterialTheme.typography.titleLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    dropletActionButtons(
                                        droplet = droplet,
                                        onActionClicked = onActionClicked,
                                        onOpenConsole = onOpenConsole,
                                        onOpenUsage = onOpenUsage
                                    ).forEach { action ->
                                        FilledTonalButton(onClick = action.onClick) {
                                            when {
                                                action.icon != null -> Icon(action.icon, contentDescription = null)
                                                action.painter != null -> Icon(action.painter, contentDescription = null)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(action.label)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.destroy))
                }
            }
        }
    }
}

@Composable
private fun DetailsStatusPill(status: String) {
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
    Surface(color = container, shape = MaterialTheme.shapes.large) {
        Text(
            text = normalized.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class DetailAction(
    val icon: ImageVector? = null,
    val painter: Painter? = null,
    val label: String,
    val onClick: () -> Unit
)

@Composable
private fun dropletActionButtons(
    droplet: Droplet,
    onActionClicked: (DropletAction) -> Unit,
    onOpenConsole: () -> Unit,
    onOpenUsage: () -> Unit
): List<DetailAction> {
    return buildList {
        if (!isDropletRunning(droplet.status)) {
            add(DetailAction(icon = Icons.Filled.PlayArrow, label = stringResource(R.string.start)) {
                onActionClicked(DropletAction.PowerOn)
            })
        } else {
            add(DetailAction(icon = Icons.Filled.PowerSettingsNew, label = stringResource(R.string.stop)) {
                onActionClicked(DropletAction.PowerOff)
            })
            add(DetailAction(icon = Icons.Filled.RestartAlt, label = stringResource(R.string.reboot)) {
                onActionClicked(DropletAction.Reboot)
            })
            add(DetailAction(icon = Icons.Filled.Autorenew, label = stringResource(R.string.power_cycle)) {
                onActionClicked(DropletAction.PowerCycle)
            })
            add(DetailAction(icon = Icons.Filled.PowerSettingsNew, label = stringResource(R.string.shutdown)) {
                onActionClicked(DropletAction.Shutdown)
            })
        }
        add(DetailAction(icon = Icons.Filled.PhotoCamera, label = stringResource(R.string.create_snapshot)) {
            onActionClicked(DropletAction.Snapshot)
        })
        val backupsEnabled = droplet.features.contains("backups")
        add(
            DetailAction(
                painter = painterResource(id = R.drawable.ic_backup),
                label = stringResource(if (backupsEnabled) R.string.disable_backups else R.string.enable_backups)
            ) {
                onActionClicked(
                    if (backupsEnabled) DropletAction.DisableBackups else DropletAction.EnableBackups
                )
            }
        )
        add(DetailAction(icon = Icons.Filled.Code, label = stringResource(R.string.console), onClick = onOpenConsole))
        add(DetailAction(icon = Icons.Filled.BarChart, label = stringResource(R.string.usage), onClick = onOpenUsage))
    }
}
