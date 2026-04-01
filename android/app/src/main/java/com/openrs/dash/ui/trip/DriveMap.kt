package com.openrs.dash.ui.trip

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.openrs.dash.R
import com.openrs.dash.data.DriveMode
import com.openrs.dash.data.DrivePointEntity
import com.openrs.dash.data.PeakEvent
import com.openrs.dash.data.PeakType
import com.openrs.dash.ui.Accent
import com.openrs.dash.ui.Ok
import com.openrs.dash.ui.Orange
import com.openrs.dash.ui.Warn
import androidx.compose.ui.geometry.Offset

// ═══════════════════════════════════════════════════════════════════════════
// DriveMap — Google Maps Compose wrapper with telemetry polyline overlay
// ═══════════════════════════════════════════════════════════════════════════

enum class ColorMode { SPEED, DRIVE_MODE }

/**
 * Google Maps composable for the MAP tab.
 *
 * @param points     Drive points to render as a polyline (live or historic)
 * @param colorMode  Polyline coloring strategy (speed thresholds or drive mode)
 * @param peakEvents Peak markers to place on the map
 * @param rtrPoint   Race-ready achievement point (optional marker)
 * @param currentLat Current live latitude (for position dot, null if no location)
 * @param currentLng Current live longitude
 * @param isRecording Whether a drive is actively recording
 * @param isPaused   Whether recording is paused (still shows position but no polyline growth)
 */
@Composable
fun DriveMap(
    points: List<DrivePointEntity>,
    colorMode: ColorMode = ColorMode.SPEED,
    peakEvents: List<PeakEvent> = emptyList(),
    rtrPoint: DrivePointEntity? = null,
    currentLat: Double? = null,
    currentLng: Double? = null,
    isRecording: Boolean = false,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapStyleOptions = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.google_map_style_dark)
    }

    val cameraPositionState = rememberCameraPositionState()

    // Auto-center on first point or current location
    LaunchedEffect(currentLat, currentLng, points.firstOrNull()) {
        val lat = currentLat ?: points.firstOrNull()?.lat ?: return@LaunchedEffect
        val lng = currentLng ?: points.firstOrNull()?.lng ?: return@LaunchedEffect
        if (cameraPositionState.position.target.latitude == 0.0) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f)
            )
        }
    }

    // Follow current position when recording
    LaunchedEffect(currentLat, currentLng, isRecording) {
        if (isRecording && currentLat != null && currentLng != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(LatLng(currentLat, currentLng)),
                300
            )
        }
    }

    val mapProperties = remember(mapStyleOptions) {
        MapProperties(
            mapStyleOptions = mapStyleOptions,
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false  // we draw our own position marker
        )
    }

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false
        )
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings
    ) {
        // ── Route polyline (color-segmented) ─────────────────────────
        if (points.size >= 2) {
            val segments = buildColorSegments(points, colorMode)
            segments.forEach { seg ->
                Polyline(
                    points = seg.latLngs,
                    color = seg.color,
                    width = 10f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
            }
        }

        // ── Current position marker ──────────────────────────────────
        if (currentLat != null && currentLng != null) {
            val posIcon = remember { createPositionDot() }
            Marker(
                state = MarkerState(position = LatLng(currentLat, currentLng)),
                icon = posIcon,
                anchor = Offset(0.5f, 0.5f),
                flat = true,
                zIndex = 10f
            )
        }

        // ── Peak markers ─────────────────────────────────────────────
        peakEvents.forEach { peak ->
            val (color, label) = when (peak.type) {
                PeakType.RPM -> Warn to "RPM: ${peak.value.toInt()}"
                PeakType.BOOST -> Accent to "Boost: ${"%.1f".format(peak.value)} PSI"
                PeakType.LATERAL_G -> Orange to "Lat-G: ${"%.2f".format(peak.value)}"
            }
            Marker(
                state = MarkerState(position = LatLng(peak.lat, peak.lng)),
                title = peak.type.label,
                snippet = label,
                icon = BitmapDescriptorFactory.defaultMarker(colorToHue(color)),
                zIndex = 5f
            )
        }

        // ── RTR marker ───────────────────────────────────────────────
        rtrPoint?.let {
            Marker(
                state = MarkerState(position = LatLng(it.lat, it.lng)),
                title = "Race Ready",
                snippet = "RTR achieved",
                icon = BitmapDescriptorFactory.defaultMarker(colorToHue(Ok)),
                zIndex = 5f
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Polyline color segmentation
// ═══════════════════════════════════════════════════════════════════════════

private data class ColorSegment(val latLngs: List<LatLng>, val color: Color)

private fun buildColorSegments(
    points: List<DrivePointEntity>,
    mode: ColorMode
): List<ColorSegment> {
    if (points.isEmpty()) return emptyList()
    val segments = mutableListOf<ColorSegment>()
    var currentColor = pointColor(points[0], mode)
    var currentLatLngs = mutableListOf(LatLng(points[0].lat, points[0].lng))

    for (i in 1 until points.size) {
        val p = points[i]
        val prev = points[i - 1]

        // Detect pause gaps (> 5s between consecutive points)
        val gap = p.timestamp - prev.timestamp > 5000

        val color = pointColor(p, mode)
        if (color != currentColor || gap) {
            segments += ColorSegment(currentLatLngs.toList(), currentColor)
            currentColor = color
            currentLatLngs = mutableListOf(
                if (gap) LatLng(p.lat, p.lng)
                else LatLng(prev.lat, prev.lng)  // overlap for seamless join
            )
        }
        currentLatLngs += LatLng(p.lat, p.lng)
    }
    if (currentLatLngs.size >= 2) {
        segments += ColorSegment(currentLatLngs.toList(), currentColor)
    }
    return segments
}

private fun pointColor(point: DrivePointEntity, mode: ColorMode): Color = when (mode) {
    ColorMode.SPEED -> when {
        point.speedKph < 60  -> Ok       // green — slow
        point.speedKph < 100 -> Accent   // cyan — moderate
        point.speedKph < 140 -> Warn     // yellow — fast
        else                 -> Orange   // orange — very fast
    }
    ColorMode.DRIVE_MODE -> when (point.driveMode) {
        DriveMode.SPORT.label  -> Warn     // yellow
        DriveMode.TRACK.label  -> Ok       // green
        DriveMode.DRIFT.label  -> Orange   // orange
        else                   -> Accent   // cyan — Normal
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Marker helpers
// ═══════════════════════════════════════════════════════════════════════════

private fun createPositionDot(): BitmapDescriptor {
    val size = 36
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    // White outer ring
    canvas.drawCircle(cx, cy, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    })
    // Cyan inner circle (Nitrous Blue accent)
    canvas.drawCircle(cx, cy, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0091EA.toInt()
        style = Paint.Style.FILL
    })
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** Map a Compose Color to a Google Maps marker hue (0-360). */
private fun colorToHue(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

