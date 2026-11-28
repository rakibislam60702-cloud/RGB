package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Crisp Facebook / Instagram style blue verified badge (`#1877F2` -> `#0095F6`)
 * with a scalloped rosette seal and a clean white checkmark inside.
 * Never tinted yellow or gold.
 */
@Composable
fun BlueVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    contentDescription: String? = "Verified"
) {
    val semanticsMod = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Canvas(
        modifier = semanticsMod.size(size)
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = minOf(w, h)

        // Draw 8-lobe Facebook / Instagram style scalloped verified seal
        val sealPath = Path()
        val steps = 128
        val lobes = 8
        val outerR = minDim * 0.49f
        val innerAmp = minDim * 0.058f
        val baseR = outerR - innerAmp

        for (i in 0..steps) {
            val theta = (i.toDouble() / steps.toDouble()) * (2.0 * PI)
            // Soft rounded rosette wave
            val wave = cos(lobes * theta)
            val r = baseR + innerAmp * (0.65 * wave + 0.35 * wave * wave * wave)
            val x = cx + (r * cos(theta)).toFloat()
            val y = cy + (r * sin(theta)).toFloat()
            if (i == 0) {
                sealPath.moveTo(x, y)
            } else {
                sealPath.lineTo(x, y)
            }
        }
        sealPath.close()

        drawPath(
            path = sealPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0095F6),
                    Color(0xFF1877F2)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Crisp pure-white checkmark centered inside the blue seal
        val checkPath = Path().apply {
            moveTo(cx - minDim * 0.21f, cy + minDim * 0.01f)
            lineTo(cx - minDim * 0.05f, cy + minDim * 0.16f)
            lineTo(cx + minDim * 0.22f, cy - minDim * 0.15f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(
                width = (minDim * 0.125f).coerceAtLeast(1.6f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
