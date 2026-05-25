# Play Store listing metadata

Text and graphics for the Google Play listing, kept in the fastlane / F-Droid
`metadata` layout so the copy is reviewable in git and can later be uploaded
with `fastlane supply`.

## Layout

- `<locale>/title.txt` — app title (max 30 characters)
- `<locale>/short_description.txt` — short description (max 80 characters)
- `<locale>/full_description.txt` — full description (max 4000 characters)
- `<locale>/images/phoneScreenshots/` — phone screenshots (at least 2)
- `<locale>/images/featureGraphic.png` — 1024×500 feature graphic
- `<locale>/images/icon.png` — 512×512 high-res icon

Locales present: `en-US`, `de-DE`.

## Still to add (needs the running app on a device or emulator)

- Phone screenshots: at least one music view and one video view per locale.
- Feature graphic (1024×500).
- 512×512 high-res icon (can be exported from the existing launcher icon).
