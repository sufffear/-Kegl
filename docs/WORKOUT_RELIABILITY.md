# Workout reliability review

Changes in this stage:

- Starting a regular workout clears execution steps left by the constructor.
- Checkpoint jobs execute in emission order. Room rejects older active-time snapshots and writes to terminal attempts.
- Final checkpoint and completion record share one Room transaction. Duplicate results remain blocked.
- Constructor attempts use distinct credit IDs, and their persisted duration comes from actual completion rather than a synthetic regular protocol.
- Stale stop confirmations cannot change a completed workout or stop a running foreground service.

Automated coverage: six engine tests and four Room tests, in addition to the existing twelve persistence/backup tests. Includes pause/resume preparation, scheduler gaps without offline credit, double start, cancelled stop, ordinary workout after constructor, distinct constructor attempt IDs, stale writes, completion deduplication and process recovery.

Remaining limitation: legacy constructor checkpoints contain only a synthetic regular protocol, not the sequence of constructor steps. Such attempts are now stopped without credit on recovery, rather than incorrectly resumed. Supporting continuation requires a versioned checkpoint containing the full execution sequence and index. Regular workout recovery remains interrupted until the user resumes.

Physical-device checks remain outstanding. The timer runs in a foreground service with a partial wake lock; these mechanisms alone do not establish behavior on all Android vendors.

Device check for the owner:
1. Start a workout and let preparation finish.
2. Lock the screen for one minute, then check phase/time and audio.
3. Pause, wait, then resume: paused time and three-second preparation must not count as active time.
4. Finish: the result must appear once.
5. Separately test a regular workout after process termination: it must offer manual continuation without crediting the offline interval.
