# Android Auto compatibility — Elantra Diagnostic Pro

## Current repository state

The app now includes a Media3-backed `MediaLibraryService` and `MediaLibrarySession`, the Android Auto media metadata declaration, foreground media-playback permissions, and a permission flow for local audio access. The service exposes music indexed by Android `MediaStore` to Android Auto.

## What works with the factory Hyundai screen

The vehicle connection described by the owner is Android Auto over USB. Android Auto projects supported phone-app experiences; it is not a general-purpose screen-mirroring channel and does not allow an ordinary phone APK to put arbitrary video on the factory display.

## Safe, supported development path

1. Keep vehicle diagnostics in the phone app. Add real OBD-II communication separately, only after selecting and testing a compatible ELM327 adapter and handling Android Bluetooth permissions and adapter differences.
2. For car-screen audio, implement an Android Auto media app using the supported `MediaBrowserService` / `MediaLibraryService` and `MediaSession` architecture (prefer Media3 for new playback work), then validate the app against Android for Cars quality requirements.
3. Do not scrape YouTube streams, embed unofficial YouTube playback, or attempt to bypass Android Auto's app-category and driver-distraction restrictions. YouTube Music playback should be provided by the official YouTube Music app; this project should not impersonate or extract its content.
4. Video playback on the car display is not a supported Android Auto category. Parked video apps are a separate Android Automotive OS capability on supported vehicles, not a feature that can be enabled on this Hyundai merely because Android Auto works.
5. While driving, expose only permitted audio controls through Android Auto. Never try to keep video visible to the driver after shifting into Drive.

## Current implementation and limitations

- The app can request access to the phone's local audio library and expose indexed music tracks through Android Auto.
- The phone app includes a shortcut to launch the official YouTube Music app when installed. This does not embed YouTube or add YouTube video playback to the factory display.
- The service uses Media3 1.5.1 and ExoPlayer. Android Auto supplies the car-optimized browsing and playback UI.
- YouTube and YouTube Music streams are not extracted or embedded. Use the official YouTube Music app for its catalog.
- The implementation has not yet been verified on a physical Hyundai head unit. The CI APK build checks compilation and packaging only.

## Next implementation milestone

Run the GitHub Actions build, install the APK on an Android phone, grant audio-library permission, and validate browsing and playback in the Android Auto Desktop Head Unit and the Hyundai. Then improve library refresh, paging, search, and playback error reporting.

## Build

The repository's GitHub Actions workflow builds the debug APK with Java 17, Gradle 8.9, and Android SDK 35 on pushes to `main`. A successful APK build verifies compilation and packaging; it does not certify Android Auto compatibility or Google Play car-app approval.
