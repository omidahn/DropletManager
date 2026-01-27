package com.omiddd.dropletmanager.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omiddd.dropletmanager.ui.theme.DropletManagerTheme
import com.omiddd.dropletmanager.ui.viewmodel.DropletViewModel
import com.omiddd.dropletmanager.ui.viewmodel.DropletViewModelFactory
import com.omiddd.dropletmanager.data.repository.DropletRepository
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

@androidx.compose.material3.ExperimentalMaterial3Api
class MemoryMetricsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dropletId = intent.getIntExtra(EXTRA_ID, -1)
        val dropletName = intent.getStringExtra(EXTRA_NAME) ?: ""
        val totalMb = intent.getIntExtra(EXTRA_MEMORY_MB, 0)
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
        val viewModel: DropletViewModel by viewModels { DropletViewModelFactory(token, DropletRepository()) }
        setContent {
            val dark = com.omiddd.dropletmanager.utils.ThemePreferences.isDark(this)
            DropletManagerTheme(useDarkTheme = dark) {
                LaunchedEffect(dropletId) { if (dropletId > 0) viewModel.loadMemoryMetrics(dropletId) }
                val mem by viewModel.memory.collectAsState()
                val scroll = rememberScrollState()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Memory Usage – $dropletName") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { padding ->
                    var errorOverlay by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(mem.error) { if (mem.error != null) errorOverlay = mem.error }
                    LaunchedEffect(errorOverlay) {
                        val msg = errorOverlay
                        if (msg != null && !msg.startsWith("Permission denied", ignoreCase = true)) {
                            kotlinx.coroutines.delay(3000)
                            if (errorOverlay == msg) errorOverlay = null
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Error handled via overlay
                        val seriesMb = mem.points.map { normalizeMemory(it.value, totalMb).first }
                        if (seriesMb.isNotEmpty()) {
                            val last = seriesMb.last()
                            val pct = if (totalMb > 0) (last / totalMb) * 100.0 else 0.0
                            Text("Last: ${formatVal(last)} MB (${formatVal(pct)}%)")
                            LabeledSparkline(seriesMb, MaterialTheme.colorScheme.primary, " MB")
                        } else if (mem.isLoading) {
                            Text("Loading…")
                        } else {
                            Text("No data")
                        }
                        // Centered overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = errorOverlay != null,
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 10 }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 10 })
                        ) {
                            androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                androidx.compose.material3.Surface(tonalElevation = 8.dp, shape = androidx.compose.material3.MaterialTheme.shapes.medium) {
                                    androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                        Text("Error", style = MaterialTheme.typography.titleLarge)
                                        Text(errorOverlay ?: "")
                                        androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                            Button(onClick = { errorOverlay = null }) { Text("OK") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_NAME = "name"
        const val EXTRA_MEMORY_MB = "memory_mb"
        const val EXTRA_TOKEN = "token"
    }
}

// Helpers (duplicated lightweight from details screen)
private fun formatVal(v: Double): String = String.format("%.2f", v)
private fun normalizeMemory(value: Double, totalMb: Int): Triple<Double, Double, Double> {
    val mb = when {
        value > totalMb * 5 -> value / 1024.0 / 1024.0
        value in 0.0..1.0 -> value * totalMb
        value in 1.0..100.0 -> (value / 100.0) * totalMb
        else -> value
    }
    val gb = mb / 1024.0
    val pct = if (totalMb > 0) (mb / totalMb) * 100.0 else 0.0
    return Triple(mb, gb, pct.coerceIn(0.0, 100.0))
}

@Composable
private fun LabeledSparkline(points: List<Double>, color: androidx.compose.ui.graphics.Color, suffix: String) {
    if (points.isEmpty()) return
    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 0.0
    val avg = points.average()
    Sparkline(points = points, color = color)
    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("min=${formatVal(min)}$suffix", style = MaterialTheme.typography.bodySmall)
        Text("avg=${formatVal(avg)}$suffix", style = MaterialTheme.typography.bodySmall)
        Text("max=${formatVal(max)}$suffix", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Sparkline(points: List<Double>, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier.fillMaxWidth().height(160.dp)) {
    if (points.isEmpty()) return
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxV = (points.maxOrNull() ?: 1.0).let { if (it == 0.0) 1.0 else it }
        val minV = points.minOrNull() ?: 0.0
        val range = (maxV - minV).let { if (it == 0.0) 1.0 else it }
        val stepX = w / (points.size - 1).coerceAtLeast(1)
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val norm = (v - minV) / range
            val y = h - (norm * h).toFloat()
            val pt = androidx.compose.ui.geometry.Offset(x, y)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
