package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.R
import com.omiddd.dropletmanager.ui.viewmodel.MetricUiState
import com.omiddd.dropletmanager.ui.viewmodel.UsageMetricsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageMetricsScreen(
    viewModel: UsageMetricsViewModel,
    dropletName: String,
    onBack: () -> Unit
) {
    val cpuState by viewModel.cpu.collectAsState()
    val memoryState by viewModel.memory.collectAsState()
    val bwOutState by viewModel.bandwidthOutbound.collectAsState()
    val bwInState by viewModel.bandwidthInbound.collectAsState()
    val fsUsedState by viewModel.filesystemUsed.collectAsState()
    val load1State by viewModel.load1.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dropletName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { viewModel.loadAllMetrics() }) { Icon(Icons.Default.Refresh, null) } }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { MetricCard(title = stringResource(R.string.metric_cpu), state = cpuState, suffix = "%") }
            item { MetricCard(title = stringResource(R.string.metric_memory), state = memoryState, suffix = "%") }
            item { MetricCard(title = stringResource(R.string.metric_bandwidth_out), state = bwOutState, suffix = " Mbps", transform = ::bytesPerSecToMbps) }
            item { MetricCard(title = stringResource(R.string.metric_bandwidth_in), state = bwInState, suffix = " Mbps", transform = ::bytesPerSecToMbps) }
            item { MetricCard(title = stringResource(R.string.metric_filesystem), state = fsUsedState, suffix = "%") }
            item { MetricCard(title = stringResource(R.string.metric_load_1), state = load1State, suffix = "") }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    state: MetricUiState,
    suffix: String,
    transform: ((Double) -> Double)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (state.isLoading) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            } else {
                val rawPoints = state.points.sortedBy { it.timestamp }
                if (rawPoints.isEmpty()) {
                    Text(stringResource(R.string.metrics_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val values = rawPoints.map { it.value }
                    val points = if (transform != null) values.map(transform) else values
                    if (points.isEmpty()) {
                        Text(stringResource(R.string.metrics_no_data), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val hasGaps = rawPoints.zipWithNext().any { (a, b) -> (b.timestamp - a.timestamp) > METRIC_GAP_THRESHOLD_SECONDS }
                        LabeledSparkline(
                            points = points,
                            color = MaterialTheme.colorScheme.primary,
                            suffix = suffix
                        )
                        if (hasGaps) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.metrics_partial_data),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun bytesPerSecToMbps(bytes: Double): Double {
    return (bytes * 8) / 1_000_000
}

private const val METRIC_GAP_THRESHOLD_SECONDS = 15 * 60
