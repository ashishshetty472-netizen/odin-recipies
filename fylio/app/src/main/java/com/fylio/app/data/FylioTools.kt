package com.fylio.app.data

import com.fylio.app.R

/** Implementation state of a tool. */
enum class ToolStatus {
    /** Fully wired up in this build. */
    READY,

    /** Needs a document picked before it runs — otherwise ready. */
    PICK_DOCUMENT,

    /** Visible in the UI, engine lands in the next phase. */
    COMING_SOON
}

data class ToolDef(
    val id: String,
    val title: String,
    /** Icon id matching one of the 88 files in res/drawable-xxhdpi/ic_tool_*.png */
    val icon: String,
    val status: ToolStatus = ToolStatus.READY,
    val description: String = ""
)

data class ToolSection(
    val title: String,
    val tools: List<ToolDef>
)

/**
 * Single source of truth for the tool catalogue.
 *
 * Every [ToolDef.icon] maps to a shipped icon from the Fylio design sheet via
 * [FylioTools.drawableFor]; the sheet's groups (Main Tools, PDF Tools, Document
 * Scanner, Create PDF, File Conversion, Editing & Annotation, File Manager,
 * Utilities & Settings) map one-to-one onto [sections].
 */
object FylioTools {

    val mainTools = listOf(
        ToolDef("scanner", "Scanner", "scanner", ToolStatus.READY, "Capture pages with the camera"),
        ToolDef("create_pdf_plus", "Create PDF", "create_pdf", ToolStatus.READY, "Build a PDF from images"),
        ToolDef("converter", "Converter", "converter", ToolStatus.READY, "Convert between formats"),
        ToolDef("ocr", "OCR", "ocr", ToolStatus.COMING_SOON, "Text recognition")
    )

    val pdfTools = listOf(
        ToolDef("merge_pdf", "Merge PDF", "merge_pdf", ToolStatus.READY, "Join documents into one"),
        ToolDef("split_pdf", "Split PDF", "split_pdf", ToolStatus.PICK_DOCUMENT, "One PDF per page"),
        ToolDef("extract_pages", "Extract Pages", "extract_pages", ToolStatus.PICK_DOCUMENT, "Keep a page range"),
        ToolDef("delete_pages", "Delete Pages", "delete_pages", ToolStatus.COMING_SOON, "Remove pages"),
        ToolDef("reorder_pages", "Reorder Pages", "reorder_pages", ToolStatus.COMING_SOON, "Drag pages around"),
        ToolDef("rotate_pages", "Rotate Pages", "rotate_pages", ToolStatus.PICK_DOCUMENT, "Fix orientation"),
        ToolDef("compress_pdf", "Compress PDF", "compress_pdf", ToolStatus.COMING_SOON, "Shrink file size"),
        ToolDef("protect_pdf", "Protect PDF", "protect_pdf", ToolStatus.COMING_SOON, "Password protect"),
        ToolDef("unlock_pdf", "Unlock PDF", "unlock_pdf", ToolStatus.COMING_SOON, "Remove a password"),
        ToolDef("pdf_to_images", "PDF to Images", "pdf_to_images", ToolStatus.PICK_DOCUMENT, "Export pages as JPG"),
        ToolDef("images_to_pdf", "Images to PDF", "images_to_pdf", ToolStatus.PICK_DOCUMENT, "Wrap images into a PDF"),
        ToolDef("watermark", "Watermark", "watermark", ToolStatus.COMING_SOON, "Stamp text or a logo"),
        ToolDef("crop_pdf", "Crop PDF", "crop_pdf", ToolStatus.COMING_SOON, "Trim margins"),
        ToolDef("sign_pdf", "Sign PDF", "sign_pdf", ToolStatus.COMING_SOON, "Draw and stamp a signature"),
        ToolDef("print_pdf", "Print PDF", "print_pdf", ToolStatus.READY, "Send to a printer"),
        ToolDef("repair_pdf", "Repair PDF", "repair_pdf", ToolStatus.COMING_SOON, "Recover damaged files")
    )

