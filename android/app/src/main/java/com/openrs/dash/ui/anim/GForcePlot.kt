package com.openrs.dash.ui.anim

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openrs.dash.ui.Accent
import com.openrs.dash.ui.Dim
import com.openrs.dash.ui.Frost
import com.openrs.dash.ui.JetBrainsMonoFamily
import com.openrs.dash.ui.LocalThemeAccent
import kotlin.math.min

/**
 * 2D G-force visualization with concentric rings, crosshairs, comet trail, and live dot.
 * X axis = lateral G (positive = right), Y axis = longitudinal G (positive = accel, negative = brake).
 */
@Composable
fun GForcePlot(
    lateralG: Float,
    longitudinalG: Float,
    trail: List<Pair<Float, Float>>,
    modifier: Modifier = Modifier,
    maxG: Float = 1.5f,
    dotColor: Color = Accent
) {
    val accent = LocalThemeAccent.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val ringLabelStyle = remember(density) {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = 9.sp,
            color = Dim.copy(alpha = 0.6f),
            fontWeight = FontWeight.Normal
        )
    }
    val axisLabelStyle = remember(density) {
        TextStyle(
            fontFamily = JetBrainsMonoFamily,
            fontSize = 10.sp,
            color = Dim.copy(alpha = 0.55f),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }

    val ringSteps = remember { listOf(0.5f, 1.0f, 1.5f) }
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) * 0.88f
        val scale = radius / maxG

        // Concentric rings
        for (g in ringSteps) {
            val r = g * scale
            drawCircle(
                color = Dim.copy(alpha = 0.22f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Crosshairs — accent-tinted
        drawLine(accent.copy(alpha = 0.10f), Offset(cx, cy - radius), Offset(cx, cy + radius), 1.dp.toPx())
        drawLine(accent.copy(alpha = 0.10f), Offset(cx - radius, cy), Offset(cx + radius, cy), 1.dp.toPx())

        // Ring labels (Compose drawText)
        for (g in ringSteps) {
            val r = g * scale
            val label = "%.1f".format(g)
            val measured = textMeasurer.measure(label, ringLabelStyle)
            val labelX = cx + r * 0.71f + 4.dp.toPx()
            val labelY = cy - r * 0.71f - measured.size.height.toFloat()
            drawText(measured, topLeft = Offset(labelX, labelY))
        }

        // Axis labels (Compose drawText)
        val accelM = textMeasurer.measure("ACCEL", axisLabelStyle)
        drawText(accelM, topLeft = Offset(cx - accelM.size.width / 2f, cy - radius - 6.dp.toPx() - accelM.size.height))
        val brakeM = textMeasurer.measure("BRAKE", axisLabelStyle)
        drawText(brakeM, topLeft = Offset(cx - brakeM.size.width / 2f, cy + radius + 6.dp.toPx()))
        val leftM = textMeasurer.measure("L", axisLabelStyle)
        drawText(leftM, topLeft = Offset(cx - radius - 12.dp.toPx() - leftM.size.width, cy - leftM.size.height / 2f))
        val rightM = textMeasurer.measure("R", axisLabelStyle)
        drawText(rightM, topLeft = Offset(cx + radius + 12.dp.toPx(), cy - rightM.size.height / 2f))

        // Comet trail with radial gradient halos
        if (trail.size >= 2) {
            for (i in trail.indices) {
                val (tLat, tLon) = trail[i]
                val ageFraction = i.toFloat() / trail.size
                val alpha = 0.05f + ageFraction * 0.45f
                val tx = cx + tLat * scale
                val ty = cy - tLon * scale

                // Radial gradient halo (fading with age)
                val haloRadius = (2.5.dp.toPx() + ageFraction * 4.dp.toPx())
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(dotColor.copy(alpha = alpha * 0.4f), Color.Transparent),
                        center = Offset(tx, ty),
                        radius = haloRadius
                    ),
                    radius = haloRadius,
                    center = Offset(tx, ty)
                )

                // Trail dot
                drawCircle(dotColor.copy(alpha = alpha), radius = 2.5.dp.toPx(), center = Offset(tx, ty))
                if (i > 0) {
                    val (pLat, pLon) = trail[i - 1]
                    val px = cx + pLat * scale
                    val py = cy - pLon * scale
                    drawLine(dotColor.copy(alpha = alpha * 0.6f), Offset(px, py), Offset(tx, ty),
                        strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
        }

        // Live dot with glow
        val dotX = cx + lateralG * scale
        val dotY = cy - longitudinalG * scale
        drawCircle(
            brush = Brush.radialGradient(
                listOf(dotColor.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(dotX, dotY),
                radius = 14.dp.toPx()
            ),
            radius = 14.dp.toPx(),
            center = Offset(dotX, dotY)
        )
        drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
        drawCircle(Color.White.copy(alpha = 0.5f), radius = 2.dp.toPx(), center = Offset(dotX, dotY))
    }
}
