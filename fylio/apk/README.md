# Fylio — installable APKs

Built by CI (`.github/workflows/fylio-apk.yml`) on every push that touches `fylio/`.

| File | Size | What it is |
| --- | --- | --- |
| `app-release.apk` | ~60 MB | Release build (debug-signed so it installs without a keystore) |
| `app-debug.apk` | ~67 MB | Debug build, includes Compose tooling |

Verify: `sha256sum -c app-release.apk.sha256`

Install: copy to the phone and tap it, or `adb install -r app-release.apk`.
Allow "install unknown apps" for your file manager or browser.

## What is in this build (v2.0.0)

- **Home / Files / Tools / Settings** — light "electric & white" theme, blue active pill.
- **Files** — import via SAF (permissions persisted), search, filter by type, sort by
  recent/name/size/type, grid ⇄ list, multi-select + select-all, delete, share.
- **PDF reader** — continuous scroll, pinch zoom, double-tap zoom, thumbnail strip,
  page jump, real in-document text search (PDFBox text extraction), print, delete,
  reading position saved to Room.
- **PPTX** — slides parsed offline from the OOXML, rendered at the deck's real aspect
  ratio, thumbnail strip, fullscreen tap.
- **DOCX** — reflowed blocks with heading detection, font scaling, dark reading mode.
- **XLSX** — spreadsheet grid, column letters + row numbers, sheet tabs, synced scroll.
- **TXT / MD / logs** — monospace reader, font size, wrap toggle, selectable.
- **Images** — pinch/double-tap zoom viewer.
- **Scanner** — CameraX, flash toggle, auto-edge guide (default OFF), shutter with
  thumbnail strip, per-page editor (crop, rotate, brightness, contrast, B&W, grayscale,
  original, magic), delete page, export "Save PDF · N Pages" with file name, A4 vs fit
  image, and JPEG quality.
- **PDF tools** — merge, split, extract range, delete range, reverse order, rotate
  90/180/270, compress, protect, unlock, watermark, page numbers, print, repair, crop.
- **Converters** — PDF→JPG/PNG, images→PDF, DOCX/PPTX/XLSX→PDF, CSV→XLSX (real
  SpreadsheetML writer), TXT/HTML→PDF, JPG⇄PNG/WEBP.
- **OCR** — ML Kit Latin model on device; copy text, export TXT or PDF.
- **Annotate** — freehand ink, highlight, underline, strikethrough, text box, signature;
  saves a flattened annotated copy.

Everything runs offline. Room stores the document index, reading position and page
counts; files whose SAF permission was revoked are flagged instead of crashing.