    val scannerTools = listOf(
        ToolDef("camera", "Camera", "camera", ToolStatus.READY, "Capture a page"),
        ToolDef("gallery", "Gallery", "gallery", ToolStatus.COMING_SOON, "Import from gallery"),
        ToolDef("auto_detect", "Auto Detect", "auto_detect", ToolStatus.READY, "Edge guide overlay"),
        ToolDef("flash", "Flash", "flash", ToolStatus.READY, "Torch while capturing"),
        ToolDef("multi_page", "Multi-page", "multi_page", ToolStatus.READY, "Batch capture"),
        ToolDef("crop", "Crop", "crop", ToolStatus.READY, "Trim a page"),
        ToolDef("rotate", "Rotate", "rotate", ToolStatus.READY, "90 degree steps"),
        ToolDef("filters", "Filters", "filters", ToolStatus.COMING_SOON, "Presets"),
        ToolDef("adjust", "Adjust", "adjust", ToolStatus.READY, "Brightness, contrast, sharpness"),
        ToolDef("save_pdf", "Save PDF", "save_pdf", ToolStatus.READY, "Write the scan to PDF")
    )

    val createPdfTools = listOf(
        ToolDef("images_to_pdf_create", "Images to PDF", "images_to_pdf_c", ToolStatus.PICK_DOCUMENT, "From your library"),
        ToolDef("camera_to_pdf", "Camera to PDF", "camera_to_pdf", ToolStatus.READY, "Scan straight to PDF"),
        ToolDef("multiple_images", "Multiple Images", "multiple_images", ToolStatus.PICK_DOCUMENT, "Combine many"),
        ToolDef("text_to_pdf", "Text to PDF", "text_to_pdf", ToolStatus.COMING_SOON, "From a text file"),
        ToolDef("documents_to_pdf", "Documents to PDF", "documents_to_pdf", ToolStatus.COMING_SOON, "Office to PDF"),
        ToolDef("blank_pdf", "Blank PDF", "blank_pdf", ToolStatus.COMING_SOON, "Start from empty pages")
    )

    val conversionTools = listOf(
        ToolDef("jpg_to_png", "JPG ⇄ PNG", "jpg_to_png", ToolStatus.COMING_SOON, "Image format swap"),
        ToolDef("images_to_pdf_conv", "Images to PDF", "images_to_pdf", ToolStatus.PICK_DOCUMENT, "Batch convert"),
        ToolDef("pdf_to_jpg", "PDF to JPG", "pdf_to_jpg", ToolStatus.PICK_DOCUMENT, "Page per image"),
        ToolDef("pdf_to_png", "PDF to PNG", "pdf_to_png", ToolStatus.COMING_SOON, "Lossless pages"),
        ToolDef("docx_to_pdf", "DOCX to PDF", "docx_to_pdf", ToolStatus.COMING_SOON, "Word documents"),
        ToolDef("pptx_to_pdf", "PPTX to PDF", "pptx_to_pdf", ToolStatus.COMING_SOON, "Slides"),
        ToolDef("xlsx_to_pdf", "XLSX to PDF", "xlsx_to_pdf", ToolStatus.COMING_SOON, "Spreadsheets"),
        ToolDef("txt_to_pdf", "TXT to PDF", "txt_to_pdf", ToolStatus.COMING_SOON, "Plain text"),
        ToolDef("webp_to_jpg", "WEBP to JPG/PNG", "webp_to_jpg_png", ToolStatus.COMING_SOON, "Image format swap"),
        ToolDef("more", "More", "more", ToolStatus.COMING_SOON, "Everything else"),
        ToolDef("pdf_to_word", "PDF to Word", "pdf_to_word", ToolStatus.COMING_SOON, "Editable document"),
        ToolDef("pdf_to_excel", "PDF to Excel", "pdf_to_excel", ToolStatus.COMING_SOON, "Editable sheet"),
        ToolDef("pdf_to_pptx", "PDF to PowerPoint", "pdf_to_powerpoint", ToolStatus.COMING_SOON, "Editable slides"),
        ToolDef("csv_to_xlsx", "CSV to XLSX", "csv_to_xlsx", ToolStatus.COMING_SOON, "Spreadsheet upcycle"),
        ToolDef("html_to_pdf", "HTML to PDF", "html_to_pdf", ToolStatus.COMING_SOON, "Web page capture")
    )

