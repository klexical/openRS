package com.openrs.dash.can

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * MeatPi Pro hardware adapter — v2.1+ implementation.
 *
 * The MeatPi Pro offers:
 *   • USB-C form factor (like WiCAN)
 *   • Built-in GPS module (u-blox M10)
 *   • Higher CAN throughput (~2000 fps vs WiCAN ~1400 fps)
 *   • Native TCP + BLE connectivity
 *   • OTA firmware updates
 *
 * STATUS: STUB — Not yet implemented. This class will be fleshed out in v2.1
 * once hardware is available for testing. The [HardwareAdapter] interface
 * ensures the rest of the app requires zero changes when this is ready.
 *
 * Connection: TCP socket to MeatPi's hotspot (192.168.4.1:3333 default)
 * GPS:        NMEA sentences on a secondary port or via BLE characteristic
 */
class MeatPiConnection : HardwareAdapter {

    override val adapterType: AdapterType = AdapterType.MEATPI
    override val displayName: String      = "MeatPi Pro"
    override val firmwareVersion: String? = null
    override val hasGps: Boolean          = true
    override val isConnected: Boolean     get() = _connected

    override val frames: Flow<String>        = emptyFlow()
    override val gpsData: StateFlow<GpsData?> = MutableStateFlow(null)

    private var _connected = false

    override suspend fun connect(host: String, port: Int) {
        // TODO v2.1: open TCP socket to MeatPi at host:port
        // TODO v2.1: start NMEA GPS reader on GPS port
        throw NotImplementedError("MeatPi Pro support is coming in openRS_ v2.1")
    }

    override suspend fun send(frame: String) {
        // TODO v2.1: write SLCAN frame to TCP socket
    }

    override fun disconnect() {
        _connected = false
        // TODO v2.1: close TCP socket and GPS reader
    }
}
