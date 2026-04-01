package com.openrs.dash.ui.trip

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.openrs.dash.OpenRSDashApp
import com.openrs.dash.data.DriveDatabase
import com.openrs.dash.data.DriveEntity
import com.openrs.dash.data.DrivePointEntity
import com.openrs.dash.data.DriveState
import com.openrs.dash.data.VehicleState
import com.openrs.dash.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// DRIVE PAGE — MAP tab: live recording + drive history
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DrivePage(
    driveState: DriveState,
    vehicleState: VehicleState,
    prefs: UserPrefs
) {
    val context = LocalContext.current
    val accent = LocalThemeAccent.current
    val scope = rememberCoroutineScope()
    val recorder = remember { OpenRSDashApp.instance.driveRecorder }

    var hasLocationPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPerm = granted }

    // Color mode toggle
    var colorMode by remember { mutableStateOf(ColorMode.SPEED) }

    // Drive history
    var drives by remember { mutableStateOf<List<DriveEntity>>(emptyList()) }
    var selectedDrivePoints by remember { mutableStateOf<List<DrivePointEntity>>(emptyList()) }
    var selectedDriveId by remember { mutableStateOf<Long?>(null) }

    // Load drive history
    LaunchedEffect(driveState.isRecording) {
        withContext(Dispatchers.IO) {
            drives = DriveDatabase.getInstance(context).driveDao()
                .getRecentDrives(AppSettings.getMaxSavedDrives(context))
        }
    }

    // Recording indicator pulse
    val recAlpha by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "recAlpha"
    )

    val isLive = vehicleState.isConnected || driveState.isRecording

    Column(Modifier.fillMaxSize().background(Bg)) {
        // ── Map section ──────────────────────────────────────────────
        Box(
            Modifier.weight(if (isLive) 0.55f else 0.45f).fillMaxWidth()
        ) {
            val mapPoints = if (driveState.isRecording) {
                driveState.recentPoints
            } else if (selectedDriveId != null) {
                selectedDrivePoints
            } else {
                emptyList()
            }

            DriveMap(
                points = mapPoints,
                colorMode = colorMode,
                peakEvents = driveState.peakEvents,
                rtrPoint = driveState.rtrAchievedPoint,
                currentLat = driveState.currentLocation?.latitude,
                currentLng = driveState.currentLocation?.longitude,
                isRecording = driveState.isRecording,
                isPaused = driveState.isPaused
            )

            // ── Floating controls ────────────────────────────────────
            // Color mode toggle (top-right)
            Box(
                Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Surf.copy(alpha = 0.85f))
                    .border(1.dp, Brd, RoundedCornerShape(6.dp))
                    .clickable {
                        colorMode = if (colorMode == ColorMode.SPEED)
                            ColorMode.DRIVE_MODE else ColorMode.SPEED
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                MonoText(
                    if (colorMode == ColorMode.SPEED) "SPD" else "MODE",
                    10.sp, accent, FontWeight.Bold
                )
            }

            // Recording indicator (top-center)
            if (driveState.isRecording && !driveState.isPaused) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surf.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(8.dp)
                            .clip(CircleShape)
                            .background(Orange.copy(alpha = recAlpha))
                    )
                    MonoText("REC", 9.sp, Orange, FontWeight.Bold)
                }
            }

            // Paused indicator
            if (driveState.isPaused) {
                Row(
                    Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surf.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MonoText("PAUSED", 9.sp, Warn, FontWeight.Bold)
                }
            }

            // Weather card (top-right, below color toggle)
            driveState.currentWeather?.let { weather ->
                Box(
                    Modifier.align(Alignment.TopEnd)
                        .padding(top = 46.dp, end = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Surf.copy(alpha = 0.85f))
                        .border(1.dp, Brd, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column {
                        MonoText("${prefs.displayTemp(weather.tempC)}${prefs.tempLabel}", 10.sp, Frost, FontWeight.Bold)
                        MonoText(weather.description, 8.sp, Dim)
                    }
                }
            }
        }

        // ── Bottom section: HUD (live) or History (idle) ─────────────
        if (isLive) {
            // Live HUD strip + controls
            LiveHud(
                vehicleState = vehicleState,
                driveState = driveState,
                prefs = prefs,
                hasLocationPerm = hasLocationPerm,
                onRequestPermission = { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                onStart = { recorder.startDrive(sessionId = 0) },
                onPause = { recorder.pauseDrive() },
                onResume = { recorder.resumeDrive() },
                onStop = {
                    recorder.stopDrive()
                    // Refresh history
                    scope.launch(Dispatchers.IO) {
                        drives = DriveDatabase.getInstance(context).driveDao()
                            .getRecentDrives(AppSettings.getMaxSavedDrives(context))
                    }
                },
                modifier = Modifier.weight(0.45f).fillMaxWidth()
            )
        } else {
            // Drive history
            DriveHistoryList(
                drives = drives,
                prefs = prefs,
                selectedId = selectedDriveId,
                onSelect = { drive ->
                    if (selectedDriveId == drive.id) {
                        selectedDriveId = null
                        selectedDrivePoints = emptyList()
                    } else {
                        selectedDriveId = drive.id
                        scope.launch(Dispatchers.IO) {
                            selectedDrivePoints = DriveDatabase.getInstance(context)
                                .driveDao().getPoints(drive.id)
                        }
                    }
                },
                onExport = { driveId ->
                    // TODO: Phase 7 — unified export
                },
                modifier = Modifier.weight(0.55f).fillMaxWidth()
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIVE HUD — telemetry strip + recording controls
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun LiveHud(
    vehicleState: VehicleState,
    driveState: DriveState,
    prefs: UserPrefs,
    hasLocationPerm: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalThemeAccent.current

    Column(
        modifier
            .background(Surf)
            .padding(horizontal = 8.dp)
            .padding(top = 6.dp, bottom = 6.dp)
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1 — Speed · RPM · Gear · Avg RPM
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DataCell("SPD",
                    "${prefs.displaySpeed(vehicleState.speedKph)} ${prefs.speedLabel}",
                    modifier = Modifier.weight(1f))
                DataCell("RPM", "%.0f".format(vehicleState.rpm), modifier = Modifier.weight(1f))
                DataCell("GEAR", vehicleState.gearDisplay, modifier = Modifier.weight(1f))
                DataCell("AVG RPM", "%.0f".format(driveState.avgRpm), modifier = Modifier.weight(1f))
            }

            // Row 2 — Coolant · Oil · Ambient · Fuel %
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DataCell("CLT",
                    if (vehicleState.coolantTempC > -90)
                        "${prefs.displayTemp(vehicleState.coolantTempC)}${prefs.tempLabel}" else "--",
                    modifier = Modifier.weight(1f))
                DataCell("OIL",
                    if (vehicleState.oilTempC > -90)
                        "${prefs.displayTemp(vehicleState.oilTempC)}${prefs.tempLabel}" else "--",
                    modifier = Modifier.weight(1f))
                DataCell("AMB", "${prefs.displayTemp(vehicleState.ambientTempC)}${prefs.tempLabel}", modifier = Modifier.weight(1f))
                DataCell("FUEL", "%.0f%%".format(vehicleState.fuelLevelPct), modifier = Modifier.weight(1f))
            }

            // Row 3 — RDU · PTU · Fuel used · Economy
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DataCell("RDU",
                    if (vehicleState.rduTempC > -90)
                        "${prefs.displayTemp(vehicleState.rduTempC)}${prefs.tempLabel}" else "--",
                    modifier = Modifier.weight(1f))
                DataCell("PTU",
                    if (vehicleState.ptuTempC > -90)
                        "${prefs.displayTemp(vehicleState.ptuTempC)}${prefs.tempLabel}" else "--",
                    modifier = Modifier.weight(1f))
                DataCell("USED", "%.2fL".format(driveState.fuelUsedL), modifier = Modifier.weight(1f))
                val (econVal, econUnit) = if (prefs.speedUnit == "MPH")
                    "%.1f".format(driveState.avgFuelMpg) to "MPG"
                else
                    "%.1f".format(driveState.avgFuelL100km) to "L/100"
                DataCell("ECON", "$econVal $econUnit", modifier = Modifier.weight(1f))
            }

            // Row 4 — Wheel speeds
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DataCell("FL", "${prefs.displaySpeed(vehicleState.wheelSpeedFL)} ${prefs.speedLabel}", modifier = Modifier.weight(1f))
                DataCell("FR", "${prefs.displaySpeed(vehicleState.wheelSpeedFR)} ${prefs.speedLabel}", modifier = Modifier.weight(1f))
                DataCell("RL", "${prefs.displaySpeed(vehicleState.wheelSpeedRL)} ${prefs.speedLabel}", modifier = Modifier.weight(1f))
                DataCell("RR", "${prefs.displaySpeed(vehicleState.wheelSpeedRR)} ${prefs.speedLabel}", modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Recording controls ───────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!driveState.isRecording) {
                // START button
                Button(
                    onClick = {
                        if (!hasLocationPerm) onRequestPermission()
                        else onStart()
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    MonoText("START", 13.sp, Bg, FontWeight.Bold)
                }
            } else if (driveState.isPaused) {
                // RESUME + STOP
                Button(
                    onClick = onResume,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    MonoText("RESUME", 13.sp, Bg, FontWeight.Bold)
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(0.5f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    MonoText("STOP", 13.sp, Bg, FontWeight.Bold)
                }
            } else {
                // PAUSE + STOP
                Button(
                    onClick = onPause,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Warn)
                ) {
                    MonoText("PAUSE", 13.sp, Bg, FontWeight.Bold)
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(0.5f).height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    MonoText("STOP", 13.sp, Bg, FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DRIVE HISTORY LIST
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DriveHistoryList(
    drives: List<DriveEntity>,
    prefs: UserPrefs,
    selectedId: Long?,
    onSelect: (DriveEntity) -> Unit,
    onExport: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalThemeAccent.current

    Column(modifier.background(Surf).padding(horizontal = 8.dp, vertical = 6.dp)) {
        SectionLabel(
            "DRIVE HISTORY",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (drives.isEmpty()) {
            Box(
                Modifier.fillMaxWidth()
                    .background(Surf2, RoundedCornerShape(10.dp))
                    .border(1.dp, Brd, RoundedCornerShape(10.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                MonoLabel("No drives recorded yet", 10.sp, Dim)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(drives, key = { it.id }) { drive ->
                    DriveCard(
                        drive = drive,
                        prefs = prefs,
                        isSelected = selectedId == drive.id,
                        onClick = { onSelect(drive) },
                        onExport = { onExport(drive.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveCard(
    drive: DriveEntity,
    prefs: UserPrefs,
    isSelected: Boolean,
    onClick: () -> Unit,
    onExport: () -> Unit
) {
    val accent = LocalThemeAccent.current
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val durationMs = if (drive.endTime > 0) drive.endTime - drive.startTime else 0L
    val durationStr = formatDuration(durationMs)

    val borderColor = if (isSelected) accent else Brd
    val bgColor = if (isSelected) Surf2 else Surf

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        // Header: date + duration
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MonoText(dateFormat.format(Date(drive.startTime)), 10.sp, Frost, FontWeight.Bold)
            MonoText(durationStr, 10.sp, Mid)
        }

        Spacer(Modifier.height(6.dp))

        // Stats row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (drive.hasGps && drive.distanceKm > 0) {
                val dist = if (prefs.speedUnit == "MPH")
                    "%.1f mi".format(drive.distanceKm * 0.621371)
                else "%.1f km".format(drive.distanceKm)
                StatChip("DIST", dist)
            }
            if (drive.peakRpm > 0) StatChip("RPM", "${drive.peakRpm}")
            if (drive.peakBoostPsi > 0) StatChip("BOOST", "%.1f".format(drive.peakBoostPsi))
            if (drive.maxSpeedKph > 0) {
                val spd = prefs.displaySpeed(drive.maxSpeedKph)
                StatChip("MAX", "$spd ${prefs.speedLabel}")
            }
        }

        // GPS indicator
        if (!drive.hasGps) {
            Spacer(Modifier.height(4.dp))
            MonoLabel("No GPS data", 8.sp, Dim)
        }

        // Export button when selected
        if (isSelected) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f))
            ) {
                MonoText("SHARE", 11.sp, accent, FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MonoLabel(label, 7.sp, Dim)
        MonoText(value, 9.sp, Frost, FontWeight.Bold)
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "Active"
    val secs = ms / 1000
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