    val annotationTools = listOf(
        ToolDef("highlight", "Highlight", "highlight", ToolStatus.COMING_SOON, "Mark passages"),
        ToolDef("underline", "Underline", "underline", ToolStatus.COMING_SOON, "Underline text"),
        ToolDef("strikethrough", "Strikethrough", "strikethrough", ToolStatus.COMING_SOON, "Cross out text"),
        ToolDef("draw", "Draw", "draw", ToolStatus.COMING_SOON, "Freehand ink"),
        ToolDef("text_box", "Text Box", "text_box", ToolStatus.COMING_SOON, "Add typed text"),
        ToolDef("notes", "Notes", "notes", ToolStatus.COMING_SOON, "Margin notes"),
        ToolDef("shapes", "Shapes", "shapes", ToolStatus.COMING_SOON, "Boxes and arrows"),
        ToolDef("signature", "Signature", "signature", ToolStatus.COMING_SOON, "Saved signatures"),
        ToolDef("color", "Color", "color", ToolStatus.COMING_SOON, "Ink colour"),
        ToolDef("eraser", "Eraser", "eraser", ToolStatus.COMING_SOON, "Remove marks"),
        ToolDef("undo", "Undo", "undo", ToolStatus.COMING_SOON, "Step back"),
        ToolDef("redo", "Redo", "redo", ToolStatus.COMING_SOON, "Step forward"),
        ToolDef("form_fill", "Form Fill", "form_fill", ToolStatus.COMING_SOON, "Fill in PDF forms")
    )

    val fileManagerTools = listOf(
        ToolDef("folder", "Folder", "folder", ToolStatus.READY, "Fylio documents"),
        ToolDef("search", "Search", "search", ToolStatus.READY, "Find a document"),
        ToolDef("filter", "Filter", "filter", ToolStatus.READY, "Narrow the list"),
        ToolDef("sort", "Sort", "sort", ToolStatus.READY, "Reorder the list"),
        ToolDef("grid_list", "Grid/List", "grid_list", ToolStatus.READY, "Change the layout"),
        ToolDef("rename", "Rename", "rename", ToolStatus.PICK_DOCUMENT, "Give it a new name"),
        ToolDef("copy", "Copy", "copy", ToolStatus.PICK_DOCUMENT, "Duplicate a document"),
        ToolDef("move", "Move", "move", ToolStatus.COMING_SOON, "Move between folders"),
        ToolDef("delete", "Delete", "delete", ToolStatus.PICK_DOCUMENT, "Remove a document"),
        ToolDef("share", "Share", "share", ToolStatus.PICK_DOCUMENT, "Send it elsewhere"),
        ToolDef("favourite", "Favourite", "favourite", ToolStatus.COMING_SOON, "Pin documents"),
        ToolDef("details", "Details", "details", ToolStatus.PICK_DOCUMENT, "Size and type"),
        ToolDef("recent", "Recent", "recent", ToolStatus.READY, "Newest first"),
        ToolDef("duplicates", "Duplicates", "duplicates", ToolStatus.COMING_SOON, "Find repeated files"),
        ToolDef("large_files", "Large Files", "large_files", ToolStatus.READY, "Biggest first")
    )

    val utilityTools = listOf(
        ToolDef("theme", "Theme", "theme", ToolStatus.READY, "Follows the system"),
        ToolDef("reading_prefs", "Reading Prefs", "reading_prefs", ToolStatus.COMING_SOON, "Reading options"),
        ToolDef("file_prefs", "File Prefs", "file_prefs", ToolStatus.COMING_SOON, "File handling"),
        ToolDef("output_prefs", "Output Prefs", "output_prefs", ToolStatus.COMING_SOON, "Where exports land"),
        ToolDef("privacy", "Privacy", "privacy", ToolStatus.READY, "Everything stays on device"),
        ToolDef("permissions", "Permissions", "permissions", ToolStatus.READY, "Camera and storage"),
        ToolDef("clear_cache", "Clear Cache", "clear_cache", ToolStatus.READY, "Free up space"),
        ToolDef("app_lock", "App Lock", "app_lock", ToolStatus.COMING_SOON, "PIN protection"),
        ToolDef("about", "About", "about", ToolStatus.READY, "Version and build"),
        ToolDef("feedback", "Feedback", "feedback", ToolStatus.COMING_SOON, "Tell us what to fix")
    )

