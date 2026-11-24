# Erthi Note — Development Status

**Branch:** `dev/erthi-stages-5-20`
**Latest commit:** `98ce56a`
**APK:** `app/build/outputs/apk/debug/app-debug.apk` — 1.8 MB (debug, unsigned)
**Local verification:** compile ✓ · lint ✓ (0 errors, 118 warnings) · unit tests ✓

---

## Completed features (source-verified, 2026-11-24)

### Identity & Branding
- Application ID: `org.erthi.note`; launcher label "Erthi Note" / "إرثي نوتة"
- Andalusian palette: Deep Glossy Black (#0A0A0A), Metallic Silver, Andalusian Gold
- AboutActivity with Erthi branding, privacy statement, licence notice
- All FG Machines / Nexvary branding removed

### Rich Text Editor
- Bold, italic, underline, strikethrough
- Font size up/down (relative), heading
- Text colour (foreground), background highlight
- Align left / centre
- Checklist toggle
- Bullet list / numbered list (toggle per line)
- Hyperlink insert (overlay dialog)
- Find & Replace (overlay dialog)
- RTL/LTR toggle per note
- Clear formatting (selection-scoped)
- Undo / redo (per-window, isolated RichHistory)
- Inline images (via ACTION_INSERT_IMAGE intent)
- Font family picker (system + asset + downloadable + imported TTF/OTF)

### Font Engine
- FontManager: FontEntry with displayName, familyName, assetPath, resId, Category
- Offline-first: asset-bundled fonts via `Typeface.createFromAsset()`
- System Arabic fonts: Noto Naskh Arabic, Noto Kufi Arabic (AOSP, no internet)
- Downloadable: Cairo, Amiri, Noto Kufi, Tajawal, Reem Kufi, Playfair Display,
  EB Garamond, Montserrat (Google Fonts provider — requires GMS, falls back gracefully)
- TTF/OTF import and deletion (magic-byte validation, 10 MB limit)
- CustomTypefaceSpan: Typeface-backed rendering for non-system fonts
- Font persistence: `<!-- erthi-fonts:[…] -->` HTML comment preserves family names
  across save/reopen; deserialization resolves via FontManager → CustomTypefaceSpan
- Font picker: per-row typeface preview; recently-used (top 5); category headers

### Floating Notes
- Up to 4 independent windows (TYPE_APPLICATION_OVERLAY)
- Per-window isolated RichHistory and editor session (no cross-window state)
- Dragging, resizing, opacity slider (per-note, persisted)
- Minimize/maximize, autosave (350 ms debounce)
- Quick Settings tile (API 33+ native add; manual instruction for older)

### Note Organization
- CRUD, pin, reminder, search
- Favorites, archive, recycle bin with undo-delete
- Tags (comma-separated, inline edit)
- Note duplication (title, body, richHtml, color, tags)
- Color picker per note (5 Andalusian colours)
- **Folders + subfolders** (NoteStore v9): createFolder, renameFolder, deleteFolder,
  getFolders, notesInFolder, moveNoteToFolder; cascade reparent on folder delete
- **Multi-select** (long-press to enter): bulk delete / archive / favorite action bar
- **Bulk operations**: bulkDelete, bulkArchive, bulkFavorite, bulkMoveToFolder

### Security
- PinActivity: PBKDF2WithHmacSHA256 PIN, 5-attempt throttle, skip option
- SecureNoteArchive: AES-256-GCM + PBKDF2 210k iterations
- Settings screen: PIN toggle with confirm-before-disable
- **Biometric unlock** (API 28+): platform BiometricPrompt (BIOMETRIC_STRONG on API 30+;
  FingerprintManager fallback on API 28–29); no extra Gradle dependency
- Manifest: USE_BIOMETRIC + USE_FINGERPRINT permissions added

### Backup & Export
- JSON backup (NoteArchive) + legacy .nfn compatibility
- Encrypted backup (SecureNoteArchive), password-protected
- **DOCX**: bold, italic, underline, strikethrough, colour, font size, font family,
  Arabic title bidi + run-level `<w:rtl/>`, CustomTypefaceSpan → `<w:rFonts>`
- **PDF**: multi-page, StaticLayout (BiDi-correct Arabic title), Andalusian header
- PNG export, TXT export, HTML export
- Share via system share sheet (email, messaging apps)

### Settings
- Dark / light theme toggle (recreate on change)
- RTL default for new notes
- PIN protection toggle
- About Erthi link

---

## CI history (branch dev/erthi-stages-5-20)
- `5a89819`: last confirmed green CI run (runs 45419373637 / 45419351073)
- `6524530`: folders + bulk ops + font tests
- `3fd1166`: biometric unlock
- `84a03a6`: fix Typeface.createFromAsset + suppress fingerprint deprecations
- `0ba4cc4`: font picker with typeface previews + recently-used section
- `98ce56a`: USE_BIOMETRIC permission + API 30 guard + DOCX/PDF Arabic RTL fix
  → LOCAL: compile ✓, lint 0 errors, unit tests ✓, APK 1.8 MB

---

## Remaining for v1.0 release
- Emulator smoke tests on API 26 + API 35 (requires running emulator)
- Release signing configuration (keystore + release APK)
- PR #1 review and merge — **owner approval required before merge**

## Explicitly deferred to future releases
- Font picker visual previews in MainActivity (floating-window picker has them)
- Per-note biometric protection (current: app-level PIN/biometric gate)
- Cloud backup (no Erthi-hosted storage of private notes)
- DOCX images (inline images are PDF/PNG only for now)
- Reminder delivery Doze mode testing on API 35

## Security constraints (permanent)
- Do NOT merge PR #1 without explicit owner approval
- Do NOT claim PIN alone encrypts the database
- Do NOT store private notes on Erthi-hosted or external servers
- Do NOT print secrets or authentication tokens
