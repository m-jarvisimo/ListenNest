# ListenNest — Project Structure + Build Plan

**Goal:** Build a local-only Android audiobook player for personal library folders, with resume support, bookmarks, embedded chapters, and a minimal dark UI.

**Scope:**
- Android only
- Local storage only
- No accounts
- No backend
- Free app
- Beta prototype quality

---

## Product rules

| Area | Decision |
|---|---|
| App name | ListenNest |
| Library unit | 1 folder = 1 book |
| Scan depth | No recursion |
| Formats | MP3, M4B, FLAC |
| Chapters | Embedded chapters when present |
| Metadata | Embedded tags first, filename fallback |
| Artwork | Embedded artwork first, filename fallback |
| Refresh | Manual only |
| Resume | Required |
| Playback | One book at a time |
| Bookmarks | Timestamp + optional label |
| Theme | Dark, minimal |
| Data storage | Local only |

---

## Proposed tech stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM + Repository
- **Playback:** Media3 / ExoPlayer
- **Local DB:** Room
- **Folder access:** Storage Access Framework (SAF)
- **Background work:** None for v0, unless later needed for scan indexing

---

## Proposed project structure

```text
listennest/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/k2s/listennest/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ListenNestApp.kt
│       │   │   ├── di/
│       │   │   │   └── AppModule.kt
│       │   │   ├── data/
│       │   │   │   ├── db/
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── BookDao.kt
│       │   │   │   │   ├── BookmarkDao.kt
│       │   │   │   │   └── ChapterDao.kt
│       │   │   │   ├── model/
│       │   │   │   │   ├── BookEntity.kt
│       │   │   │   │   ├── BookFileEntity.kt
│       │   │   │   │   ├── ChapterEntity.kt
│       │   │   │   │   └── BookmarkEntity.kt
│       │   │   │   └── repository/
│       │   │   │       ├── LibraryRepository.kt
│       │   │   │       └── PlaybackRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── scanner/
│       │   │   │   │   ├── LibraryScanner.kt
│       │   │   │   │   ├── MetadataExtractor.kt
│       │   │   │   │   ├── ChapterExtractor.kt
│       │   │   │   │   └── FileOrdering.kt
│       │   │   │   ├── playback/
│       │   │   │   │   ├── PlaybackManager.kt
│       │   │   │   │   └── ProgressTracker.kt
│       │   │   │   └── settings/
│       │   │   │       └── LibrarySettingsStore.kt
│       │   │   ├── ui/
│       │   │   │   ├── navigation/
│       │   │   │   │   └── NavGraph.kt
│       │   │   │   ├── screens/
│       │   │   │   │   ├── library/
│       │   │   │   │   │   ├── LibraryScreen.kt
│       │   │   │   │   │   └── LibraryViewModel.kt
│       │   │   │   │   ├── player/
│       │   │   │   │   │   ├── PlayerScreen.kt
│       │   │   │   │   │   └── PlayerViewModel.kt
│       │   │   │   │   └── settings/
│       │   │   │   │       ├── SettingsScreen.kt
│       │   │   │   │       └── SettingsViewModel.kt
│       │   │   │   └── components/
│       │   │   │       ├── BookCard.kt
│       │   │   │       ├── ChapterList.kt
│       │   │   │       └── BookmarkSheet.kt
│       │   │   └── util/
│       │   │       ├── UriUtils.kt
│       │   │       ├── Formatters.kt
│       │   │       └── DateUtils.kt
│       │   └── res/
│       │       ├── values/
│       │       └── drawable/
│       ├── test/
│       └── androidTest/
├── docs/
│   └── plans/
│       └── listennest-mvp.md
└── README.md
```

---

## File responsibilities

### App entry
- `MainActivity.kt` — Compose entry point, permission/launcher hooks
- `ListenNestApp.kt` — app theme + root navigation

