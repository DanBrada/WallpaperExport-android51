# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

WallpaperExport is a small Android app whose sole purpose is to let a user back up (view, share, or save)
the device's current wallpaper — home screen, built-in default, and lock screen. It exists because
accessing the wallpaper on Android 13+ requires the `MANAGE_EXTERNAL_STORAGE` permission, which Google
Play disallows for this use case; this app is distributed outside the Play Store (F-Droid, GitHub releases)
specifically so it can request that permission.

## Build & test commands

Windows (PowerShell), from the repo root:

```powershell
.\gradlew.bat build              # full build (compile + lint + unit tests)
.\gradlew.bat assembleDebug       # debug APK only
.\gradlew.bat assembleRelease     # release APK (see signing below)
.\gradlew.bat lint                # Android lint
.\gradlew.bat licenseReleaseReport  # regenerate open_source_licenses.html in app/src/main/assets
```

There are no unit or instrumented test source sets in this project (`app/src/test`, `app/src/androidTest`
do not exist) — `./gradlew build` runs compile + lint only. If adding tests, create the standard
`app/src/test/java/...` (JVM) or `app/src/androidTest/java/...` (instrumented) source sets first.

### Release signing

`app/build.gradle.kts` looks for a `keystore` Gradle project property (plus `keystorepassword`,
`keystorealias`, `keystorekeypassword`). No keystore is committed to this repo, so without one,
`assembleRelease` produces an unsigned APK. CI (`.github/workflows/gradleCI.yml`) doesn't build or sign
releases at all — it only runs `assembleDebug` and uploads the debug-signed APK as a workflow artifact.

## Architecture

This is a two-Activity, no-DI, no-database app — essentially all logic lives in one file:
`app/src/main/java/com/github/cvzi/wallpaperexport/main.kt`.

- **`MainActivity`** — the entire feature:
  - On `onResume`, checks permissions (`hasPermissions`) and, if granted, loads three wallpaper
    `Drawable`s off the main thread via `WallpaperManager`: index 0 = current system wallpaper
    (`wallpaperManager.drawable`), index 1 = built-in default wallpaper, index 2 = lock-screen
    wallpaper (`FLAG_LOCK`, with SDK-version-dependent fallback logic — try `getWallpaperFile`, then
    `getDrawable(FLAG_LOCK)` on UPSIDE_DOWN_CAKE+, then `getBuiltInDrawable(FLAG_LOCK)`).
  - Each of the three wallpapers has a matching image view, a share button, and a save button, wired up
    generically via `initShareButton`/`initDragDropImage`, which stash the wallpaper index and
    `IntentType` (`SEND`, `VIEW`, `SAVE`) as view tags rather than creating per-index callbacks.
  - Sharing/viewing writes the bitmap to a cache-dir temp file and exposes it via the `FileProvider`
    declared in the manifest (authority in `@string/file_provider_authority`, paths in
    `res/xml/file_provider.xml`); saving uses `CreateDocument("image/png")` (SAF) to let the user pick a
    destination directly, no temp file involved.
  - Drag-and-drop (`DragStartHelper`/`onDragStartListener`) also routes through the same temp-file
    mechanism, primarily for multi-window drag-and-drop use.
  - Permission handling branches on SDK version: pre-R uses `READ_EXTERNAL_STORAGE`; R–S uses
    `MANAGE_EXTERNAL_STORAGE` (`isExternalStorageManager()`); Tiramisu+ additionally requires
    `READ_MEDIA_IMAGES` on top of all-files access. `askForPermission()` builds its dialog message
    incrementally based on which of these are still missing. While backgrounded without the all-files
    permission, `permissionCheckerRunnable` polls every second (capped at 60s) and relaunches the
    activity once the permission is granted, since there's no direct callback for that system dialog.
- **`AboutActivity`** — static info screen (version, license, links), rendered from HTML string
  resources via `Html.fromHtml`. Open-source license list is generated at build time into
  `app/src/main/assets/open_source_licenses.html` by the `com.jaredsburrows.license` Gradle plugin
  (`licenseReleaseReport` task) and shown in a `WebView` inside an `AlertDialog`.

### Conventions worth knowing

- View binding is enabled (`viewBinding = true`); UI code interacts with `ActivityMainBinding` /
  `ActivityAboutBinding`, not `findViewById`.
- Versioning: bump `versionCode`/`versionName` in `app/build.gradle.kts` and add an entry to
  `CHANGELOG.md` together. This fork's CI (`.github/workflows/gradleCI.yml`) only builds and uploads an
  unsigned debug APK as a workflow artifact — there is no fastlane metadata, release signing, or
  GitHub release publishing.
- Translations live in `app/src/main/res/values-<locale>/strings.xml`; supported locales are also listed
  in `res/xml/locales_config.xml`.
- Licensed under GPLv3-or-later; every source file carries the standard GPL header block — match it in
  any new source file.
