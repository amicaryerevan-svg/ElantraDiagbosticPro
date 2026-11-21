# Bluetooth OBD-II setup

## Supported adapter type

Version 1.4 introduced the initial read-only connection test for an already-paired **Bluetooth Classic RFCOMM SPP** ELM327-compatible adapter. Version 1.6 added one-time readings for supported standard PIDs. Version 1.8 adds a read-only request for stored emission-related powertrain DTCs (OBD-II Mode 03).

The app does not support BLE-only adapters or Wi-Fi OBD-II adapters yet. Some inexpensive ELM327 clones advertise compatibility but do not implement the standard SPP service reliably.

## Pair the adapter

1. Park the Elantra safely and switch the ignition to ON. The engine does not need to be started for the initial connection check.
2. Insert the OBD-II adapter into the vehicle's diagnostic connector.
3. On the Android phone, open **Settings → Bluetooth** and pair the adapter manually. Common PINs are 1234 or 0000; use the PIN printed by the adapter manufacturer if different.
4. Open Elantra Diagnostic Pro and choose **Подключить ELM327 Bluetooth**.
5. Grant **Nearby devices / Bluetooth Connect** permission when Android asks.
6. Select the already-paired adapter from the list.

The app intentionally reads only the phone's bonded-device list. It does not perform Bluetooth discovery or scan for nearby devices.

## What the connection test does

After opening the standard Bluetooth Classic SPP connection, the app sends the ELM initialization commands:

- `ATZ` — reset and identify the adapter
- `ATE0`, `ATL0`, `ATS0`, `ATH0` — configure response formatting
- `ATSP0` — request automatic OBD protocol selection
- `0100` — read the standard PID 00 supported-PID bitmap from the ECU

A response containing `41 00` (or the compact equivalent `4100`) indicates that the ECU answered the supported-PID query. The app decodes the four-byte PID support bitmap and lists which standard PIDs 01–20 the ECU reports as supported. It highlights PID 05 (coolant temperature) and PID 0C (engine RPM).

When the ECU advertises support, the app makes one read-only request for engine RPM (`010C`) and coolant temperature (`0105`). RPM is decoded as `(256 × A + B) / 4`; coolant temperature is `A − 40 °C`. These are one-time values captured during connection, not continuous telemetry.

After a valid PID 0100 handshake, version 1.7 also sends **Mode 03** to read stored emission-related powertrain DTCs. A positive response starts with `43`; each DTC is decoded from two data bytes into its standard five-character form (P, C, B, or U family). A `00 00` pair marks the end of the reported codes. The parser rejects missing or incomplete responses instead of presenting partial codes as valid.

A valid Mode 03 response with no codes is shown separately from an adapter timeout or malformed response. Mode 03 does not provide a complete scan of every vehicle module: ABS, airbag/SRS, body electronics, and manufacturer-specific transmission data may require a Hyundai-capable enhanced diagnostic tool.

A Bluetooth connection without a valid `41 00` bitmap means the phone reached the adapter, but the ECU communication handshake was not confirmed. In that case, Mode 03 is not requested.

## Automatic disconnect after a one-time scan

Version 1.9 closes the Bluetooth RFCOMM socket after the final supported-PID and Mode 03 responses have been collected, before the result dialog is shown. The app currently captures a one-time snapshot rather than streaming live telemetry, so keeping the adapter connected after the scan provides no benefit and can prevent another diagnostic app from using it. Dismissing the result dialog therefore does not leave the ELM327 connection open.

If the user cancels while the connection or a command is still in progress, the client closes the socket and stops the pending diagnostic sequence. Android documents that closing a BluetoothSocket from another thread can abort a blocking connect operation.

## ELM327 response framing and timeout handling

Version 1.8 treats the ELM327 prompt (`>`) as the required end of every command response. Bytes received in multiple Bluetooth reads are accumulated until that prompt arrives. If the timeout expires first, the command is considered incomplete and the current diagnostic transaction is stopped; the app does not send another PID or Mode 03 request on a potentially desynchronized stream. This avoids interpreting delayed bytes from one command as the response to the next. Replies are also capped at 8 KiB to prevent an unexpectedly noisy adapter from growing the response buffer without limit.

## Local diagnostic history

Version 1.10 stores the most recent 20 completed Bluetooth diagnostic reports locally in the app's private preferences. Each entry includes the date/time, paired adapter name, and the same summary shown after the scan (ECU handshake, supported PIDs, one-time RPM/coolant values, and Mode 03 result). The **История диагностики** menu opens saved reports; the user can clear all saved reports from that screen. No diagnostic history is uploaded or synchronized.

A report is saved only after the diagnostic client has completed its one-time exchange and returned a result. A failed Bluetooth connection that never reaches the result callback is not stored. History is a convenience record, not a live vehicle connection or a service record verified by a repair shop.

## Read-only safety

The app sends only adapter setup commands and standard read-only OBD requests: Mode 01 PIDs and Mode 03 DTC retrieval. It does **not** send Mode 04 clear/reset commands, actuator tests, coding, or vehicle-control commands. Use the diagnostic screen only while safely parked; do not watch or operate it while driving.

## Android permissions

- Android 12 (API 31) and newer: runtime **BLUETOOTH_CONNECT** permission.
- Android 11 and older: legacy **BLUETOOTH** permission.
- No **BLUETOOTH_SCAN** permission is requested because the app only lists devices that the user has already paired in Android Settings.

## Troubleshooting

- If the adapter is not listed, pair it first in Android Settings.
- Close other OBD apps that may already be using the adapter.
- Confirm Bluetooth is enabled and the adapter is powered.
- If Bluetooth connects but the ECU does not answer, check ignition position, adapter fit, protocol compatibility, and clone quality.
- If Mode 03 returns `NO DATA`, this is not proof that the car has no faults; the ECU/adapter may not have returned a valid response.
- A Wi-Fi or BLE adapter will not work with this SPP implementation.

## Known limitations and next step

Version 1.9 performs a one-time PID capability query, reads RPM and coolant temperature when supported, and parses standard Mode 03 DTCs. The RFCOMM connection is released as soon as the one-time scan finishes. It does not continuously poll values, read freeze-frame data, retrieve manufacturer-specific enhanced codes, or clear faults. The next step is physical validation with a real ELM327 and the Elantra, followed by better code descriptions and connection-reliability tests.

CI can verify parser tests, compilation, and APK packaging, but only a physical ELM327 adapter and the Elantra can confirm the Bluetooth and ECU handshake end to end.