### Data layer
- `AppDatabase.kt` — Room database holder
- `*Dao.kt` — queries for books, bookmarks, chapters, and playback state
- `*Entity.kt` — persistent local models
- `LibraryRepository.kt` — scan results, library reads/writes
- `PlaybackRepository.kt` — progress persistence and playback state access

### Domain layer
- `LibraryScanner.kt` — scan selected folder, one level deep only
- `MetadataExtractor.kt` — embedded tags + filename fallback
- `ChapterExtractor.kt` — embedded chapter parsing
- `FileOrdering.kt` — deterministic ordering for files in each book folder
- `PlaybackManager.kt` — Media3 playback control
- `ProgressTracker.kt` — persist resume positions safely
- `LibrarySettingsStore.kt` — folder URI and app preferences

### UI layer
- `LibraryScreen.kt` — list books and refresh button
- `PlayerScreen.kt` — playback UI and chapter list
- `SettingsScreen.kt` — select library folder, refresh, basic preferences
- `BookCard.kt` — reusable library list item
- `ChapterList.kt` — simple embedded chapter list
- `BookmarkSheet.kt` — add/view bookmarks with optional labels

---

## Build plan

### Phase 1 — Project scaffold
**Objective:** Create the Android app shell, navigation, and theme.

Tasks:
1. Create the Android Studio project using Kotlin + Compose.
2. Add dark theme scaffolding.
3. Set up navigation with placeholder Library, Player, and Settings screens.
4. Confirm app launches cleanly on an emulator/device.

### Phase 2 — Storage and settings
**Objective:** Let the user choose a library folder and persist that selection.

Tasks:
1. Add SAF folder picker.
2. Persist the selected library URI.
3. Add a Settings screen showing the current folder.
4. Add a manual refresh action.

### Phase 3 — Library scanning
**Objective:** Scan top-level folders and turn them into books.

Tasks:
1. Implement top-level-only folder scan.
2. Filter supported audio formats: MP3, M4B, FLAC.
3. Extract embedded metadata.
4. Apply filename fallback for missing tags.
5. Populate the library list from scanned results.

### Phase 4 — Playback
**Objective:** Play one book at a time and save resume state.

Tasks:
1. Integrate Media3 playback.
2. Load all files belonging to a selected book.
3. Add play/pause, seek, and speed controls.
4. Persist current position periodically and on pause/exit.
5. Restore saved position on book open.

### Phase 5 — Chapters and bookmarks
**Objective:** Show embedded chapters and allow user bookmarks.

Tasks:
1. Extract embedded chapters when available.
2. Render a simple chapter list in the player.
3. Add bookmark creation at current position.
4. Support optional bookmark labels.
5. Show bookmarks for the current book.

### Phase 6 — Beta cleanup
**Objective:** Tighten the MVP for real-world use.

Tasks:
1. Improve error handling for bad files and empty folders.
2. Verify ordering behavior for multi-file books.
3. Ensure resume works after app restart.
4. Validate offline-only behavior.
5. Fix any UI rough edges while keeping the design minimal.

---

## Recommended implementation order

1. Project scaffold
2. Settings + folder picker
3. Library scan
4. Playback engine
5. Resume state
6. Chapters
7. Bookmarks
8. Polish

---

## Acceptance criteria for v0

- User can select a local folder.
- App scans immediate subfolders only.
- Each top-level folder becomes one book.
- Supported audio files are discovered and listed.
- A book opens and plays successfully.
- Playback resumes from the last saved position.
- Embedded chapters appear when available.
- User can create timestamp bookmarks with optional labels.
- App stays local-only and works offline.

---

## Open technical decisions to settle before coding

- Do we want one shared book-art cache file or just decode embedded artwork on the fly?
- Do we want the app to treat one file inside a folder as the full book automatically? (likely yes)
- Do we want natural-sort ordering for filenames, or strict lexical sort for v0?
- Do we want the player to auto-advance through multiple files in a book folder? (recommended yes)

---

## Next step

Start Phase 1 with the Android project scaffold, then wire in folder selection and a minimal library screen.
