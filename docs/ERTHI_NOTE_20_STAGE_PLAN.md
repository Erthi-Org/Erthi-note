# Erthi Note — 20-stage implementation and verification ledger

Status legend: DONE (source changed and verifiable), PARTIAL (code written, not yet runtime-verified), PENDING (not implemented or not verified). No phase may be marked DONE based solely on documentation.

---

1. DONE — New isolated branch `feature/erthi-note` from `dev/foundation`.
   Evidence: branch HEAD 1785664b75abc7e4a1ef6446456a881b4920f2b0 matches expected commit.

2. DONE — Android application ID `org.erthi.note`; Kotlin namespace retained as `com.fgmachines.fgnote` pending coordinated rename.
   Evidence: `app/build.gradle.kts` applicationId = "org.erthi.note".

3. DONE — Launcher label and floating-note notification renamed to Erthi Note.
   Evidence: `values/strings.xml` app_name = "Erthi Note"; notification_title = "Erthi Note is active".

4. DONE — Theme name changed to `Theme.ErthiNote` in manifest and styles.
   Evidence: `styles.xml` and `AndroidManifest.xml` both reference Theme.ErthiNote.

5. DONE — Inventoried and migrated all user-visible FG Machines and Nexvary identifiers.
   Changes applied (2026-11-24):
   • Quick Settings tile strings (English + Arabic): "FG Note" → "Erthi Note" / "إرثي نوتة".
   • Arabic app_name: "FG Note" → "إرثي نوتة".
   • FloatingNoteService Intent action constants: `com.fgmachines.fgnote.*` → `org.erthi.note.*`.
   • ReminderScheduler ACTION_REMIND: `com.fgmachines.fgnote.REMIND` → `org.erthi.note.REMIND`.
   • MainActivity backup filename: "nexvary-notes-backup.json" → "erthi-notes-backup.json".
   • MainActivity EXTRA_TILE_TOGGLE: "fg_note_quick_tile_toggle" → "erthi_note_quick_tile_toggle".
   • SecureNoteArchive error message: "FG Note encrypted backup" → "Erthi Note encrypted backup".
   • Log tags updated: "FGNote" → "ErthiNote"; "FGNoteTile" → "ErthiTile".
   • NoteArchive JSON format identifier "nexvary-floating-note" preserved for backward compatibility.
   • MainActivity header "FG MACHINES / NOTES" label removed.
   Pending: Kotlin package rename (coordinated, breaking change—needs test suite pass and manifest update in same commit).

6. PARTIAL — Andalusian design language applied to color palette and styles.
   Changes applied (2026-11-24):
   • `colors.xml` replaced with deep Glossy Black (0x0A0A0A), Metallic Silver, Andalusian Gold (0xC8A86E) palette.
   • `styles.xml` updated to dark Material theme with gold accent, false windowLightStatusBar.
   • `Palette` object in `Ui.kt` updated: BACK=0x0A0A0A, PANEL=0x1C1C1C, MINT=0xC8A86E (gold), WHITE=0xF0EDE8.
   • Silver border constant added to Palette; card outlines updated.
   Pending: Geometric Andalusian ornament drawables, real launcher icon, card border overlay drawables, RTL-aware home-screen layout audit. Requires visual QA on physical Android 15 device.

7. PARTIAL — Font infrastructure created using Android Downloadable Fonts API.
   Changes applied (2026-11-24):
   • `res/font/cairo.xml`, `res/font/amiri.xml`, `res/font/playfair_display.xml` added with Google Fonts provider declarations.
   • `res/values/font_certs.xml` added with GMS font provider certificates.
   Pending: Apply font families to EditText/TextView in FloatingNoteService and MainActivity. Test Arabic shaping on device. Verify offline fallback. Add Noto Kufi Arabic, Tajawal, Reem Kufi, EB Garamond. Font manager picker UI not yet implemented.

8. PARTIAL — Arabic Kufi heading style token defined.
   Changes applied (2026-11-24):
   • `ErthiTitleArabic`, `ErthiSectionHeader`, `ErthiCard`, `ErthiButton`, `ErthiFloatingWindow` styles added to `styles.xml`.
   Pending: Apply styles to Views programmatically or migrate layout XMLs. TalkBack and large-font accessibility test pending. Adjustable text-size user preference not implemented.

9. PARTIAL — Dark/light theme split created.
   Changes applied (2026-11-24):
   • `Theme.ErthiNote.Light` style added for warm ivory light mode.
   • `Palette` object uses dark values by default.
   Pending: User-selectable theme toggle in Settings screen. SharedPreferences persistence. Apply Theme.ErthiNote.Light programmatically on selection.

10. PARTIAL — Note CRUD, autosave and undo/redo audited.
    Source review (2026-11-24):
    • NoteStore.kt: SQLite v1→v2→v3 migration chain intact; create/read/update/delete, search, pin, reminder CRUD present.
    • FloatingNoteService.kt: 350 ms debounced autosave per open note window; saves on minimize and service stop.
    • RichNotes.kt: Android undo/redo stack present for text changes.
    Pending: Runtime verification of autosave persistence after process kill; undo/redo across window reopen; physical device test.

