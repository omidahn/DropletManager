package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.data.model.Droplet
import com.omiddd.dropletmanager.data.model.DropletAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import kotlin.collections.buildList

@OptIn(ExperimentalMaterial3Api::class)
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
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.droplet_details)) },
        text = {
            val context = LocalContext.current
            val config = LocalConfiguration.current
            val maxHeight = (config.screenHeightDp.dp * 0.8f)
            val detailItems = remember(droplet, accruedCost) {
                buildList<Pair<String, String>> {
                    add(context.getString(R.string.name_label) to droplet.name)
                    add(context.getString(R.string.status_label) to droplet.status)
                    add(context.getString(R.string.region_label) to droplet.region.name)
                    add(context.getString(R.string.image_label) to "${droplet.image.distribution} ${droplet.image.name}")
                    add(context.getString(R.string.detail_label_vcpus) to droplet.vcpus.toString())
                    add(context.getString(R.string.detail_label_memory) to "${droplet.memory} MB")
                    add(context.getString(R.string.detail_label_disk) to "${droplet.disk} GB")
                    val ipV4 = droplet.networks.v4.firstOrNull { it.type == "public" }?.ip_address
                        ?: context.getString(R.string.no_public_ip)
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

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(detailItems) { (label, value) ->
                    DetailRow(label, value)
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    val isOn = isDropletRunning(droplet.status)
                    if (!isOn) {
                        ActionButton(
                            icon = Icons.Filled.PlayArrow,
                            label = stringResource(R.string.start),
                            onClick = { onActionClicked(DropletAction.PowerOn) }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionButton(
                                icon = Icons.Filled.PowerSettingsNew,
                                label = stringResource(R.string.stop),
                                onClick = { onActionClicked(DropletAction.PowerOff) }
                            )
                            ActionButton(
                                icon = Icons.Filled.RestartAlt,
                                label = stringResource(R.string.reboot),
                                onClick = { onActionClicked(DropletAction.Reboot) }
                            )
                            ActionButton(
                                icon = Icons.Filled.Autorenew,
                                label = stringResource(R.string.power_cycle),
                                onClick = { onActionClicked(DropletAction.PowerCycle) }
                            )
                            ActionButton(
                                icon = Icons.Filled.PowerSettingsNew,
                                label = stringResource(R.string.shutdown),
                                onClick = { onActionClicked(DropletAction.Shutdown) }
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            icon = Icons.Filled.PhotoCamera,
                            label = stringResource(R.string.create_snapshot),
                            onClick = { onActionClicked(DropletAction.Snapshot) }
                        )
                        val backupsEnabled = droplet.features.contains("backups")
                        ActionButton(
                            iconPainter = painterResource(id = R.drawable.ic_backup),
                            label = stringResource(if (backupsEnabled) R.string.disable_backups else R.string.enable_backups),
                            onClick = {
                                onActionClicked(
                                    if (backupsEnabled) DropletAction.DisableBackups else DropletAction.EnableBackups
                                )
                            }
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            icon = Icons.Filled.Code,
                            label = stringResource(R.string.console),
                            onClick = onOpenConsole
                        )
                        ActionButton(
                            icon = Icons.Filled.BarChart,
                            label = stringResource(R.string.usage),
                            onClick = onOpenUsage
                        )
                    }
                }

                item {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.destroy))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) {
                Text(stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = "$label:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(110.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value)
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    label: String,
    onClick: () -> Unit
) {
    FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        when {
            icon != null -> Icon(icon, contentDescription = null)
            iconPainter != null -> Icon(iconPainter, contentDescription = null)
        }
        Spacer(Modifier.width(6.dp))
        Text(label)
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
