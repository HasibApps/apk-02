# PDF Gallery

Native Android PDF Reader, Snippet Cropper, Google Lens Text Assistant, Folder Manager, and Unlimited Note Organizer built with **Kotlin** and **Jetpack Compose** (Material Design 3).

## Key Features

- **Home Library & Folder Organization**: Automatic scanning of device MediaStore PDFs, imported PDFs, downloaded PDFs, and sample PDF generation, plus custom folders, multi-select folder assignment, favorites, search, and full JSON backup export/restore.
- **Google Drive & URL PDF Downloader**: Import PDF links (including `ACTION_SEND` share intents and auto-clipboard paste), auto-extract Drive titles and covers, pause/resume partial downloads via foreground service, and customize cover thumbnails with the Android Photo Picker.
- **High-Precision PDF Viewer**: Smooth vertical page streaming (`PdfRenderer`), pinch-to-zoom, double-tap zoom, draggable right-edge fast scroll bar with page tooltip, direct page jump dialog, and PDF sharing.
- **Page Crop & Lossless Text Snippet**: Interactive crop overlay with **Invert** and **B&W** modes, JPEG export to `Pictures/PDF_Gallery`, transparent lossless ink extraction, and direct sharing with customizable AI prompts.
- **Google Lens & Screen Snippet Overlay**: Official Google Lens handoff with floating Prev/Next page navigator bar and Accessibility auto-copy helper, plus ML Kit Text Recognition fallback and `MediaProjection` screen snippet capture.
- **Unlimited Notes & 2-Column Chapter Index**: Bengali & English note-taking with Noto Serif Bengali typography, Undo/Redo history, KaTeX scientific/math formula preview, auto-formatting chemical/physics notation, auto-incrementing `(পৃষ্ঠা= N)` tags that link back directly to PDF pages, 1-column / 2-column chapter index mode, and `.txt` export to Downloads.
