# ListenNest Media Playback Notification Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Add an Android media playback notification to ListenNest so users can see what is playing and pause/resume playback from the notification shade while audio continues in the background.

**Architecture:** Keep the existing ExoPlayer-based playback flow, but move playback ownership into a dedicated Media3-backed foreground playback service with a MediaSession. The UI should control playback through a shared controller layer, while the service owns the player and publishes a media-style notification with artwork, title, progress, and transport controls.

**Tech Stack:** Kotlin, Jetpack Compose, Media3 / ExoPlayer, MediaSession, foreground service, Android notifications.

---

## Current state to build from

ListenNest already has:
- `PlayerViewModel.kt` with an `ExoPlayer` instance
- `PlayerScreen.kt` with play/pause and seek controls
- background playback that already survives leaving the app
- no notification service, no MediaSession, and no notification actions yet

That means the feature is mostly a **playback plumbing** change, not a UI rewrite.

---

## Task 1: Define the playback ownership model

**Objective:** Decide where the player lives and how UI + notification controls talk to it.

**Files:**
- Modify: `app/src/main/java/com/k2s/listennest/ui/screens/player/PlayerViewModel.kt`
- Add: `app/src/main/java/com/k2s/listennest/playback/PlaybackService.kt`
- Add: `app/src/main/java/com/k2s/listennest/playback/PlaybackController.kt`

**Step 1: Split responsibilities**
- `PlaybackService` owns the long-lived player and notification
- `PlaybackController` exposes play, pause, seek, and load-book operations to the UI
- `PlayerViewModel` becomes a UI-facing state layer instead of the player owner

**Step 2: Define the API**
- `loadBook(book)`
- `play()` / `pause()` / `togglePlayback()`
- `seekTo(positionMs)`
- `skipForward()` / `skipBack()`
- `currentTrack()` and `isPlaying()` state exposure

**Step 3: Decide state source of truth**
- Playback state should come from the service/player
- UI state should observe it, not duplicate it

**Verification:**
- The design clearly separates player ownership from screen rendering
- No notification code is added yet

---

## Task 2: Add a MediaSession-backed foreground service

**Objective:** Create the Android service that keeps playback alive and publishes the system notification.

**Files:**
- Add: `app/src/main/java/com/k2s/listennest/playback/PlaybackService.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`

**Step 1: Add required dependencies if missing**
- Ensure Media3 session/notification support is available
- If the project only has exoplayer and UI artifacts today, add the session module as needed

**Step 2: Implement the service**
- Build an `ExoPlayer` inside the service
- Attach a `MediaSession`
- Run the service as a foreground service during active playback
- Use Media3’s notification helper/pattern so Android shows the playback controls in the shade

**Step 3: Register the service**
- Add the service declaration to the manifest
- Add foreground service permissions if required by the chosen implementation path

**Step 4: Wire lifecycle behavior**
- Start foreground playback when audio begins
- Keep notification visible while playing
- Stop foreground mode when playback ends or is explicitly stopped

**Verification:**
- App builds
- Service is registered in the manifest
- The player can be promoted to foreground playback without crashing

---

## Task 3: Wire the UI to the service/controller layer

**Objective:** Make the current player screen drive the service instead of a screen-scoped player instance.

**Files:**
- Modify: `app/src/main/java/com/k2s/listennest/ui/screens/player/PlayerViewModel.kt`
- Modify: `app/src/main/java/com/k2s/listennest/ui/screens/player/PlayerScreen.kt`
- Modify: `app/src/main/java/com/k2s/listennest/ui/navigation/NavGraph.kt`

**Step 1: Replace direct player ownership**
- Remove or minimize `ExoPlayer` creation from `PlayerViewModel`
- Route all transport commands through the controller

**Step 2: Keep current UI behavior**
- Preserve play/pause button behavior
- Preserve seek buttons and progress display
- Preserve the existing resume state logic

**Step 3: Make state updates reactive**
- Continue to show book title, track title, and progress
- Add notification-driven state updates if playback changes outside the app

**Verification:**
- The player screen still works as before
- Leaving the app does not interrupt playback
- Notification and UI stay in sync

---

## Task 4: Build the media-style notification layout

**Objective:** Make the notification visually match a proper audiobook player.

**Files:**
- Add: `app/src/main/java/com/k2s/listennest/playback/NotificationProvider.kt` or equivalent helper
- Modify: `app/src/main/java/com/k2s/listennest/playback/PlaybackService.kt`

**Step 1: Define notification content**
- App name / channel name
- Current book title
- Current chapter or track label
- Cover art / fallback icon
- Progress indicator if practical

**Step 2: Add transport actions**
- Play / pause
- Skip backward 10 or 15 seconds
- Skip forward 30 seconds
- Optional previous/next track if the book is multi-file

**Step 3: Match Android media conventions**
- Use a media style notification
- Show the notification in the shade and lock screen as supported
- Keep it compact but informative

**Verification:**
- Notification shows the right title/artwork
- Pause/resume works from the shade
- Controls are visible without opening the app

---

## Task 5: Handle artwork, metadata, and progress updates

**Objective:** Make the notification feel polished instead of generic.

**Files:**
- Modify: `app/src/main/java/com/k2s/listennest/ui/screens/player/PlayerViewModel.kt`
- Modify: `app/src/main/java/com/k2s/listennest/playback/PlaybackService.kt`
- Modify: `app/src/main/java/com/k2s/listennest/ui/screens/library/LibraryBookItem.kt` if needed for artwork access

**Step 1: Use existing cover art when available**
- Reuse the book cover art data already present in the player state
- Fall back to the app icon or a default drawable when needed

**Step 2: Keep progress fresh**
- Push elapsed time and duration updates often enough for the notification to stay useful
- Avoid excessive persistence writes

**Step 3: Keep labels human-friendly**
- Use the book title and current file/chapter name
- Avoid internal URIs or debug text

**Verification:**
- Notification text matches the current playback item
- Artwork fallback works for books without cover art
- Progress doesn’t lag badly behind playback

---

## Task 6: Validate on-device behavior

**Objective:** Prove the feature works the way an audiobook app should.

**Files:**
- No new files required; verify against the app build

**Step 1: Run a debug build**
- `./gradlew :app:assembleDebug`

**Step 2: Smoke test the workflow**
- Open a book
- Start playback
- Leave the app
- Confirm the notification appears
- Pause and resume from the notification shade

**Step 3: Verify edge cases**
- Background playback survives screen lock
- Notification disappears when playback stops
- Controls remain correct after track changes

**Acceptance criteria:**
- ListenNest shows a playback notification while audio is active
- The notification displays the current book/track
- Pause/resume works from the notification shade
- Background playback still works when the app is not visible
- The app still builds cleanly

---

## Recommended implementation order

1. Playback ownership model
2. MediaSession foreground service
3. UI/controller wiring
4. Notification layout
5. Artwork/progress polish
6. On-device verification

---

## Notes

- This feature should be built on the existing ExoPlayer foundation, not a parallel player stack.
- The service should be the single source of truth for active playback.
- Keep the UI changes minimal unless a notification-driven state sync issue forces a deeper refactor.
