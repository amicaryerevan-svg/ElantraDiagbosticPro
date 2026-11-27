# Android Auto media integration

## Local audio library

The app exposes local music through Media3 `MediaLibraryService` and `MediaLibrarySession`. Android Auto renders its own driver-optimized browsing and playback interface; the phone Activity is not projected onto the car display.

Version 1.17 adds `onAddMediaItems` resolution for legacy MediaBrowser playback requests. Some clients select a browsed track by `mediaId` without carrying the original MediaStore URI. The service now resolves requested IDs against the current local MediaStore library and returns the canonical playable `MediaItem` with its content URI. Unknown IDs and files that have been removed are omitted instead of being passed to ExoPlayer as unplayable entries.

The service still requires the user to grant the Android audio-library permission. If permission is revoked or a file is removed, the library may be empty or a previously browsed item may no longer play. Reopen or refresh the media library after granting permission.

## YouTube Music

Elantra Diagnostic Pro does not extract, proxy, or control YouTube Music streams. Use the official YouTube Music app and its own Android Auto integration for that service. This project only exposes music files stored on the phone.

## Safety and platform limits

- No video playback is exposed through this Android Auto media service.
- Do not use the phone Activity or read diagnostic reports while driving.
- Android Auto decides the in-car media UI and available controls; this app cannot force unsupported content or bypass host restrictions.
- A successful build verifies packaging and unit tests, but actual browsing/playback must still be tested with Android Auto and a compatible head unit.
