# Bluetooth OBD-II setup

## Supported adapter type

Version 1.4 adds an initial read-only connection test for an already-paired **Bluetooth Classic RFCOMM SPP** ELM327-compatible adapter.

It does not support BLE-only adapters or Wi-Fi OBD-II adapters yet. Some inexpensive ELM327 clones advertise compatibility but do not implement the standard SPP service reliably.

## Pair the adapter

1. Park the Elantra safely and switch the ignition to ON. The engine does not need to be started for the initial connection check.
2. Insert the OBD-II adapter into the vehicle's diagnostic connector.
3. On the Android phone, open **Settings → Bluetooth** and pair the adapter manually. Common PINs are 1234 or 0000; use the PIN printed by the adapter manufacturer if different.
4. Open Elantra Diagnostic Pro and choose **Подключить ELM327 Bluetooth**.
5. Grant **Nearby devices / Bluetooth Connect** permission when Android asks.
6. Select the already-paired adapter from the list.

The app intentionally reads only the phone's bonded-device list. It does not perform Bluetooth discovery or scan for nearby devices.

## What the first test does

After opening the standard Bluetooth Classic SPP connection, the app sends the ELM initialization commands:

- `ATZ` — reset and identify the adapter
- `ATE0`, `ATL0`, `ATS0`, `ATH0` — configure response formatting
- `ATSP0` — request automatic OBD protocol selection
- `0100` — read the standard PID 00 supported-PID bitmap from the ECU

A response containing `41 00` (or the compact equivalent `4100`) indicates that the ECU answered the supported-PID query. A Bluetooth connection without that response means the phone reached the adapter, but ECU communication has not yet been confirmed.

No trouble codes are cleared, no actuator tests are run, and no vehicle-control commands are sent.

## Android permissions

- Android 12 (API 31) and newer: runtime **BLUETOOTH_CONNECT** permission.
- Android 11 and older: legacy **BLUETOOTH** permission.
- No **BLUETOOTH_SCAN** permission is requested because the app only lists devices that the user has already paired in Android Settings.

## Troubleshooting

- If the adapter is not listed, pair it first in Android Settings.
- Close other OBD apps that may already be using the adapter.
- Confirm Bluetooth is enabled and the adapter is powered.
- If Bluetooth connects but the ECU does not answer, check ignition position, adapter fit, protocol compatibility, and clone quality.
- A Wi-Fi or BLE adapter will not work with this first SPP implementation.

## Known limitations and next step

The current implementation verifies the SPP link and performs only the `0100` supported-PID query. It does **not** yet decode live values such as RPM, coolant temperature, vehicle speed, or diagnostic trouble codes. Those require a separate, tested read-only PID/DTC layer.

CI can verify compilation and APK packaging, but only a physical ELM327 adapter and the Elantra can confirm the Bluetooth and ECU handshake end to end.
