# Erthi Note — 20-stage implementation and verification ledger

Status legend: DONE (source changed), PENDING (not implemented or not verified). No phase may be marked complete based solely on documentation.

1. DONE — New isolated branch feature/erthi-note from dev/foundation.
2. DONE — Android application ID org.erthi.note; retain existing Kotlin namespace until coordinated migration.
3. DONE — Launcher app label and active floating-note notification renamed Erthi Note.
4. DONE — Theme name changed to Theme.ErthiNote in manifest and styles.
5. PENDING — Inventory all FG Machines and Nexvary identifiers, assets, URLs, and user-visible copy.
6. PENDING — Design Arabic RTL home screen with accessible Andalusian ornamental details and silver card borders.
7. PENDING — Replace placeholder/drawn icons with licensed real colored icon assets.
8. PENDING — Introduce Arabic Kufi typography and scalable text with accessibility testing.
9. PENDING — Refactor theme into dark/light settings and contrast-safe tokens.
10. PENDING — Audit note CRUD, autosave, persistence, and undo/redo.
11. PENDING — Audit overlay permission and floating bubble lifecycle on Android 13–15.
12. PENDING — Verify 4 floating notes, dragging, resizing, minimizing, and foreground service.
13. PENDING — Audit rich text, checklists, and embedded images.
14. PENDING — Audit reminders, boot restore, and notification permission.
15. PENDING — Audit search, pinned notes, sort, and empty states.
16. PENDING — Audit PDF, PNG, TXT, HTML exports and sharing.
17. PENDING — Verify encrypted backup integrity, limits, and import of old .nfn files.
18. PENDING — Add About Erthi and settings, without corporate branding.
19. PENDING — Run unit tests, build, lint, emulator and physical Android 15 QA; record evidence.
20. PENDING — Review privacy, release signing, APK distribution and acceptance; do not merge without approval.

Migration: the new application ID is distinct from com.nexvary.floatingnote and com.fgmachines.fgnote. Never delete old installs before backup restore is verified. Current package namespace com.fgmachines.fgnote is a source-compatibility measure, not user-facing branding. Avoid renaming Kotlin packages without updating every declaration and reference.

Acceptance: attach logs, screenshots, test matrix, and a signed installable artifact only after successful verification.
