package com.openrs.dash

import com.openrs.dash.can.CanDecoder
import com.openrs.dash.data.DriveMode
import com.openrs.dash.data.VehicleState
import org.junit.Assert.*
import org.junit.Test

class CanDecoderTest {

    private val blank = VehicleState()

    @Test
    fun `decode RPM frame`() {
        // 0x090 (ID_ENGINE_RPM): RPM = ((byte4 & 0x0F) << 8 | byte5) * 2
        // RPM 6000 → raw 3000 → byte4 low nibble = 0x0B, byte5 = 0xB8
        // Baro: byte2 × 0.5 kPa → 101 kPa = byte2 = 202 = 0xCA
        val data = byteArrayOf(0x00, 0x00, 0xCA.toByte(), 0x00, 0x0B, 0xB8.toByte())
        val result = CanDecoder.decode(0x090, data, blank)
        assertNotNull(result)
        assertEquals(6000.0, result!!.rpm, 1.0)
        assertEquals(101.0, result.barometricPressure, 0.5)
    }

    @Test
    fun `decode speed frame`() {
        // 0x130 (ID_SPEED): speed = word(6,7) × 0.01 km/h
        // 100 km/h = 10000 raw = 0x2710
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x27, 0x10)
        val result = CanDecoder.decode(0x130, data, blank)
        assertNotNull(result)
        assertEquals(100.0, result!!.speedKph, 0.1)
    }

    @Test
    fun `unknown CAN ID returns null`() {
        val data = byteArrayOf(0x00, 0x01, 0x02)
        val result = CanDecoder.decode(0x999, data, blank)
        assertNull(result)
    }

    @Test
    fun `decode throttle frame`() {
        // 0x076 (ID_THROTTLE): throttle = byte0 × 0.392
        // 100% ≈ byte0 = 255 → 99.96%
        val data = byteArrayOf(0xFF.toByte())
        val result = CanDecoder.decode(0x076, data, blank)
        assertNotNull(result)
        assertTrue(result!!.throttlePct > 99.0)
    }

    @Test
    fun `short data returns null`() {
        // RPM frame needs at least 6 bytes
        val data = byteArrayOf(0x00, 0x01)
        val result = CanDecoder.decode(0x090, data, blank)
        assertNull(result)
    }

    @Test
    fun `drive mode decode`() {
        // 0x1B0 (ID_DRIVE_MODE): byte6 upper nibble: 0=Normal, 1=Sport+Track, 2=Drift
        // byte6 = 0x20 → upper nibble = 2 → Drift
        val data = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x20, 0x00)
        val result = CanDecoder.decode(0x1B0, data, blank)
        assertNotNull(result)
        assertEquals(DriveMode.DRIFT, result!!.driveMode)
    }
}
