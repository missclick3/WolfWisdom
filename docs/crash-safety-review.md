# Crash and responsiveness review

Reviewed: 2026-10-27. Scope: application startup, Room storage and migrations,
coroutines, navigation and error recovery, sharing, feedback, seasonal icons,
and widget configuration, rendering, updates and restoration.

Status: both findings were addressed after the initial review. The findings
below describe the original code; see the remediation and validation section
at the end for the implemented changes. Neither original failure was reproduced
on a device.

## 1. High: widget broadcast failures escape into the process exception handler

Locations in `app/src/main/java/ru/missclick/wolfwisdom/widget/FavoriteWidgetProvider.kt`:

- Lines 25–32: `onDeleted` calls `synchronizeQuotes` from a coroutine with only
  `try/finally`.
- Lines 70–77: the update dispatcher does the same after rendering widgets.
- Lines 35–46: restore also has no exception recovery around its work.
- Line 87: the shared scope has a `SupervisorJob`, but no exception handler.
- Lines 98–105: synchronization calls the Room DAO directly, bypassing the
  repository's `localResult` error conversion.

Concrete trigger: delete or update a widget while the database cannot accept
the membership update, for example because storage is full. A thrown
`SQLiteFullException` escapes the coroutine. `pending.finish()` still executes,
but `finally` does not handle the exception. A supervisor keeps sibling jobs
independent; it does not consume uncaught `launch` exceptions. On Android these
can reach the process uncaught-exception handler and crash the application,
including a currently visible activity.

The startup synchronization loop at lines 125–135 catches IO and SQLite
exceptions, but not Room `IllegalStateException` failures such as an unsupported
database version or schema validation failure. Since initialization always queues
synchronization, this can turn a database compatibility problem into a recurring
startup crash even without an active widget, instead of the main screen's normal
storage-error UI.

Recommended fix: handle recoverable storage and host failures at each background
operation boundary, preserving cancellation and the `finally` cleanup. Share the
repository's storage-error policy with membership synchronization. Add a scope
exception handler as a final reporting boundary, without relying on it for
operation recovery. Do not delete the user's database to hide migration errors.

Suggested validation: inject a failing membership writer and verify delete,
update and startup synchronization do not propagate the error; verify each
broadcast result finishes exactly once. Include a database-open compatibility
failure separately from SQLite failures.

Reference: [Kotlin coroutine exception handling](https://kotlinlang.org/docs/exception-handling.html).

## 2. Medium: widget broadcasts can exceed Android's execution deadline

Locations: `FavoriteWidgetProvider.kt`, lines 35–46 and 70–77; shared mutex at
line 88; `update` at line 165 onward. The pin receiver in
`WidgetConfigurationActivity.kt`, lines 294–310, uses the same update path.

Each receiver holds its `goAsync()` result until its coroutine finishes. Update
batches process all widget IDs serially, and each update waits for a global
mutex that is also used by other updates, deletion and replacement. Rendering
may produce several size variants, decode images, lay out text and recolor
backgrounds. There is no deadline or bounded batch duration.

Trigger: a large restored widget batch, slow storage or a queued update behind
expensive rendering can leave a broadcast unfinished past the platform deadline.
Moving work to `Dispatchers.IO` does not remove that deadline. This is an ANR
risk ("application not responding"), rather than a proven ordinary-use crash.
Actual timing has not been measured on a device.

Recommended fix: coalesce updates and move potentially long batches to a
scheduled job, keeping broadcast work short. Bound lock waits and the amount of
work owned by a broadcast. A coroutine timeout alone is insufficient for long
synchronous rendering calls that do not cooperate with cancellation.

Suggested validation: deliberately delay a widget update while another receiver
is waiting, and verify that receiver completion does not wait for an entire
render batch. Measure restore/update batches on a slow device.

Reference: [Android broadcast receiver ANRs](https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs#broadcast-receiver).

## Verification and limits

- Ran `:app:testDebugUnitTest` offline with a three-minute external deadline and
  bounded console capture. Gradle succeeded in about 31 seconds; the test task
  was up to date and reused existing results: 69 tests, zero failures/errors.
  This was not a fresh execution of all test bodies.
- Inspected the existing debug lint report: zero errors, 59 warnings. That
  report predates this review and was not regenerated.
- The Gradle run emitted a metrics access warning for the user's `.android`
  directory. No further Android test/build runs were attempted. The owned
  launcher and single-use Gradle daemon exited; no emulator was started.
- Existing widget tests cover rendering fallbacks, sizing, membership,
  localization, configuration and stored state. They do not establish safety
  for the broadcast exception and deadline cases above.
- The repository already converts ordinary quote-storage exceptions into domain
  failures and preserves coroutine cancellation. Sharing catches ordinary
  exceptions around export and chooser launch. Widget rendering has bitmap
  budgets, a bounded cache and an allocation fallback. These protections reduce
  risk but do not cover the two findings.
- No device tests, release runtime tests, low-storage fault injection or ANR
  timing measurements were performed. This review cannot certify the absence
  of other crashes.

## Remediation and validation

Implemented on 2026-10-27:

- Added `widgetOperation` to contain ordinary storage, schema and launcher
  exceptions while propagating cancellation and fatal errors. Startup membership
  synchronization uses this boundary, and `WidgetUpdates.update` returns false
  on an ordinary operation failure so configuration/replacement can show errors.
  The application-owned update scope also has a final exception-reporting boundary.
- Widget update, delete, restore and pin receivers now enqueue work for
  `WidgetWorkService`, a permission-protected native `JobService`. Receivers no
  longer own database, rendering or mutex waits through `goAsync`.
- The job processes items off the main thread, finishes failed items without
  an automatic storage retry loop, and leaves canceled work unfinished for
  JobScheduler to redeliver. Empty-queue completion uses `dequeueWork`, avoiding
  the race where `jobFinished` could discard concurrently arriving work.
- Restore stores an operation token atomically with the ID remap, protecting
  overlapping bindings if an interrupted job is delivered again.
- No dependency, database version, migration or destructive recovery was added.

Validation after the changes:

- `:app:testDebugUnitTest`: fresh execution, 76 passing tests, including seven
  new tests for operation failures, queue continuation, cancellation and delayed
  completion.
- `:app:compileDebugAndroidTestKotlin`: passed. A new device test covers repeated
  restore delivery with overlapping IDs; this test was compiled, not executed.
- `:app:lintDebug`: passed, zero errors and 56 warnings.
- `git diff --check`: passed.
- Android tooling used the existing workspace cache with a workspace-local
  JVM home and Android user directory. The first attempt had conflicting Android
  preference locations and failed before compilation; the corrected run completed
  without the earlier user `.android` access warning.
- All owned Gradle processes exited. Temporary check runner and daemon logs were
  removed. No emulator was started.

Remaining validation: real-device restore/pin behavior, release runtime behavior
and slow-launcher timings. Android can defer scheduled work under background
execution limits; broadcasts return without waiting for that work.

Scheduling lifecycle reference:
[Android JobParameters work queue](https://developer.android.com/reference/android/app/job/JobParameters#dequeueWork()).
