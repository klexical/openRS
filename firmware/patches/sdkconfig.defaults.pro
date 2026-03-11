# openrs-fw sdkconfig defaults for ESP32-S3 (WiCAN Pro hardware)
# Applied by: idf.py set-target esp32s3
#
# *** UNVERIFIED — requires actual WiCAN Pro hardware for testing ***
# Settings extracted from meatpiHQ/wican-fw v4.48p sdkconfig.
# HARDWARE_VER is set in CMakeLists.txt (WICAN_PRO = 4), not here.

# WebSocket support — config_server.c ws_handler
CONFIG_HTTPD_WS_SUPPORT=y

# HTTP server tuning
CONFIG_HTTPD_MAX_REQ_HDR_LEN=512
CONFIG_HTTPD_MAX_URI_LEN=512

# BT stack — Bluedroid (same as USB build)
CONFIG_BT_ENABLED=y
CONFIG_BT_BLUEDROID_ENABLED=y
CONFIG_BT_BLE_ENABLED=y

# BLE 4.2 legacy advertising API — required by ble.c
CONFIG_BT_BLE_42_FEATURES_SUPPORTED=y

# FreeRTOS backward compat — wican-fw uses portTickType
CONFIG_FREERTOS_ENABLE_BACKWARD_COMPATIBILITY=y

# NVS + FAT filesystem
CONFIG_FATFS_LFN_HEAP=y

# Flash size — WiCAN Pro has 16MB flash
CONFIG_ESPTOOLPY_FLASHSIZE_16MB=y
CONFIG_ESPTOOLPY_FLASHSIZE="16MB"

# PSRAM — Pro has 8MB octal PSRAM (used by heap_caps_malloc with MALLOC_CAP_SPIRAM)
CONFIG_SPIRAM=y
CONFIG_SPIRAM_MODE_OCT=y
CONFIG_SPIRAM_TYPE_AUTO=y
CONFIG_SPIRAM_SPEED_80M=y

# Custom partition table — dual OTA with rollback
CONFIG_PARTITION_TABLE_CUSTOM=y
CONFIG_PARTITION_TABLE_CUSTOM_FILENAME="partitions_openrs_pro.csv"
CONFIG_PARTITION_TABLE_OFFSET=0x8000
