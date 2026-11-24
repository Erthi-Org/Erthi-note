# Erthi Note — Development Status

**Branch:** `dev/erthi-stages-5-20`
**Latest local commit:** `00ae30e` + pending note-duplication commit
**Last successful APK build:** pending CI verification

---

## Completed features (source-verified)

### Identity & Branding
- Application ID: `org.erthi.note`; launcher label "Erthi Note" / "إرثي نوتة"
- All FG Machines / Nexvary branding replaced
- Andalusian color palette (Deep Glossy Black, Metallic Silver, Andalusian Gold)
- AboutActivity with Erthi branding

### Rich Text Editor
- Bold, italic, underline, strikethrough
- Font size up/down (relative), heading
- Text color (foreground), background highlight (yellow)
- Align left / center
- Checklist toggle
- Bullet list / numbered list (toggle per line)
- Hyperlink insert (overlay dialog)
- Find & Replace (overlay dialog)
- RTL/LTR toggle per note
- Clear formatting (selection-scoped)
- Undo / redo (per-window, isolated history)
- Inline images (via ACTION_INSERT_IMAGE intent)
- Font family picker (system + downloadable + imported TTF/OTF)

### Font Engine
- FontManager: FontEntry with displayName, familyName, resId, Category
- Downloadable fonts: Cairo, Amiri, Noto Kufi Arabic, Tajawal, Reem Kufi,
  Playfair Display, EB Garamond, Montserrat (Google Fonts provider XML)
- TTF/OTF import and deletion
- CustomTypefaceSpan: proper Typeface-backed rendering for non-system fonts
- Font persistence: erthi-fonts HTML comment preserves TypefaceSpan and
  CustomTypefaceSpan family names across save/reopen

### Floating Notes
- Up to 4 independent windows (windowManager TYPE_APPLICATION_OVERLAY)
- Per-window isolated RichHistory (EditorSession.history)
- Dragging, resizing, opacity slider
- Minimize/maximize, autosave (350ms debounce)
- Quick Settings tile

### Note Organization
- CRUD, pin, reminder, search
- Favorites, archive, recycle bin
- Tags (comma-separated, editable inline)
- **Note duplication** (duplicates title, body, richHtml, color, tags)
- Color picker per note
- **Folders + subfolders** (NoteStore v9: createFolder, renameFolder, deleteFolder, getFolders, getSubfolders, moveNoteToFolder; cascade reparent on delete)
- **Multi-select** (long-press to enter selection mode; bulk delete, archive, favorite via action bar)
- **Bulk operations** (bulkDelete, bulkArchive, bulkFavorite, bulkMoveToFolder)

### Security
- PinActivity: PBKDF2WithHmacSHA256 PIN, throttle, skip option
- SecureNoteArchive: AES-256-GCM, PBKDF2 210k iterations
- Settings screen: PIN toggle with confirm-before-disable
- **Biometric unlock** (API 28+): platform BiometricPrompt, BIOMETRIC_STRONG,
  falls back to PIN on API 26–27 or unenrolled device; no new Gradle dependency

### Backup & Export
- JSON backup (NoteArchive) with legacy .nfn compatibility
- Encrypted backup (SecureNoteArchive)
- NoteExporter: PDF via Canvas, PNG, TXT, HTML
- DOCX export via OpenXML

### Settings
- Dark theme toggle (recreate on change)
- RTL default for new notes
- PIN protection toggle
- About Erthi link

---

## Partially implemented
- Downloadable fonts: declared in XML, loaded via ResourcesCompat on demand;
  offline fallback to system font if provider not yet delivered
- Font previews in picker: shows display name only, no visual preview row
- Light theme: style defined, applied on recreate; no live preview
- Geometric Andalusian ornament drawables: not yet created
- Settings: backup section shows info text only; no interactive backup controls
  in SettingsActivity (export/import still only in MainActivity)

## Remaining (priority order)
1. **Green CI build** — verify compile/lint/tests for commits 6524530 + 3fd1166
2. Font picker with per-row typeface preview
3. DOCX export: Arabic RTL direction
4. PDF: Arabic direction, long-note pagination
5. Reminder delivery on API 35 / Doze test
6. Runtime verification: 4 floating windows, keyboard, rotation, screen-lock
7. Emulator smoke tests (API 26 + API 35)
8. Release signing configuration

## Known fixed defects (this session)
- Duplicate `settings_section_appearance` in strings.xml → build failure FIXED
- Shared `editorHistory`/`currentNoteTitle` service fields shared across windows FIXED
- SuspiciousIndentation lint error in NoteArchive.kt:83 FIXED
- Missing Arabic translations for 20+ strings FIXED

## CI history
- Run 45396801077: FAILED — duplicate string
- Run 45400809384: FAILED — lint SuspiciousIndentation + font translations missing
- Run 45401981486: FAILED — MissingTranslation (settings_section_appearance in AR)
- Run 45403675525: PENDING result (pushed Arabic translations fix)
- Runs for 00ae30e, note-duplication commit: not yet checked (rate-limited)

## Exact next engineering action
1. Wait for GitHub rate limit reset, check CI for 45403675525+.
2. If green: proceed to font picker preview rows.
3. If red: get error, fix in one batch.
