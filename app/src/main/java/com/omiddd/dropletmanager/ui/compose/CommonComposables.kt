package com.omiddd.dropletmanager.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun LabeledSparkline(points: List<Double>, color: Color, suffix: String) {
    if (points.isEmpty()) return
    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 0.0
    val avg = points.average()
    Sparkline(points = points, color = color)
    val locale = Locale.getDefault()
    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("min=${String.format(locale, "%.2f", min)}$suffix", style = MaterialTheme.typography.bodySmall)
        Text("avg=${String.format(locale, "%.2f", avg)}$suffix", style = MaterialTheme.typography.bodySmall)
        Text("max=${String.format(locale, "%.2f", max)}$suffix", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun Sparkline(points: List<Double>, color: Color, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val drawModifier = Modifier.fillMaxWidth().height(64.dp).then(modifier)
    Canvas(modifier = drawModifier) {
        val w = size.width
        val h = size.height
        val maxV = (points.maxOrNull() ?: 1.0).let { if (it == 0.0) 1.0 else it }
        val minV = points.minOrNull() ?: 0.0
        val range = (maxV - minV).let { if (it == 0.0) 1.0 else it }
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val norm = (v - minV) / range
            val y = h - (norm * h).toFloat()
            val pt = Offset(x, y)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
