# Android Auto media integration

## Local audio library

The app exposes phone-stored music through Media3 `MediaLibraryService` and `MediaLibrarySession`. Android Auto renders its own driver-optimized browsing and playback interface; the phone Activity is not projected onto the car display.

Version 1.18 organizes the local library into two root folders:

- **Все композиции** — the existing flat, title-sorted list of local audio files.
- **Исполнители** — a case-insensitively sorted list of artist folders. Opening an artist shows only that artist's available tracks.

Artist folder IDs are URL-encoded and stable for the artist name, including names containing spaces, punctuation or non-Latin characters. Missing or revoked audio permission returns an empty library instead of crashing the service. The existing `onAddMediaItems` resolver still checks playback requests against current MediaStore items, so folder IDs, unknown IDs and removed files are not sent to ExoPlayer as tracks.

The service requires the user to grant Android's audio-library permission. If permission is revoked or a file is removed, the library may be empty or a previously browsed item may no longer play.

## YouTube Music

Elantra Diagnostic Pro does not extract, proxy, or stream YouTube content. Use the official YouTube Music app and its own Android Auto integration for that service. This project only exposes music files stored on the phone.

## Safety and platform limits

- No video playback is exposed through this Android Auto media service.
- Do not use the phone Activity or read diagnostic reports while driving.
- Android Auto decides the in-car media UI and available controls; this app cannot force unsupported content or bypass host restrictions.
- A successful build verifies packaging and unit tests, but actual browsing/playback must still be tested with Android Auto and a compatible head unit.
