package com.openrs.dash.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openrs.dash.data.VehicleState
import com.openrs.dash.ui.anim.CarDiagram
import com.openrs.dash.ui.anim.GForcePlot
import com.openrs.dash.ui.anim.RingBuffer
import com.openrs.dash.ui.anim.tireStatusColor
import kotlin.math.abs
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════
// CHASSIS PAGE (G-Force + Unified Tires & AWD)
// ═══════════════════════════════════════════════════════════════════════════
@Composable fun ChassisPage(vs: VehicleState, p: UserPrefs, onReset: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GForceSection(vs, onReset)
        UnifiedChassisSection(vs, p)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// UNIFIED CHASSIS SECTION — Tires & AWD merged ("Neon Connect" layout)
//
// Layout:  [FL card]          [RS wireframe]          [FR card]
//          [RL card]          (diamond markers)       [RR card]
//                    ⚠ LOW TIRE PRESSURE
//          ────────── AWD — GKN TWINSTER ──────────
//          [torque bar] [metrics] [temps] [clutch]
// ═══════════════════════════════════════════════════════════════════════════
@Composable fun UnifiedChassisSection(vs: VehicleState, p: UserPrefs) {
    val accent = LocalThemeAccent.current
    val lowThreshold = p.tireLowPsi.toDouble()

    // Animated tire status colors for accent edge bars
    val flColor by animateColorAsState(
        tireStatusColor(vs.tirePressLF, lowThreshold), tween(400), label = "flC"
    )
    val frColor by animateColorAsState(
        tireStatusColor(vs.tirePressRF, lowThreshold), tween(400), label = "frC"
    )
    val rlColor by animateColorAsState(
        tireStatusColor(vs.tirePressLR, lowThreshold), tween(400), label = "rlC"
    )
    val rrColor by animateColorAsState(
        tireStatusColor(vs.tirePressRR, lowThreshold), tween(400), label = "rrC"
    )

    // Deltas
    val deltaLF = tpmsDeltaText(vs.tirePressLF, vs.tireStartLF)
    val deltaRF = tpmsDeltaText(vs.tirePressRF, vs.tireStartRF)
    val deltaLR = tpmsDeltaText(vs.tirePressLR, vs.tireStartLR)
    val deltaRR = tpmsDeltaText(vs.tirePressRR, vs.tireStartRR)

    Column(
        Modifier.fillMaxWidth()
            .background(Surf, RoundedCornerShape(16.dp))
            .border(1.dp, Brd, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        SectionLabel("CHASSIS — TIRES & AWD")

        if (!vs.hasTpmsData) {
            // Waiting state — show wireframe + message
            Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FocusRsOutline()
                    Spacer(Modifier.height(12.dp))
                    MonoLabel("WAITING FOR SENSOR DATA", 10.sp, Dim, letterSpacing = 0.15.sp)
                    Spacer(Modifier.height(4.dp))
                    MonoLabel("Sensors transmit when wheels are rolling", 9.sp, Dim.copy(alpha = 0.7f))
                }
            }
        } else {
            // ── Neon Connect layout: cards flanking wireframe ──
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left column: FL + RL
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonTireCard("FL", vs.tirePressLF, p, lowThreshold, flColor,
                        vs.wheelSpeedFL, vs.tireTempLF, deltaLF)
                    NeonTireCard("RL", vs.tirePressLR, p, lowThreshold, rlColor,
                        vs.wheelSpeedRL, vs.tireTempLR, deltaLR)
                }

                // Center: RS wireframe with diamond markers + F/R bar
                CarDiagram(
                    vs = vs,
                    prefs = p,
                    modifier = Modifier.width(120.dp).aspectRatio(0.6f)
                )

                // Right column: FR + RR
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonTireCard("FR", vs.tirePressRF, p, lowThreshold, frColor,
                        vs.wheelSpeedFR, vs.tireTempRF, deltaRF)
                    NeonTireCard("RR", vs.tirePressRR, p, lowThreshold, rrColor,
                        vs.wheelSpeedRR, vs.tireTempRR, deltaRR)
                }
            }

            // ── Low pressure / imbalance warnings ──
            if (vs.anyTireLow(lowThreshold)) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(Orange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .border(1.dp, Orange.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MonoLabel("⚠ LOW TIRE PRESSURE", 10.sp, Orange, letterSpacing = 0.2.sp)
                }
            }
            val spread = vs.maxTirePressSpread
            if (spread >= 4.0 && !vs.anyTireLow(lowThreshold)) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(Warn.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, Warn.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MonoLabel("⚠ PRESSURE IMBALANCE — ${"%.1f".format(spread)} PSI spread", 10.sp, Warn, letterSpacing = 0.1.sp)
                }
            }
        }

        // ── AWD Drivetrain Section (always visible) ──
        Spacer(Modifier.height(12.dp))
        AwdMetrics(vs, p)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// NEON TIRE CARD — colored accent edge bar, hero PSI, speed, temp bar
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun NeonTireCard(
    label: String,
    psi: Double,
    p: UserPrefs,
    lowThreshold: Double,
    statusColor: Color,
    wheelSpeedKph: Double,
    tempC: Double,
    deltaText: String
) {
    val isMissing = psi < 0
    val isLow = psi in 0.0..(lowThreshold - 0.001)
    val borderColor = if (isLow) Orange.copy(alpha = 0.5f) else Brd
    val hasTemp = tempC > -90

    // Format speed
    val speedStr = com.openrs.dash.ui.anim.formatWheelSpeed(wheelSpeedKph, p)

    Row(
        Modifier.fillMaxWidth()
            .background(Surf2, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        // Accent edge bar
        Box(
            Modifier.width(4.dp)
                .fillMaxHeight()
                .background(statusColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
        )

        Column(
            Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row: label + delta
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonoLabel(label, 9.sp, Dim, letterSpacing = 0.12.sp)
                if (deltaText.isNotEmpty()) {
                    val deltaColor = if (deltaText.startsWith("\u25B2")) Ok else Orange
                    MonoText(deltaText, 8.sp, deltaColor)
                }
            }

            Spacer(Modifier.height(2.dp))

            // Hero PSI
            HeroNum(
                if (isMissing) "—" else p.displayTire(psi),
                18.sp,
                statusColor
            )
            MonoLabel(p.tireLabel, 7.sp, Dim, letterSpacing = 0.1.sp)

            // Divider
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth(0.85f).height(1.dp).background(Brd))
            Spacer(Modifier.height(4.dp))

            // Speed + Temp row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonoText(speedStr, 9.sp, Mid)
                if (hasTemp) {
                    MonoText(
                        p.displayTemp(tempC) + p.tempLabel,
                        9.sp,
                        tireTempColor(tempC)
                    )
                }
            }

            // Temperature bar visual
            if (hasTemp) {
                Spacer(Modifier.height(4.dp))
                val tempFraction = ((tempC - 0.0) / 60.0).toFloat().coerceIn(0.05f, 1f)
                val tempColor = tireTempColor(tempC)
                Box(
                    Modifier.fillMaxWidth()
                        .height(4.dp)
                        .background(Surf3, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        Modifier.fillMaxWidth(tempFraction)
                            .height(4.dp)
                            .background(tempColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .border(0.5.dp, tempColor, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// AWD METRICS — torque bar + data cells (moved from old AwdSection)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun AwdMetrics(vs: VehicleState, p: UserPrefs) {
    val accent = LocalThemeAccent.current
    val total = vs.totalRearTorque
    val leftPct = if (total > 0) (vs.awdLeftTorque / total).toFloat() else 0.5f
    val rightPct = (1f - leftPct).coerceIn(0.01f, 0.99f)
    val leftPctC = leftPct.coerceIn(0.01f, 0.99f)
    val avgF = (vs.wheelSpeedFL + vs.wheelSpeedFR) / 2
    val avgR = (vs.wheelSpeedRL + vs.wheelSpeedRR) / 2
    val frDelta = avgR - avgF
    val lrDelta = vs.wheelSpeedRR - vs.wheelSpeedRL

    SectionLabel("AWD — GKN TWINSTER")

    // Torque bar with L/R labels
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        MonoText("L ${vs.awdLeftTorque.roundToInt()} Nm", 10.sp, accent)
        MonoText("${vs.awdRightTorque.roundToInt()} Nm R", 10.sp, Ok)
    }
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth().height(8.dp).background(Surf3, RoundedCornerShape(4.dp))) {
        Box(
            Modifier.weight(leftPctC).fillMaxHeight()
                .background(
                    Brush.horizontalGradient(listOf(accent, accent.copy(0.4f))),
                    RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                )
        )
        Box(
            Modifier.weight(rightPct).fillMaxHeight()
                .background(
                    Brush.horizontalGradient(listOf(Ok.copy(0.4f), Ok)),
                    RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                )
        )
    }

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DataCell("REAR BIAS", vs.rearLeftRightBias, modifier = Modifier.weight(1f))
        val spdLabel = if (p.speedUnit == "MPH") "mph" else "km/h"
        val lrDisp = if (p.speedUnit == "MPH") lrDelta * UnitConversions.KM_TO_MI else lrDelta
        val frDisp = if (p.speedUnit == "MPH") frDelta * UnitConversions.KM_TO_MI else frDelta
        DataCell("L/R DELTA", "${"%.1f".format(lrDisp)} $spdLabel", modifier = Modifier.weight(1f))
        DataCell("F/R DELTA", "${"%.1f".format(frDisp)} $spdLabel", modifier = Modifier.weight(1f))
    }
    if (vs.awdMaxTorque > 0) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataCell("AWD MAX", "${vs.awdMaxTorque.roundToInt()} Nm", modifier = Modifier.weight(1f))
            val ptuStr = if (vs.ptuTempC > -90) "${p.displayTemp(vs.ptuTempC)}${p.tempLabel}" else "—"
            DataCell("PTU TEMP", ptuStr, modifier = Modifier.weight(1f))
            DataCell("RDU TEMP",
                if (vs.rduTempC > -90) "${p.displayTemp(vs.rduTempC)}${p.tempLabel}" else "—",
                modifier = Modifier.weight(1f))
        }
    }
    val hasAwdExpanded = vs.awdClutchTempL > -90 || vs.awdReqTorqueL > 0 || vs.awdDmdPressure > 0
    if (hasAwdExpanded) {
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataCell("CLT L", if (vs.awdClutchTempL > -90) "${p.displayTemp(vs.awdClutchTempL)}${p.tempLabel}" else "—", modifier = Modifier.weight(1f))
            DataCell("CLT R", if (vs.awdClutchTempR > -90) "${p.displayTemp(vs.awdClutchTempR)}${p.tempLabel}" else "—", modifier = Modifier.weight(1f))
            DataCell("TRANS", if (vs.transOilTempC > -90) "${p.displayTemp(vs.transOilTempC)}${p.tempLabel}" else "—", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataCell("REQ L", "${vs.awdReqTorqueL.roundToInt()} Nm", modifier = Modifier.weight(1f))
            DataCell("REQ R", "${vs.awdReqTorqueR.roundToInt()} Nm", modifier = Modifier.weight(1f))
            DataCell("PUMP", "${"%.1f".format(vs.awdPumpCurrent)} A", modifier = Modifier.weight(1f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// G-FORCE SECTION (unchanged)
// ═══════════════════════════════════════════════════════════════════════════
@Composable fun GForceSection(vs: VehicleState, onReset: () -> Unit) {
    val accent = LocalThemeAccent.current
    val animLatG by animateFloatAsState(vs.lateralG.toFloat(), spring(stiffness = Spring.StiffnessHigh), label = "latG")
    val animLonG by animateFloatAsState(vs.longitudinalG.toFloat(), spring(stiffness = Spring.StiffnessHigh), label = "lonG")

    // G-force trail (sampled at ~10 Hz)
    val gTrail = remember { RingBuffer<Pair<Float, Float>>(30) }
    val lastTrailTime = remember { mutableLongStateOf(0L) }
    val now = vs.lastUpdate
    if (now - lastTrailTime.longValue >= 100L) {
        lastTrailTime.longValue = now
        gTrail.push(vs.lateralG.toFloat() to vs.longitudinalG.toFloat())
    }

    Column(
        Modifier.fillMaxWidth()
            .background(Surf, RoundedCornerShape(16.dp))
            .border(1.dp, Brd, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        SectionLabel("G-FORCE & DYNAMICS")

        // G-Force dot plot
        val gPlotModifier = if (isWideLayout())
            Modifier.fillMaxWidth().aspectRatio(1f).heightIn(max = 280.dp).padding(8.dp)
        else
            Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)
        GForcePlot(
            lateralG = animLatG,
            longitudinalG = animLonG,
            trail = gTrail.toList(),
            modifier = gPlotModifier,
            dotColor = accent
        )

        Spacer(Modifier.height(8.dp))

        // Condensed numeric readout
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataCell("LAT G",  "${"%.2f".format(animLatG)}",  modifier = Modifier.weight(1f))
            DataCell("LON G",  "${"%.2f".format(animLonG)}",  modifier = Modifier.weight(1f))
            DataCell("VERT G", "${"%.2f".format(vs.verticalG)}", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DataCell("YAW",      "${"%.1f".format(vs.yawRate)}°/s",    modifier = Modifier.weight(1f))
            DataCell("STEER",    "${"%.1f".format(vs.steeringAngle)}°", modifier = Modifier.weight(1f))
            DataCell("COMBINED", "${"%.2f".format(vs.combinedG)}",     modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth()
                .background(Surf2, RoundedCornerShape(8.dp))
                .border(1.dp, Brd, RoundedCornerShape(8.dp))
                .clickable { onReset() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            MonoLabel("↺ RESET PEAKS", 9.sp, Dim, letterSpacing = 0.15.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════════════════════

/** Formats a TPMS delta string with arrow prefix. Returns "" when delta is below threshold or data is missing. */
private fun tpmsDeltaText(currentPsi: Double, startPsi: Double): String {
    if (startPsi < 0 || currentPsi <= 0) return ""
    val delta = currentPsi - startPsi
    if (abs(delta) <= 0.5) return ""
    return if (delta > 0) "\u25B2 +${"%.1f".format(delta)}"
    else "\u25BC ${"%.1f".format(delta)}"
}
