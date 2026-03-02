# Hardware Setup Guide

## WiCAN OBD-II Adapter

openRS connects to a [MeatPi WiCAN](https://www.mouser.com/ProductDetail/MeatPi/WICAN-USB-C3?qs=rQFj71Wb1eVDX2eEy0FC7A%3D%3D) adapter via Wi-Fi TCP.

### Configuration

The WiCAN should be in its default configuration:

| Setting | Value |
|---------|-------|
| Mode | ELM327 TCP |
| Wi-Fi SSID | `WiCAN_XXXXXX` |
| IP Address | `192.168.80.1` |
| Port | `3333` |
| Protocol | CAN 500 kbps (auto-detected via `ATSP6`) |

### First-Time Setup

**Step 1 — Physical install**
1. Locate the OBD-II port: under the steering column, left of the hood release lever
2. Plug in the WiCAN — the angled connector faces down to avoid knee contact
3. Turn the car to ACC or RUN — the WiCAN LED will flash then go solid

**Step 2 — Connect your phone to the WiCAN hotspot**
1. On your phone, open WiFi settings
2. Connect to the network named `WiCAN_XXXXXX` (last 6 chars of MAC, printed on device)
3. Default password: `bla2020blabla`

**Step 3 — Verify WiCAN configuration**
1. Open a browser on your phone → go to `http://192.168.80.1`
2. The WiCAN web interface loads
3. Confirm these settings under **CAN Settings / Device Settings**:

| Setting | Required Value |
|---------|----------------|
| Mode | ELM327 |
| Protocol | CAN (auto / 500 kbps) |
| TCP Port | 3333 |
| WiFi Mode | AP (Access Point) |

4. If anything differs, update and press **Save** — the device reboots

**Step 4 — Connect openRS_**
1. Open the openRS_ app
2. Tap **● OFFLINE** in the top-right header → it changes to **● CONNECTED**
3. The app connects to `192.168.80.1:3333`, runs the ELM327 handshake, and begins streaming data

> **Tip:** The WiCAN WiFi and Android Auto Wireless use different radios. Your phone can be on WiCAN WiFi while AA runs over Bluetooth — they don't conflict.

> **Tip:** The WiCAN will slowly drain the 12V battery if left plugged in with the ignition off. Unplug when parked for extended periods.

### Changing the WiCAN IP / Port

If your WiCAN is configured in **Client mode** (joining your phone's hotspot), the IP address will be different from `192.168.80.1`. You can update the connection target inside the app:

**Settings → WiCAN Host / Port**

The app saves your last-used values and reconnects automatically.

## OBD-II Port Pinout (Focus RS MK3)

| Pin | Signal | Notes |
|-----|--------|-------|
| 4 | Chassis Ground | |
| 5 | Signal Ground | |
| 6 | HS-CAN High | 500 kbps — primary bus |
| 14 | HS-CAN Low | 500 kbps — primary bus |
| 3 | MS-CAN High | 125 kbps — secondary bus (TPMS broadcast) |
| 11 | MS-CAN Low | 125 kbps — secondary bus |
| 16 | Battery +12V | Always on |

openRS uses the HS-CAN bus (pins 6/14) for all data. The standard WiCAN connects to HS-CAN automatically.

## Tested Head Units

| Unit | Type | AA Connection | Status |
|------|------|---------------|--------|
| Ford Sync 3 | OEM | USB | ✅ Works |
| Kenwood DMX907S | Aftermarket | USB + Wireless | ✅ Works |
| Pioneer DMH-WT76NEX | Aftermarket | USB | ✅ Works |
| Android Auto DHU | Desktop emulator | ADB | ✅ Works (dev only) |
