package com.openrs.dash.can

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Hardware abstraction layer for CAN-over-WiFi adapters.
 *
 * Currently implemented by [WiCanConnection] (WiCAN / openRS_ firmware).
 * Future: [MeatPiConnection] for MeatPi Pro (USB-C, GPS-integrated).
 *
 * Connection types supported:
 *  - WICAN:   WiCAN-USB-C3 running stock or openRS_ firmware (SLCAN over WebSocket)
 *  - MEATPI:  MeatPi Pro (TCP/BLE, native GPS, higher bandwidth — v2.1+)
 */
interface HardwareAdapter {

    /** Type identifier used in UI / diagnostics */
    val adapterType: AdapterType

    /** Display name for connection status (e.g. "WiCAN", "MeatPi Pro") */
    val displayName: String

    /** Firmware version string as reported by the device, or null if unknown */
    val firmwareVersion: String?

    /** Whether the adapter has a built-in GPS unit */
    val hasGps: Boolean

    /** Whether the adapter is currently connected */
    val isConnected: Boolean

    /**
     * Raw SLCAN frame strings from the CAN bus.
     * Format: e.g. "t1B0803400000000000\r"
     * Each emission is one complete SLCAN frame (terminated by \r).
     */
    val frames: Flow<String>

    /**
     * GPS location data (latitude, longitude, altitude, speed, heading).
     * Only non-empty when [hasGps] is true and a fix is available.
     */
    val gpsData: StateFlow<GpsData?>

    /** Connect to the adapter at [host]:[port]. Suspends until connected or throws. */
    suspend fun connect(host: String, port: Int)

    /** Send a raw SLCAN frame string to the adapter. */
    suspend fun send(frame: String)

    /** Disconnect and release resources. */
    fun disconnect()
}

enum class AdapterType {
    WICAN,    // WiCAN-USB-C3 (current hardware)
    MEATPI    // MeatPi Pro (future hardware)
}

/** GPS position and motion data from adapters with integrated GPS. */
data class GpsData(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double,
    val speedKph: Double,
    val headingDeg: Float,
    val accuracyM: Float,
    val timestampMs: Long = System.currentTimeMillis()
)
