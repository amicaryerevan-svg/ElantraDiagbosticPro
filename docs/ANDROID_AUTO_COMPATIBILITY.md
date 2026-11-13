# Android Auto compatibility — Elantra Diagnostic Pro

## Current repository state

The current app is a native Android `Activity` with a diagnostic-by-symptom menu. It does not yet contain a media browser service, a media session, a Media3 player, or Android for Cars metadata. The current manifest only declares the launcher activity, and the app module has no media or Android for Cars dependencies.

## What works with the factory Hyundai screen

The vehicle connection described by the owner is Android Auto over USB. Android Auto projects supported phone-app experiences; it is not a general-purpose screen-mirroring channel and does not allow an ordinary phone APK to put arbitrary video on the factory display.

## Safe, supported development path

1. Keep vehicle diagnostics in the phone app. Add real OBD-II communication separately, only after selecting and testing a compatible ELM327 adapter and handling Android Bluetooth permissions and adapter differences.
2. For car-screen audio, implement an Android Auto media app using the supported `MediaBrowserService` / `MediaLibraryService` and `MediaSession` architecture (prefer Media3 for new playback work), then validate the app against Android for Cars quality requirements.
3. Do not scrape YouTube streams, embed unofficial YouTube playback, or attempt to bypass Android Auto's app-category and driver-distraction restrictions. YouTube Music playback should be provided by the official YouTube Music app; this project should not impersonate or extract its content.
4. Video playback on the car display is not a supported Android Auto category. Parked video apps are a separate Android Automotive OS capability on supported vehicles, not a feature that can be enabled on this Hyundai merely because Android Auto works.
5. While driving, expose only permitted audio controls through Android Auto. Never try to keep video visible to the driver after shifting into Drive.

## Next implementation milestone

Before adding dependencies, choose a legitimate audio source owned or licensed by this app (for example, user-provided local audio or a permitted stream). Then add a Media3-backed media service, declare its service and automotive metadata in the manifest, and test browsing, play/pause, next/previous, voice search, background playback, and reconnect behavior with the Android Auto Desktop Head Unit and a real vehicle.

## Build

The repository's GitHub Actions workflow builds the debug APK with Java 17, Gradle 8.9, and Android SDK 35 on pushes to `main`. A successful APK build verifies compilation and packaging; it does not certify Android Auto compatibility or Google Play car-app approval.