11. PENDING — Overlay permission and floating bubble lifecycle audit on Android 13–15.
    Source review: FloatingNoteService uses ACTION_START / ACTION_STOP with foregroundServiceType=specialUse. Settings.canDrawOverlays gate present.
    Pending: Runtime test on Android 15 emulator and OPPO CPH2577 (API 35). Permission revocation mid-session test. Rotation and keyboard-open handling.

12. PENDING — 4 floating notes, dragging, resizing, minimizing, foreground service verified.
    Source review: WindowManager-based multi-window up to 4 windows; resize handles present; minimize/maximize logic in FloatingNoteService.
    Pending: Runtime test with 4 simultaneous windows. Dragging, resizing, minimize/maximize. Keyboard compatibility. Screen-lock behaviour.

13. PENDING — Rich text, checklists, embedded images audited at runtime.
    Source review: RichNotes.kt handles bold, heading, text colour, checklist toggle, inline image spans via SpannableStringBuilder and Html serializer.
    Pending: Runtime test of formatting round-trip (close/reopen, backup restore, export). Verify checklist toggle, image scale, HTML preservation.

14. PENDING — Reminders, boot restore, notification permission verified at runtime.
    Source review: ReminderScheduler.kt uses AlarmManager.setInexactRepeating with RECEIVE_BOOT_COMPLETED. POST_NOTIFICATIONS permission declared (API 33+).
    Pending: Reminder delivery on OPPO CPH2577 with Doze. Boot restore after reboot. Notification channel visibility test.

15. PENDING — Search, pinned notes, sort, empty states verified at runtime.
    Source review: NoteStore.search() does case-insensitive LIKE on title+body. Pinned notes sort to top. Empty-state label present.
    Pending: Runtime test with Arabic text search. Large-library sort performance. Empty state visual QA.

16. PENDING — PDF, PNG, TXT, HTML exports and sharing verified.
    Source review: NoteExporter.kt generates PDF via Canvas, PNG via Bitmap, TXT via plain text strip, HTML via Android Html.toHtml().
    Pending: Long-note PDF clipping test. Arabic RTL direction in PDF. Email sharing via Android Sharesheet. Export QA on API 35.

17. PARTIAL — Encrypted backup architecture reviewed; legacy .nfn compatibility preserved.
    Source review: SecureNoteArchive.kt uses AES-256-GCM, PBKDF2-HMAC-SHA256 at 210,000 iterations, random salt+nonce, 5 MiB limit, max 2,000 notes.
    Changes applied (2026-11-24):
    • Error message updated to "Erthi Note encrypted backup".
    • NoteArchive format identifier "nexvary-floating-note" preserved for legacy .json import.
    Pending: Cross-device restore test. Wrong-password rejection test. Corrupted/truncated archive test. Large library (>500 notes) test. Streaming/chunked architecture for large embedded images not yet implemented.

18. PARTIAL — About Erthi screen implemented.
    Changes applied (2026-11-24):
    • `AboutActivity.kt` created: app name (English + Arabic), version, website (erthi.org), email (info@erthi.org), privacy statement, open-source notice.
    • No Nexvary or FG Machines branding.
    • Registered in AndroidManifest.xml.
    • "About Erthi" button added to MainActivity.
    Pending: Settings screen (theme toggle, language, floating bubble config, security, backup). Full Settings UI not yet implemented—only stub button added. Dedicated settings screen is a follow-up task.

19. PARTIAL — GitHub Actions workflows updated for Erthi Note.
    Changes applied (2026-11-24):
    • `android.yml`: triggers on main and dev/** branches + PRs; artifact renamed to Erthi-Note-debug.
    • `emulator-smoke.yml`: triggers on main and dev/** + workflow_dispatch; artifact renamed to Erthi-Note-emulator.
    • `signed-release.yml`: keystore path renamed to erthi-note-production.jks; error message updated.
    Pending: Workflow files cannot be pushed without GitHub OAuth `workflow` scope on the Erthuna account. Owner must run:
      gh auth refresh --scopes workflow
    then push the .github/workflows/ directory to Erthi-Org/Erthi-note. CI evidence (green build badge) not yet produced for the Erthi repository.

20. PENDING — Final delivery: reviewed, signed APK, CI evidence, screenshots, screen recording.
    Pending all items in stages 11–16 and 19 CI verification. Signing configuration not yet set up in Erthi-Org/Erthi-note repository secrets. Physical-device screenshots and screen recording not yet captured.

---

## Migration summary (2026-11-24)

Source: nexvary/Floating-Note @ 1785664b75abc7e4a1ef6446456a881b4920f2b0 (feature/erthi-note)
Destination: Erthi-Org/Erthi-note

Stages completed or partially implemented in this session: 5, 6, 7, 8, 9, 18, 19 (partial).
Stages requiring runtime verification or physical device: 11–16, 19 (CI), 20.

## Remaining critical actions for the owner

1. Add `workflow` scope to Erthuna GitHub token and push the updated .github/workflows/ directory:
   ```
   gh auth refresh --scopes workflow
   cd <floating-note-clone>
   git push erthi dev/erthi-stages-5-20
   ```
2. Merge PR (after CI passes) — do not merge without owner review.
3. Configure Android signing secrets in Erthi-Org/Erthi-note → Settings → Secrets.
4. Run emulator smoke tests and capture evidence.
5. Test on OPPO CPH2577 (Android 15, API 35) and capture screenshots.
