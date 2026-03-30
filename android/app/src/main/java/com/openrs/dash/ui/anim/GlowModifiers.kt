package com.openrs.dash.ui.anim

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════════
// GLOW MODIFIER LIBRARY — neon / cyberpunk visual effects
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Draws a soft radial glow behind the composable — works on all API levels (no RenderEffect).
 */
fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 10.dp,
    alpha: Float = 0.15f
): Modifier = this.drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = radius.toPx().coerceAtLeast(size.minDimension * 0.6f)
        )
    )
}

/**
 * Draws a soft rectangular glow — useful for bars and indicators.
 */
fun Modifier.neonGlowRect(
    color: Color,
    alpha: Float = 0.20f,
    spread: Dp = 4.dp
): Modifier = this.drawBehind {
    val s = spread.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent)
        ),
        topLeft = Offset(0f, -s),
        size = Size(size.width, size.height + s * 2)
    )
}

/**
 * Glowing border that replaces flat borders on cards.
 * Draws an outer glow layer + inner 1dp border, optionally pulsing.
 *
 * @param animated When true, alpha breathes via rememberInfiniteTransition (2s cycle).
 *                 Use only on hero-tier cards (3-5 per page max) to limit GPU cost.
 */
fun Modifier.neonBorder(
    color: Color,
    cornerRadius: Dp = 12.dp,
    alpha: Float = 0.25f,
    glowSpread: Dp = 6.dp,
    animated: Boolean = false
): Modifier = composed {
    val pulseAlpha = if (animated) {
        val t = rememberInfiniteTransition(label = "neonBrd")
        val a by t.animateFloat(
            initialValue = alpha * 0.5f,
            targetValue = alpha,
            animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
            label = "neonBrdA"
        )
        a
    } else alpha

    this.drawBehind {
        val cr = cornerRadius.toPx()
        val sp = glowSpread.toPx()
        // Outer glow layer — diffuse halo
        drawRoundRect(
            color = color.copy(alpha = pulseAlpha * 0.4f),
            cornerRadius = CornerRadius(cr + sp * 0.5f),
            topLeft = Offset(-sp, -sp),
            size = Size(size.width + sp * 2, size.height + sp * 2),
            style = Stroke(width = sp)
        )
        // Inner border — crisp 1dp line
        drawRoundRect(
            color = color.copy(alpha = pulseAlpha),
            cornerRadius = CornerRadius(cr),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Breathing alpha modulation for status dots and indicators.
 * Wraps the composable in a pulsing alpha envelope.
 */
fun Modifier.neonPulse(
    color: Color,
    periodMs: Int = 1500,
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 1.0f
): Modifier = composed {
    val t = rememberInfiniteTransition(label = "neonPulse")
    val a by t.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = EaseInOut), RepeatMode.Reverse),
        label = "neonPulseA"
    )
    this.drawBehind {
        drawCircle(
            color = color.copy(alpha = a * 0.3f),
            radius = size.minDimension * 0.8f,
            center = center
        )
    }
}

/**
 * Intense double-layer radial bloom behind active hero values.
 * Stronger than neonGlow — outer diffuse ring + inner bright core.
 *
 * @param intensity 0.0–1.0, scales both layers.
 */
fun Modifier.bloomGlow(
    color: Color,
    radius: Dp = 40.dp,
    intensity: Float = 0.3f
): Modifier = this.drawBehind {
    val r = radius.toPx().coerceAtLeast(size.minDimension * 0.6f)
    // Outer diffuse layer
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = intensity * 0.5f), Color.Transparent),
            center = center,
            radius = r * 1.2f
        )
    )
    // Inner bright core
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color.copy(alpha = intensity), Color.Transparent),
            center = center,
            radius = r * 0.7f
        )
    )
}

/**
 * Horizontal light sweep that travels vertically across the composable.
 * Very faint CRT/HUD feel — apply to non-scrolling wrappers only.
 *
 * @param speedMs Duration for one full sweep (top to bottom).
 */
fun Modifier.scanLine(
    color: Color,
    speedMs: Int = 4000,
    lineHeight: Dp = 2.dp,
    alpha: Float = 0.06f
): Modifier = composed {
    val t = rememberInfiniteTransition(label = "scan")
    val progress by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(speedMs, easing = LinearEasing)),
        label = "scanY"
    )
    this.drawBehind {
        val y = progress * size.height
        val band = lineHeight.toPx() * 3
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, color.copy(alpha = alpha), Color.Transparent),
                startY = y - band,
                endY = y + band
            )
        )
    }
}
