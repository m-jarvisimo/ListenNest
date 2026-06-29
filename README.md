# ListenNest

**ListenNest** is a local-only, offline-first Android audiobook app for personal library folders.

This repository is currently at **release pre0.2**: it works at a basic level, but it is still an early build and will continue to improve.

## What it does right now

- Pick a local folder using Android's folder picker
- Scan the selected folder for audiobook content
- Treat **1 folder = 1 book**
- Avoid recursive scanning
- Recognize supported audio formats:
  - `mp3`
  - `m4b`
  - `flac`
- Show imported books in a dark, minimal library screen
- Open a book into a player/detail screen
- Play real audio through **Media3 / ExoPlayer**
- Show a simple track list and chapter placeholders
- Save and restore resume position per book

## Current build

This project is set up as a standard Android app:

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Playback:** Media3 / ExoPlayer
- **Persistence:** Room
- **Storage access:** SAF folder picker
- **Architecture:** MVVM + repository-style separation

## Requirements

- Android Studio or a compatible Android build environment
- Android SDK platform 34
- Java 21 for the current build environment

## Build

From the project root:

```bash
./gradlew assembleDebug
```

The debug APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## App compatibility

Current configuration:

- `minSdk = 26`
- `targetSdk = 34`
- `compileSdk = 34`

That means the app is intended for Android 8.0+ devices, including Android 16.

## Release pre0.2 notes

This version is a functional prototype, not a finished release.

### Strengths

- Basic folder import works
- Playback is wired up
- Resume state is saved
- The app builds cleanly

### Known limitations

- UI is still intentionally simple
- Chapter handling is still a placeholder
- Library metadata is minimal
- There is no cloud sync or account system
- This is an offline-only app by design

## Project goal

The long-term goal for ListenNest is a clean, private audiobook app with:

- folder-based library management
- one-book-at-a-time playback
- resume support
- bookmarks
- embedded chapters when present
- minimal dark UI
- no cloud dependency

## Repository layout

```text
app/
  src/main/java/com/k2s/listennest/
  src/main/res/
gradle/
README.md
LISTENNEST_PROJECT_PLAN.md
```

## License

No license has been added yet.
