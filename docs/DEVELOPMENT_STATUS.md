# Erthi Note — Development Status

**Branch:** `dev/erthi-stages-5-20`
**Latest verified commit:** `0ca9ec4` (Stage C: RTL/LTR toggle; fix FontEntry field names in font picker)
**Last successful APK build:** None yet (CI blocked by duplicate string)

---

## Completed features (source-verified)
- Application ID: `org.erthi.note`; launcher label "Erthi Note" / "إرثي نوتة"
- All FG Machines / Nexvary branding replaced
- Andalusian color palette (Deep Glossy Black, Metallic Silver, Andalusian Gold)
- FontManager: `FontEntry(displayName, familyName)` — field names corrected
- Font picker dialog with RTL/LTR toggle
- RichNotes: bold, heading, color, checklist, inline images, HTML serialize/deserialize
- RichHistory: per-editor undo/redo stack (now per-window via `EditorSession.history`)
- FloatingNoteService: up to 4 independent windows, dragging, resizing, opacity
- NoteStore: SQLite v1–v8 migration, CRUD, pin, reminder, favorites, archive, trash
- SecureNoteArchive: AES-256-GCM, PBKDF2 210k iterations, legacy .nfn compatibility
- PinActivity: SHA-256 hashed PIN, throttle, skip option
- TrashActivity: recycle bin with restore and permanent delete
- AboutActivity: Erthi branding, website, email, open-source notice
- NoteExporter: PDF via Canvas, PNG, TXT, HTML
- NoteArchive JSON: backup/restore, format identifier preserved for legacy import
- Dark/light theme split in styles.xml

## Partially implemented features
- Downloadable fonts (Cairo, Amiri, Playfair Display declared; not yet applied to views)
- Settings screen: stub button added; full UI not yet implemented
- Light theme toggle: style defined; user-selectable toggle not wired
- Geometric Andalusian ornament drawables: not yet created

## Remaining features (priority order)
1. **CI green build** — push current batch fix commit, verify passing
2. Font engine: apply downloadable fonts to EditText; add Noto Kufi Arabic, Tajawal, Reem Kufi, EB Garamond; offline fallback
3. Font persistence: custom TypefaceSpan not preserved by Html.toHtml — needs supplementary span serialization
4. Settings screen: theme toggle, RTL default, PIN, backup controls
5. Biometric / Android Keystore-backed encryption
6. Folders, categories, tag management, bulk operations
7. DOCX export: rich formatting, Arabic RTL
8. PDF: Arabic direction, long-note pagination
9. Reminder delivery on API 35 / Doze test
10. Runtime verification: 4 floating windows, dragging, keyboard, rotation, screen-lock
11. Emulator smoke tests (API 26 + API 35)
12. Release signing configuration

## Known defects (fixed in this session)
- **FIXED:** Duplicate `settings_section_appearance` in strings.xml → build failure
- **FIXED:** `editorHistory` and `currentNoteTitle` were service-level fields shared across windows; find-replace and font-picker now use `EditorSession.history` and `activeNoteId`

## Latest CI result
- Run `45396801077`: FAILED — `String/settings_section_appearance more than one time`
- Fix committed and pushed in batch after `0ca9ec4`

## Exact next engineering action
1. Confirm CI passes on the batch fix commit.
2. Apply downloadable fonts to FloatingNoteService `EditText` and `SettingsActivity`.
3. Implement full SettingsActivity (theme, RTL, PIN, backup).
4. Add font span persistence (supplementary JSON alongside HTML blob in NoteStore).
