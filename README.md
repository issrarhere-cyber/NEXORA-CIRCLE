# NEXORA-CIRCLE

**Connect beyond the universe.** A space and galaxy themed social network for Android.

## Current status: Stage 1

Working now:
- Android app (Kotlin + Jetpack Compose) with animated galaxy background and dark theme
- Bottom navigation: Orbit, Comms, Arcade, Nexora-X, Me
- Nexora-X translator and keyboard, driven by JSON tables (tables are empty until you supply your language)
- Firebase security rules for users, posts, chats and file storage
- GitHub Actions workflow that builds an installable APK

Not built yet (clearly marked as placeholders inside the app): login, profiles, feed,
messaging, groups, notifications, games. See `docs/ARCHITECTURE.md` for the stage plan.

## Get the APK using only your phone

1. Create a new empty repository on GitHub named `NEXORA-CIRCLE` (new, separate from your other projects).
2. Upload all files from this project to it, keeping the folder structure.
3. Open the repository's **Actions** tab, choose **Build Android APK**, and tap **Run workflow**.
4. When the run turns green, open it and download the artifact **NEXORA-CIRCLE-debug-apk**.
5. Unzip it, open `app-debug.apk` on your phone and allow "install unknown apps" when asked.

If the build turns red, open the failed run and send me the error text. I will fix it.

## Adding your Nexora-X language

Edit `app/src/main/assets/nexora_x/alphabet.json` and `numbers.json`.
Instructions are in `app/src/main/assets/nexora_x/README.md`.
