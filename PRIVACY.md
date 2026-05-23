# Privacy Policy

**App:** MediaCenter (`io.github.reneknap.mediacenter`)
**Effective date:** 2026-05-23

> This is a stub privacy policy for the pre-release app. It will be hosted publicly
> and linked from the app's store listing before the Play Store release.

## Summary

MediaCenter does **not** collect, store, or transmit any personal data. The app
itself has no network access, no analytics, no advertising, and no third-party
tracking. The only data it keeps is a few preferences stored locally on your device
(see below).

## What the app accesses

- **Audio files in a folder you choose.** When you pick a folder, the app uses
  Android's Storage Access Framework to read the audio files inside it, solely to
  list and play them. The app does **not** request broad storage permissions and
  cannot see files outside the folder you grant.

## What the app stores

The app stores a small amount of data **locally on your device**, using Android's
DataStore:

- the folder you selected (so it can be reopened),
- your theme preference,
- your current playback/queue state,
- how many times you have opened the app, and whether you have dismissed the
  one-time "Support development" hint — used only to decide when (and whether) to
  show that hint, and never to identify you.

The app never uploads this data and removes it when you uninstall. Note that
Android's built-in **Auto Backup** — a system feature you control in your device's
backup settings, not something the app initiates — may include these preferences in
your device's backup to your Google account. The app itself transmits nothing and
has no access to that backup.

## Network, analytics, and ads

- The app declares **no internet permission** and makes no network requests of its
  own.
- There is **no analytics, no advertising, and no third-party tracking SDK.**

The only outbound action is **initiated by you**: the optional "Support development"
dialog can open external donation links (Ko-fi, PayPal.me) in your browser or another
app. Once a link opens, that external service handles it under **its own** privacy
policy — MediaCenter sends it nothing about you.

## Permissions

MediaCenter requests only what playback needs:

- **Notifications** (`POST_NOTIFICATIONS`) — to show the playback notification with
  transport controls.
- **Foreground service** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`)
  — to keep music playing in the background.
- **Folder access** (granted by you via the Storage Access Framework) — to read and
  play the folder you pick.

It requests **no** storage, location, contacts, microphone, camera, or internet
permissions.

## Data sharing and children

The app shares no data with anyone, sells no data, and contains nothing directed
specifically at children. Because no personal data is collected, none can be shared.

## Changes to this policy

If this policy changes, the updated version will be published in the project
repository with a new effective date.

## Contact

Questions about this policy: **rene.knap.92@googlemail.com**
