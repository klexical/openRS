package com.openrs.dash.ui.anim

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openrs.dash.R
import com.openrs.dash.data.VehicleState
import com.openrs.dash.ui.Dim
import com.openrs.dash.ui.Frost
import com.openrs.dash.ui.LocalThemeAccent
import com.openrs.dash.ui.Ok
import com.openrs.dash.ui.Orange
import com.openrs.dash.ui.ShareTechMono
import com.openrs.dash.ui.Surf3
import com.openrs.dash.ui.UnitConversions
import com.openrs.dash.ui.UserPrefs
import kotlin.math.roundToInt

/**
 * Focus RS MK3 wireframe with diamond wheel markers and F/R torque
 * split bar overlaid at physical wheel positions.
 *
 * Designed to sit between flanking tire cards in the unified
 * Chassis section ("Neon Connect" layout). Connector lines are
 * drawn by the parent composable via [wheelAnchors].
 */
@Composable
fun CarDiagram(
    vs: VehicleState,
    prefs: UserPrefs,
    modifier: Modifier = Modifier
) {
    val accent = LocalThemeAccent.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Tire status colors — animated transitions
    val lowThreshold = prefs.tireLowPsi.toDouble()
    val flColor by animateColorAsState(
        tireStatusColor(vs.tirePressLF, lowThreshold), tween(400), label = "flCol"
    )
    val frColor by animateColorAsState(
        tireStatusColor(vs.tirePressRF, lowThreshold), tween(400), label = "frCol"
    )
    val rlColor by animateColorAsState(
        tireStatusColor(vs.tirePressLR, lowThreshold), tween(400), label = "rlCol"
    )
    val rrColor by animateColorAsState(
        tireStatusColor(vs.tirePressRR, lowThreshold), tween(400), label = "rrCol"
    )

    val rearPct = vs.rearTorquePct.roundToInt()
    val frontPct = 100 - rearPct
    val splitLabel = "F $frontPct% / R $rearPct%"

    val splitStyle = remember(density) {
        TextStyle(
            fontFamily = ShareTechMono,
            fontSize = with(density) { 10.sp },
            color = Frost,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
    val splitLabelStyle = remember(density) {
        TextStyle(
            fontFamily = ShareTechMono,
            fontSize = with(density) { 8.sp },
            color = Dim,
            textAlign = TextAlign.Center
        )
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        // RS wireframe image — tinted to theme accent
        Image(
            painter = painterResource(R.drawable.focus_rs_wireframe),
            contentDescription = "Focus RS",
            colorFilter = ColorFilter.tint(accent, BlendMode.SrcIn),
            modifier = Modifier.fillMaxSize()
        )

        // Data overlay drawn on top of the wireframe
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height

                // Diamond markers at wheel positions
                val diamondSize = 5.dp.toPx()
                drawDiamondMarker(w * 0.14f, h * 0.26f, diamondSize, flColor)
                drawDiamondMarker(w * 0.86f, h * 0.26f, diamondSize, frColor)
                drawDiamondMarker(w * 0.14f, h * 0.74f, diamondSize, rlColor)
                drawDiamondMarker(w * 0.86f, h * 0.74f, diamondSize, rrColor)

                // AWD torque split (center)
                drawTorqueSplit(w, h, frontPct, rearPct, splitLabel, accent,
                    textMeasurer, splitStyle, splitLabelStyle)
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Drawing helpers
// ═════════════════════════════════════════════════════════════════════════════

/** Diamond-shaped wheel marker with glow. */
private fun DrawScope.drawDiamondMarker(
    cx: Float, cy: Float, size: Float, color: Color
) {
    // Outer glow diamond
    val glowPath = Path().apply {
        moveTo(cx, cy - size * 1.6f)
        lineTo(cx + size * 1.6f, cy)
        lineTo(cx, cy + size * 1.6f)
        lineTo(cx - size * 1.6f, cy)
        close()
    }
    drawPath(glowPath, color.copy(alpha = 0.2f))

    // Filled diamond
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx + size, cy)
        lineTo(cx, cy + size)
        lineTo(cx - size, cy)
        close()
    }
    drawPath(path, color)

    // Edge stroke for definition
    drawPath(path, color.copy(alpha = 0.7f), style = Stroke(width = 1f))
}

private fun DrawScope.drawTorqueSplit(
    w: Float, h: Float,
    frontPct: Int, rearPct: Int,
    splitLabel: String,
    accent: Color,
    textMeasurer: TextMeasurer,
    splitStyle: TextStyle,
    labelStyle: TextStyle
) {
    val barW = 6.dp.toPx()
    val barH = h * 0.28f
    val barX = w / 2f - barW / 2f
    val barY = h / 2f - barH / 2f

    // Background bar
    drawRoundRect(
        color = Surf3,
        topLeft = Offset(barX, barY),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(3.dp.toPx())
    )

    // Front portion (accent, from top)
    val frontH = barH * (frontPct / 100f).coerceIn(0.02f, 0.98f)
    drawRoundRect(
        color = accent.copy(alpha = 0.8f),
        topLeft = Offset(barX, barY),
        size = Size(barW, frontH),
        cornerRadius = CornerRadius(3.dp.toPx())
    )

    // Rear portion (green, from bottom)
    val rearH = barH - frontH
    drawRoundRect(
        color = Ok.copy(alpha = 0.8f),
        topLeft = Offset(barX, barY + frontH),
        size = Size(barW, rearH),
        cornerRadius = CornerRadius(3.dp.toPx())
    )

    // "F/R" label above bar
    val frLabel = textMeasurer.measure("F/R", labelStyle)
    drawText(
        frLabel,
        topLeft = Offset(w / 2f - frLabel.size.width / 2f, barY - frLabel.size.height - 2.dp.toPx())
    )

    // Split text below bar
    val splitResult = textMeasurer.measure(splitLabel, splitStyle)
    drawText(
        splitResult,
        topLeft = Offset(w / 2f - splitResult.size.width / 2f, barY + barH + 3.dp.toPx())
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// Public helpers — wheel anchor positions for connector lines
// ═════════════════════════════════════════════════════════════════════════════

/** Fractional wheel positions within the wireframe (matches drawable geometry). */
data class WheelAnchor(val xFraction: Float, val yFraction: Float)

val WHEEL_ANCHORS = listOf(
    WheelAnchor(0.14f, 0.26f),  // FL
    WheelAnchor(0.86f, 0.26f),  // FR
    WheelAnchor(0.14f, 0.74f),  // RL
    WheelAnchor(0.86f, 0.74f),  // RR
)

// ═════════════════════════════════════════════════════════════════════════════
// Formatting helpers
// ═════════════════════════════════════════════════════════════════════════════

internal fun tireStatusColor(psi: Double, lowThreshold: Double): Color = when {
    psi < 0            -> Dim       // no data yet
    psi < lowThreshold -> Orange    // low pressure
    psi > 40.0         -> Orange    // over-inflated
    else               -> Ok        // normal
}

internal fun formatTirePressure(psi: Double, prefs: UserPrefs): String {
    if (psi < 0) return "—"
    return prefs.displayTire(psi)
}

internal fun formatWheelSpeed(kph: Double, prefs: UserPrefs): String {
    val value = if (prefs.speedUnit == "MPH") kph * UnitConversions.KM_TO_MI else kph
    val label = if (prefs.speedUnit == "MPH") "mph" else "kph"
    return "${value.roundToInt()} $label"
}
