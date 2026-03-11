"""Device profile: WiCAN Pro (ESP32-S3) — wican-fw v4.48p.

UNVERIFIED — requires actual WiCAN Pro hardware for build and flash testing.
Anchor strings extracted from the v4.48p tag via GitHub API; some anchors that
exist in the USB build are MISSING in the Pro firmware and marked as None.
"""

PROFILE = {
    "name": "pro",
    "description": "WiCAN Pro (ESP32-S3)",
    "wican_tag": "v4.48p",
    "soc": "esp32s3",
    "idf_target": "esp32s3",
    "sdkconfig": "sdkconfig.defaults.pro",
    "partitions": "partitions_openrs_pro.csv",
    "output_prefix": "openrs-fw-pro",

    # HARDWARE_VER is set in wican-fw's CMakeLists.txt per tag.
    # Pro tag already has: set(HARDWARE_VER ${WICAN_PRO})  → 4

    "anchors": {
        # The Pro's config_server.c ws_handler does NOT use xQueueSend(*xRX_Queue).
        # The WebSocket path was refactored in v4.4x. The universal SLCAN probe in
        # slcan.c handles firmware detection for all transports (TCP and WS).
        "ws_probe_queue": None,

        # wc_mdns_init() is absent in the Pro's main.c. The CAN TX registration
        # needs a different anchor. TODO: identify the right insertion point after
        # the last can_enable() block in app_main once we have the hardware.
        "can_tx_register": None,
        "can_tx_register_replacement": None,
    },

    # Pro uses TCP SLCAN — the universal slcan.c probe covers firmware detection.
    "has_ws_probe": False,

    # CAN TX (write) support requires finding the Pro-specific anchor.
    # Read-only features (passive CAN, OBD polling, REST GET) work without it.
    "has_can_tx": False,

    "verified": False,
}