    val sections = listOf(
        ToolSection("Main Tools", mainTools),
        ToolSection("PDF Tools", pdfTools),
        ToolSection("Document Scanner", scannerTools),
        ToolSection("Create PDF", createPdfTools),
        ToolSection("File Conversion", conversionTools),
        ToolSection("Editing & Annotation", annotationTools),
        ToolSection("File Manager", fileManagerTools),
        ToolSection("Utilities & Settings", utilityTools)
    )

    val all: List<ToolDef> = sections.flatMap { it.tools }.distinctBy { it.id }

    fun byId(id: String): ToolDef? = all.firstOrNull { it.id == id }

    fun section(title: String): ToolSection? = sections.firstOrNull { it.title == title }

    // ---- icon resolution -------------------------------------------------

    /** Every shipped tool icon, keyed by its id. Generated from the design sheet. */
    private val drawables: Map<String, Int> = mapOf(
        "scanner" to R.drawable.ic_tool_scanner,
        "create_pdf" to R.drawable.ic_tool_create_pdf,
        "converter" to R.drawable.ic_tool_converter,
        "ocr" to R.drawable.ic_tool_ocr,
        "merge_pdf" to R.drawable.ic_tool_merge_pdf,
        "split_pdf" to R.drawable.ic_tool_split_pdf,
        "extract_pages" to R.drawable.ic_tool_extract_pages,
        "delete_pages" to R.drawable.ic_tool_delete_pages,
        "reorder_pages" to R.drawable.ic_tool_reorder_pages,
        "rotate_pages" to R.drawable.ic_tool_rotate_pages,
        "compress_pdf" to R.drawable.ic_tool_compress_pdf,
        "protect_pdf" to R.drawable.ic_tool_protect_pdf,
        "unlock_pdf" to R.drawable.ic_tool_unlock_pdf,
        "pdf_to_images" to R.drawable.ic_tool_pdf_to_images,
        "images_to_pdf" to R.drawable.ic_tool_images_to_pdf,
        "watermark" to R.drawable.ic_tool_watermark,
        "crop_pdf" to R.drawable.ic_tool_crop_pdf,
        "sign_pdf" to R.drawable.ic_tool_sign_pdf,
        "print_pdf" to R.drawable.ic_tool_print_pdf,
        "repair_pdf" to R.drawable.ic_tool_repair_pdf,
        "camera" to R.drawable.ic_tool_camera,
        "gallery" to R.drawable.ic_tool_gallery,
        "auto_detect" to R.drawable.ic_tool_auto_detect,
        "flash" to R.drawable.ic_tool_flash,
        "multi_page" to R.drawable.ic_tool_multi_page,
        "crop" to R.drawable.ic_tool_crop,
        "rotate" to R.drawable.ic_tool_rotate,
        "filters" to R.drawable.ic_tool_filters,
        "adjust" to R.drawable.ic_tool_adjust,
        "save_pdf" to R.drawable.ic_tool_save_pdf,
        "images_to_pdf_c" to R.drawable.ic_tool_images_to_pdf_c,
        "camera_to_pdf" to R.drawable.ic_tool_camera_to_pdf,
        "multiple_images" to R.drawable.ic_tool_multiple_images,
        "text_to_pdf" to R.drawable.ic_tool_text_to_pdf,
        "documents_to_pdf" to R.drawable.ic_tool_documents_to_pdf,
        "blank_pdf" to R.drawable.ic_tool_blank_pdf,
        "jpg_to_png" to R.drawable.ic_tool_jpg_to_png,
        "pdf_to_jpg" to R.drawable.ic_tool_pdf_to_jpg,
        "pdf_to_png" to R.drawable.ic_tool_pdf_to_png,
        "docx_to_pdf" to R.drawable.ic_tool_docx_to_pdf,
        "pptx_to_pdf" to R.drawable.ic_tool_pptx_to_pdf,
        "xlsx_to_pdf" to R.drawable.ic_tool_xlsx_to_pdf,
        "txt_to_pdf" to R.drawable.ic_tool_txt_to_pdf,
        "webp_to_jpg_png" to R.drawable.ic_tool_webp_to_jpg_png,
        "more" to R.drawable.ic_tool_more,
        "pdf_to_word" to R.drawable.ic_tool_pdf_to_word,
        "pdf_to_excel" to R.drawable.ic_tool_pdf_to_excel,
        "pdf_to_powerpoint" to R.drawable.ic_tool_pdf_to_powerpoint,
        "csv_to_xlsx" to R.drawable.ic_tool_csv_to_xlsx,
        "html_to_pdf" to R.drawable.ic_tool_html_to_pdf,
        "highlight" to R.drawable.ic_tool_highlight,
        "underline" to R.drawable.ic_tool_underline,
        "strikethrough" to R.drawable.ic_tool_strikethrough,
        "draw" to R.drawable.ic_tool_draw,
        "text_box" to R.drawable.ic_tool_text_box,
        "notes" to R.drawable.ic_tool_notes,
        "shapes" to R.drawable.ic_tool_shapes,
        "signature" to R.drawable.ic_tool_signature,
        "color" to R.drawable.ic_tool_color,
        "eraser" to R.drawable.ic_tool_eraser,
        "undo" to R.drawable.ic_tool_undo,
        "redo" to R.drawable.ic_tool_redo,
        "form_fill" to R.drawable.ic_tool_form_fill,
        "folder" to R.drawable.ic_tool_folder,
        "search" to R.drawable.ic_tool_search,
        "filter" to R.drawable.ic_tool_filter,
        "sort" to R.drawable.ic_tool_sort,
        "grid_list" to R.drawable.ic_tool_grid_list,
        "rename" to R.drawable.ic_tool_rename,
        "copy" to R.drawable.ic_tool_copy,
        "move" to R.drawable.ic_tool_move,
        "delete" to R.drawable.ic_tool_delete,
        "share" to R.drawable.ic_tool_share,
        "favourite" to R.drawable.ic_tool_favourite,
        "details" to R.drawable.ic_tool_details,
        "recent" to R.drawable.ic_tool_recent,
        "duplicates" to R.drawable.ic_tool_duplicates,
        "large_files" to R.drawable.ic_tool_large_files,
        "theme" to R.drawable.ic_tool_theme,
        "reading_prefs" to R.drawable.ic_tool_reading_prefs,
        "file_prefs" to R.drawable.ic_tool_file_prefs,
        "output_prefs" to R.drawable.ic_tool_output_prefs,
        "privacy" to R.drawable.ic_tool_privacy,
        "permissions" to R.drawable.ic_tool_permissions,
        "clear_cache" to R.drawable.ic_tool_clear_cache,
        "app_lock" to R.drawable.ic_tool_app_lock,
        "about" to R.drawable.ic_tool_about,
        "feedback" to R.drawable.ic_tool_feedback
    )

    fun drawableFor(iconId: String): Int =
        drawables[iconId] ?: R.drawable.ic_tool_documents_to_pdf

    /** Best icon for a document, reusing the document icons from the sheet. */
    fun iconForFile(item: FylioFile): String = when (item.kind) {
        FileKind.PDF -> "save_pdf"
        FileKind.IMAGE -> "gallery"
        FileKind.WORD -> "pdf_to_word"
        FileKind.EXCEL -> "pdf_to_excel"
        FileKind.PPT -> "pdf_to_powerpoint"
        FileKind.TEXT -> "documents_to_pdf"
        FileKind.OTHER -> "documents_to_pdf"
    }
}
