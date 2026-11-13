# Erthi Note — إرثي نوت

Offline-first Android notes application for the Erthi initiative.

Development branch: `feature/erthi-note`. This branch is under active development and is not a verified release.

- Application ID: `org.erthi.note`
- Version: `0.5.0-dev`
- Android 8+; Arabic RTL and English support are development priorities.
- Planned UI: Andalusian accents, silver borders, accessible typography and real colored icons.
- Preserve local note editing, floating notes, checklists, reminders, search, exports and encrypted backups after verifying implementation.

## Migration warning
The new application ID installs separately from `com.nexvary.floatingnote` and `com.fgmachines.fgnote`. Android does not migrate private app data automatically. Export an encrypted backup in the old app and verify restore in Erthi Note before uninstalling it. Backup format compatibility remains to be tested.

## Release gates
Verify manifest and Kotlin package references, compile, unit tests, emulator QA, Android 15 overlay behavior, backup restore, and signed APK before release.
