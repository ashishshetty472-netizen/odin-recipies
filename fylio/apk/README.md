# Fylio — installable APKs

Built by CI (`.github/workflows/fylio-apk.yml`) on every push that touches `fylio/`.

| File | Size | What it is |
| --- | --- | --- |
| `app-release.apk` | ~60 MB | Release build (debug-signed so it installs without a keystore) |
| `app-debug.apk` | ~67 MB | Debug build, includes Compose tooling |

Verify: `sha256sum -c app-release.apk.sha256`

Install: copy to the phone and tap it, or `adb install -r app-release.apk`.
Allow "install unknown apps" for your file manager or browser.

## Phase 1 — scanner & photo-to-PDF rebuild

- **Session survives navigation.** One `ScanSessionViewModel` holds every captured page
  in memory (full-res JPEG on disk, edits kept as parameters). Back never loses pages:
  camera → Home, crop → camera, enhance → crop, and "Add page" returns to the session.
  `BackHandler` is installed on every scanner screen, so back never exits the app.
- **Clean viewfinder by default.** Auto-detection is OFF: no boxes, no margins. Switch it
  ON and frames are analysed on a background thread; the detected page is drawn as a
  `#2563EB` polygon at 20% fill with corner dots. Frame coordinates are translated through
  the real FILL_CENTER crop matrix, so the polygon lands on the paper, not next to it.
- **4-corner perspective crop.** Four draggable `#2563EB` handles with third-lines between
  them, a live area readout, plus Reset / Rotate 90° / Auto-fit. "Apply Crop" solves a
  3×3 homography and resamples the quad into a straightened rectangle (bilinear, no OpenCV
  dependency). No linear crop sliders anywhere.
- **Enhance.** Original / Magic Color / Grayscale / B&W filters, each rendered off the main
  thread with the previous frame kept on screen; brightness (-1…1) and contrast (0.5…2)
  in a floating sheet. Page deck supports tap-to-select, delete, add and long-press drag
  reordering.
- **Save PDF.** FAB reads "Save PDF · N Pages". Sheet offers the auto name
  `Fylio_Scan_yyyyMMdd_HHmmss.pdf`, Low/Medium/High quality, A4 / Letter / Fit-image, a SAF
  folder picker, and Save or Share. Compiled with `android.graphics.pdf.PdfDocument` on
  `Dispatchers.IO` with a per-page progress overlay.

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
