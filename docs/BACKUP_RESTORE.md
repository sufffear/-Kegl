# Backup format and restore guarantees

Format 2 adds `chantal_progress.json` and its SHA-256 checksum. The companion snapshot includes the prologue, journey timer, pending story, inventory, rewards, traits and journal. New archives also contain custom workout programs, sound ducking and frozen dates. Format 1 remains readable: missing companion and custom-program data leave those existing stores unchanged. Restoring an empty format-2 collection clears the corresponding collection.

Before writes, restore checks the manifest/file list, required checksums, duplicate entries, supported format, JSON decoding and companion schema. These checks detect accidental corruption; checksums are not authentication. The existing extraction size limit remains in effect.

Room stays in a transaction while settings, companion and book writes finish. A write exception rolls back Room; a non-cancellable cleanup restores the original settings, companion and book. Failures during cleanup are retained and reported, rather than promising that data is unchanged. Export and restore operations share a mutex to prevent overlapping archive operations from interfering with staging files.

This is compensation across different storage systems, not a single durable transaction across all of Android. Force-stop, process death or power loss between external writes can still leave stores out of sync. Durable recovery journaling and physical-device checks remain future work. Regular gameplay writes are not locked by the archive-operation mutex; use the backup screen with no workout running.

## Automated validation

`BackupRestoreTest` uses a real in-memory Room database and a temporary Preferences DataStore under Robolectric. It covers:

- Round-trip of training plans, custom programs, settings and companion progress.
- Injected failure after settings have been written: Room and settings return to the previous values.
- Book-directory failure after companion writes: Room and companion progress return to the previous values.
- Legacy archive compatibility.
- Corrupted companion data and omitted checksums.
- Refusal to restore during an active workout.

CI runs these seven tests together with the five existing Chantal persistence tests, builds the APK and verifies its permanent test-signing certificate. This does not replace device tests for archive selection, importing a book, closing the app or reinstalling an APK.
