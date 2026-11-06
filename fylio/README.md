# Fylio — Android document viewer, scanner & PDF toolkit

Phase 1 build: scanner redesign (subtle auto-detect guide, 280-style shutter, floating
page thumbnails, brightness/contrast/sharpness adjustments, delete confirmation),
a 22-icon custom vector set, a real document library, a PDF/image/text viewer, and a
working Convert / Merge / Split / Extract toolset.

> **Provenance:** this tree was reconstructed from `fylio-source.md` plus the design notes
> that accompanied it. The original zip was not provided, so this is a faithful rebuild of
> the described behaviour rather than a byte-for-byte copy. Drop the original `app/src`
> over this one if you want to keep your exact file history — the Gradle setup, manifest,
> FileProvider and CI below work with either.

## Download the APK

The APK is **not** built on your machine. GitHub Actions builds it:

1. Push a change under `fylio/` (or run the *Build Fylio APK* workflow manually).
2. The runner assembles `app-debug.apk` and `app-release.apk` (release is signed with the
   auto-generated debug key so it sideloads).
3. Both files are force-pushed to the **`apk-latest`** branch.

Direct download (once the workflow has run):

```
https://raw.githubusercontent.com/ashishshetty472-netizen/odin-recipies/apk-latest/app-release.apk
```

Install on a device with `adb install -r app-release.apk`, or copy the file over and tap it
(allow "install unknown apps" for your file manager).

## Build locally

```bash
cd fylio
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # minify-off release APK, debug-signed
```

Requires JDK 17, Android SDK 34 and network access to Google Maven.

Outputs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

## What's inside

| Area | Notes |
| --- | --- |
| Library | Search, sort by recent/name/size/type, custom type icons, seeded welcome doc |
| Viewer | PDF via `PdfRenderer` with page nav + pinch zoom, image zoom/rotate, text view, Office preview card with "open with" hand-off |
| Scanner | CameraX capture, auto-detect guide (off by default), 84dp/58dp shutter with spring press, page thumbnails, adjustments, export to PDF |
| Toolkit | Convert (PDF→JPG, JPG→PDF), Merge, Split, Extract pages, plus Phase-2 placeholders |
| Platforms | `OPEN WITH` intents for `application/pdf` and `image/*`, FileProvider sharing |

### Known limits (Phase 1)

- Toolkit page rewrites are rasterised (Android has no built-in PDF writer), so edited
  PDFs are image-backed and not text-selectable. A real engine (PdfBox-Android) is the
  Phase 2 upgrade.
- OCR, Compress, Sign, Lock, Annotate are listed in the UI but not implemented.
- `targetSdk 34` keeps `READ_EXTERNAL_STORAGE` working on older devices; the library reads
  only Fylio's own `files/documents` folder.
