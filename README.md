# Localplay

A clean, ad-free Android media player for local files — music and video.

Localplay plays music and video from a folder you pick on your device. It's
deliberately simple: no ads, no accounts, no tracking, no in-app purchases. Pick
a folder, press play, and the music keeps going in the background with full
controls from the notification and lockscreen. The app is free and open source.

> **Status:** early / pre-release (`v0.1.0`). Music and video playback work end
> to end; a public Play Store release is in preparation (see [Roadmap](#roadmap)).

## Features

- **Pick a folder** to play from, via the Storage Access Framework — no broad
  storage permissions required.
- **Media scan** of the chosen folder for supported audio and video files.
- **Playback queue** with **sequential** and **shuffle** modes.
- **Video playback** — inline player with fullscreen and orientation handling;
  video keeps playing as audio in the background.
- **Background playback** through a foreground service, so playback continues
  when the app is closed.
- **Media-style notification** and **lockscreen controls**: play/pause, next,
  previous.
- **Headset and Bluetooth** button handling.
- **Audio focus and ducking** — pauses or lowers volume for calls and other apps.
- **Now-playing screen** and a **queue / track-list view**.
- **Material 3 UI** with **dark mode** and dynamic (Material You) theming.

## Tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Playback:** AndroidX Media3 (ExoPlayer + MediaSession)
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Persistence:** DataStore + kotlinx.serialization
- **File access:** AndroidX DocumentFile (Storage Access Framework)

## Requirements

- Android Studio (recent stable)
- JDK 17
- Android SDK 35

Min SDK 26 · Target SDK 35.

## Build & run

```bash
git clone <repository-url>
cd localplay
./gradlew assembleDebug
```

Or open the project in Android Studio and run the `app` configuration on a device
or emulator. On Windows, use `gradlew.bat` instead of `./gradlew`.

## Roadmap

1. **Foundation & basic music playback** — folder picker, scan, queue, play/pause/
   next/previous, sequential & shuffle. ✅
2. **Background playback & notification widget** — MediaSession, foreground service,
   notification & lockscreen controls, headset/Bluetooth, audio focus. ✅
3. **UI polish** — now-playing screen, queue view, dark mode & dynamic theming. ✅
4. **Pre-release essentials** — branding, donation/rating hooks, license & docs. ✅
5. **Video support** — local video playback sharing the picked-folder model. ✅
6. **Play Store launch** — signed release, store listing, public repository. ⏳

## Support

Localplay is free and won't nag you with ads or pop-ups. If you'd like to support
development, the app has a small, opt-in "Support development" dialog with external
donation links (Ko-fi and PayPal.me). Donations are entirely voluntary and the app
stays fully functional without them.

## Privacy

Localplay does not collect, store, or transmit personal data, and the app itself
has no network access. See [PRIVACY.md](PRIVACY.md) for details.

## License

Released under the [MIT License](LICENSE). © 2026 Rene Knap.
