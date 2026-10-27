# Виджеты «Любимая цитата»

## Integration

The app remains one Android module, minSdk 26, compileSdk/targetSdk 37. No dependencies or SDK levels were changed. No AGENTS.md or CLAUDE.md was found in the project or its parent directories. Existing unrelated staged SVG removal was left alone.

Three thin AppWidgetProvider declarations expose 2×2, 3×2 and 4×2 in the host picker. They share rendering, configuration, lifecycle and storage. Initial reference dimensions are 160/248/336 × 160 dp; hosts choose actual geometry. Android 12+ receives up to three actual host sizes in a size-keyed RemoteViews payload. Older hosts receive portrait/landscape variants. Updates are complete replacements. Resizes never change the quote binding or theme.

Classic RemoteViews was selected instead of adding Glance: the app has no existing widget stack, and direct control of bitmap allocation, payload variants and submission failure is useful with targetSdk 37. The configuration Activity uses the existing Compose Material theme and actual font resources. It stays Moonlight regardless of widget theme.

QuoteRepository/Room/Koin remain the sole quote source. `getQuoteById` is a stable lookup that never moves reading history. Attribution uses the app's existing shared author, «— Волк»; the domain model has no per-quote author. The existing widget buttons now open theme configuration and use requestPinAppWidget. Confirming a widget leaves the quote's saved status unchanged. Launchers that do not support pinning show a Russian instruction to use their picker. In-app pinning requests the 2×2 provider; the picker offers all three sizes, and placed widgets are resizable.

Launcher placement binds the current main-screen quote; in-app placement binds the explicitly selected quote. Both remain fixed to that quote when reading history or saved quotes change. Liking and unliking never change a widget binding or hide its quote. Compact instance metadata uses private SharedPreferences (`favorite_widgets`) to store version, quote ID and theme. Version 3 reads existing version 1 and 2 records, retaining their quote and theme while dropping the former saved-status restriction. Pending pin requests from earlier versions are also compatible. No shortened quote is stored. A committed empty instance can bind the current quote on a later update; a nonempty binding is never replaced automatically. A physically deleted or unavailable quote has an explicit unavailable state. Tapping any widget opens the application main screen.

All widget states use an immutable, instance-specific PendingIntent that starts MainActivity on its normal main screen. ACTION_MAIN/CATEGORY_LAUNCHER with NEW_TASK/CLEAR_TASK resets an already open secondary destination too. Widget taps carry no quote ID or deep link and do not change the reading cursor. The widget-specific quote-detail screen and navigation route have been removed.

## Configuration and lifecycle

Launcher configuration validates both ID and provider, begins with RESULT_CANCELED, and writes nothing until confirmation. The ViewModel guards duplicate submissions, survives rotation, and uses SavedStateHandle for a restored draft. Reconfiguration loads committed state. Confirmation commits, explicitly updates, then finishes with RESULT_OK and the same ID. Terminal update rejection leaves the screen open with an error. Pin requests have unique callback tokens, private temporary records expiring after a day (cleaned on subsequent requests), and commit instance state only after host acceptance. Android's pin API intentionally skips the provider configuration Activity; configuration occurs before the request.

Room invalidation, widget options, app/package startup, boot, wallpaper broadcasts, WallpaperManager color callbacks (API 27+), and configuration broadcasts trigger updates. A conflated channel serializes data/color updates; a mutex serializes rendering/submission. No periodic work, alarm, network, or refresh control was introduced. Provider restoration snapshots old settings before remapping IDs, including overlapping IDs. Deletion removes only that instance's state; the bounded shared cache can be evicted safely because active RemoteViews retain their own bitmap references.

## Text and assets

Normal widget text is a transparent, tightly bounded ARGB_8888 text block, separate from the landscape. StaticLayout measures and draws the same Tinos Regular and Inter Regular Typefaces from the application resources. Emoji/characters outside those fonts use Android's glyph fallback. Alpha is retained. TalkBack receives the complete quote and author on the clickable root, not the excerpt. Fallback rendering uses real system serif/sans text, with a solid drawable and no bitmap actions; it intentionally substitutes system fonts.

After subtracting 16 dp padding, measured author height and 8 dp spacing, fitting tests 20/24 sp (≤4 lines), 18/22 (≤4), then 16/20 (≤5). The largest complete layout wins. Only the last option is truncated, using ICU grapheme boundaries with additional protection for older ICU emoji ZWJ/skin-tone boundaries. Word boundaries are preferred when they do not discard most of the prefix. Explicit breaks and unbroken words use native wrapping. The available height, including system font scaling, overrides line-count limits. If no safe layout fits, the native fallback reserves attribution separately. Text never modifies the stored original.

The widget backgrounds now use the exact supplied light (`22_38_55`) and dark (`22_38_46`) PNG artwork, packaged separately as nodpi resources. Preserve their original orientation and right-anchor center-cropping at every widget size so the wolf remains visible. Do not mirror the artwork or simulate the light landscape by bleaching the night image. A horizontal protective surface gradient supports text; long text uses a lighter 25% overlay to keep more of the landscape visible.

Implementation requirement for Android theme: use the light landscape as one base image and map its luminance into shadow, midtone and highlight colors from the system accent palette. Preserve mountain, forest, water and fog detail. The green (`22_38_24`) reference is an example of dynamic coloring, not a fixed green asset. Resolve separate light/dark tonal ranges and text colors. Re-render on wallpaper/color/configuration callbacks. API 26-30 uses deterministic blue tonal fallback according to system night mode. Cache keys include the complete resolved palette, light/dark mode, resource version, pixel size, density and text overlay. Recolor in-place using a single scanline and a 256-entry LUT; retain bounded RGB_565 outputs and the existing total-payload memory checks.

Figma's original full layout could not be inspected because of its plan limit. These supplied landscape assets replace the previous artwork substitutions. Full screen pixel fidelity to Figma remains unverified.

## Android 17 memory controls

Reviewed the [Android 17 bitmap ceiling documentation](https://developer.android.com/about/versions/17/behavior-changes-17#memory-limit-widget) and the [pinning API contract](https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,android.os.Bundle,android.app.PendingIntent)).

* API 30+ budget uses currentWindowMetrics bounds; older devices use active display metrics. All arithmetic for memory uses Long: `6L * widthPx * heightPx`.
* The internal payload ceiling is the smaller of 4 MiB and one quarter of the system ceiling. It is divided among all variants before rendering; each variant includes text and background. No compressed-file-size estimates or internal cache reflection are used.
* DP is converted to pixels using display density. Text uses SP conversion, including Android's font scaling. Rendering/decoding runs on Dispatchers.IO. Only the selected theme is rendered.
* BitmapFactory first reads image bounds and selects a power-of-two sample. The actual decoded config and allocationByteCount are inspected and logged. On the API 35 test device, PNG decode ignored RGB_565 and returned ARGB_8888 (1112×834, 3,709,632 bytes); this temporary bitmap is recycled after transformation. The separate opaque output is explicitly RGB_565.
* The alpha text allocation is reserved first. Remaining budget reduces decorative output resolution, then drops decoration if fewer than 2 KiB remain. Actual output allocationByteCount is rechecked against the variant allowance. If text cannot fit the budget, a zero-bitmap native widget is sent.
* The 4 MiB LruCache contains backgrounds only. Keys include resource/version, theme, output pixel dimensions, density, complete resolved palette/mode and long-text overlay. Eviction never recycles a bitmap that may still be referenced by the host. Text bitmaps are not cached. Resizing cannot grow the cache beyond its byte cap.
* Each submission is a full update. IllegalArgumentException/SecurityException at the submission boundary triggers one bitmap-free replacement. Rejection of that replacement is logged and returned to configuration, without a retry loop. Rendering resource/allocation failures also fall back. Coroutine cancellation is preserved.
* No content URIs, public storage, remote image URLs or FileProvider paths are involved.

Diagnostics use `FavoriteWidget` and include ID, dp/px bounds, variants, decoded/output configs and allocations, allowance and cache size. Quote text is never logged.

Debug APK only (the exported fault-injection receiver does not exist in release):

```powershell
adb shell am broadcast -n ru.missclick.wolfwisdom/.widget.WidgetDebugReceiver -a ru.missclick.wolfwisdom.WIDGET_DEBUG --ez fail_images true
adb shell am broadcast -n ru.missclick.wolfwisdom/.widget.WidgetDebugReceiver -a ru.missclick.wolfwisdom.WIDGET_DEBUG --ez fail_images false --el budget_bytes 0
# Restore normal budget (clamped to the runtime internal ceiling):
adb shell am broadcast -n ru.missclick.wolfwisdom/.widget.WidgetDebugReceiver -a ru.missclick.wolfwisdom.WIDGET_DEBUG --el budget_bytes 9223372036854775807
adb shell dumpsys appwidget
adb logcat -s FavoriteWidget
```

## Verification

Build commands (PowerShell needs quotes around dotted -P options):

```powershell
.\gradlew.bat --offline --gradle-user-home .gradle-user-home '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e package ru.missclick.wolfwisdom.widget ru.missclick.wolfwisdom.test/androidx.test.runner.AndroidJUnitRunner
```

Gradle's connected-test task could not resolve uncached `com.android.tools.utp:gradle-work-action:32.4.1` offline. Tests are instead run directly with the same AndroidJUnitRunner through adb. In-process Kotlin compilation avoids writing Kotlin daemon markers outside the permitted workspace.

Verified on the available API 35 emulator (2560×1600, density 320), plus temporary phone geometry overrides. Display size, free rotation and font scale 1.0 were restored after testing.

| Check | Result |
| --- | --- |
| assembleDebug / assembleDebugAndroidTest | Passed |
| testDebugUnitTest | 59 tests passed, including 4 new budget tests |
| lintDebug | Passed, no errors; existing warnings and KTX style suggestions remain |
| Widget instrumentation package, normal scale | 13 tests passed |
| Configuration tests, short 844×390 dp display and 200% fonts | 5 tests passed; theme cards and confirmation reached through scrolling |
| Reconfiguration/confirmation, portrait 390×844 dp | 1 test passed |
| git diff --check | Passed |

Follow-up navigation change: removed the widget quote-detail destination and quote/deep-link intent handling. Rebuilt both APKs and passed the targeted API 35 test that opens the main screen first, navigates to Saved, then taps the same widget PendingIntent and verifies that the main screen opens again. Earlier full-suite results above predate this navigation simplification.

Follow-up empty-favorites change: the default binding now falls back to the main-screen current quote. Rebuilt both APKs, passed the JVM suite, and passed five targeted API 35 tests for selection priority, stable unsaved fallback binding, state isolation/restoration, version 1 compatibility, and fallback reconfiguration/recreation without adding a favorite. The fallback permission is persisted per instance; existing favorite bindings retain their removal behavior.

The instrumented tests cover all nine size/theme combinations, complete size selection including reachable 18/16 sp options, overflowing text, explicit newlines, unbroken words, composed accents, family emoji/skin tones/flags, alpha text output, author visibility at doubled font scale, first/latest favorite selection, edited/removed/deleted quotes, empty favorites, state isolation/reopening/deletion/overlapping restoration IDs, invalid configuration IDs, cancel without persistence, retained theme on Activity recreation, initial update and result ID, reconfiguration, opening the main screen from a widget, including when the app is already on the saved-quotes screen, forced image failure, reduced memory allowance and bounded cache across 45 raster widths. The system AppWidgetHost test binds two real IDs, submits RemoteViews through AppWidgetManager, resizes one and retrieves its view from the system while preserving the other instance's theme.

Visual QA inspected all nine raster outputs and actual hardware-composited configuration screenshots, including rounded corners and long text with visible attribution. Software Canvas captures of standalone RemoteViews do not emulate hardware outline clipping; the actual Activity preview does show the 24 dp rounded outline. `adb shell dumpsys appwidget` was captured with a configured instance. Memory diagnostics confirmed actual decoded configs/allocation sizes and complete variant submissions, rather than compressed resource sizes. Local artifacts are under `build/widget-verification/`: contact sheet, individual size/theme images, configuration screenshots, instrumentation output, memory log and service dumps.

Still requiring verification: API 26 execution and its older ICU/font fallback; Android 17/API 37 enforcement on a device (SDK is installed, available emulator is API 35); physical phone/foldable and launcher picker/drag-resize/pin acceptance/cancellation on OEM launchers; force-killed process and full backup/restore lifecycle; actual wallpaper/palette and system-night transitions including process-dead delivery; TalkBack traversal on a host; physical corruption of packaged backgrounds; and induced Binder rejection on API 37. Persistence reopening, restore mapping, callbacks and native fallback are tested or implemented, but these broader scenarios are not reported as passed. Android/OEM background delivery restrictions can delay palette raster regeneration until a supported callback, widget event or application restart; there is deliberately no polling service. Full Figma visual comparison remains blocked by its plan limit.

## Supplied landscape follow-up (2026-10-26)

Replaced both widget artwork substitutions with the supplied original PNGs (SHA-256 matched). Android uses one luminance master and separate light/dark system accent ranges; the sage reference is illustrative. Added a regression check for palette-only cache invalidation, repeat cache hits, retained landscape detail, and darker night output.

Build, instrumentation APK, JVM suite (59 tests) and lint passed. The API 35 emulator rendering suite passed all 7 tests, covering all nine widget size/theme combinations, reduced budget/image failure fallback, bounded resize cache, and the new palette test. Its first invocation hit a process-startup ANR before tests; the retry completed successfully. Rendered light/dark, lilac and sage outputs were inspected under `build/widget-verification/artwork-update/`. Android 17 enforcement and real wallpaper transition delivery remain device verification items.

## Interface languages

All 76 translatable interface resources are supplied in English and Russian. English is the default fallback; Russian devices select values-ru. Widget picker labels, configuration, theme names, accessibility descriptions, pin instructions, error states and attribution use resources. Quote content remains in its original language. Placed widgets refresh on locale broadcasts, and retained configuration drafts reload localized quote presentation after Activity recreation. Error state stores resource IDs so its text resolves in the current locale.

Localization verification: English/Russian/default resource parity passed for 76 translatable strings. Build and lint passed (0 errors; existing warnings remain). All 18 widget instrumentation tests passed on API 35, including localized English, Russian and unsupported-language fallback contexts, configuration flows, data binding, rendering and memory fallbacks.


## Independent widget quotes (2026-10-27)

Removed automatic saving from pin confirmation and all saved-status checks from widget loading. Launcher placement now selects the current quote independently of favorites. Updated regression tests cover unliking without hiding the quote, stable identity after navigation/favorite changes, unsaved pin confirmation and reconfiguration, and legacy state compatibility. Previous verification results above describe earlier revisions.

Validation: debug and instrumentation APK builds passed, all 59 JVM tests passed, lint passed, and `git diff --check` passed. Updated widget instrumentation tests compiled but were not executed: no device was connected, and emulator startup was blocked by filesystem permissions on its lock files.


## Widget membership indicator (2026-10-27)

Added `isOnWidget` to the Room DTO and domain quote with schema migration 2 to 3. The main-screen and saved-quote widget buttons use filled artwork and selected styling while at least one active configured widget displays that quote. Committed state writes, deletion and restoration request reconciliation; startup also reconciles existing bindings against launcher IDs. Rebinding to a different quote uses the same path, preparing for future quote selection. Draft/pin cancellation does not set membership. Multiple instances keep the flag true until the last binding is removed. Likes and saved timestamps remain independent.

Validation: both debug APK builds, all 59 JVM tests, lint, and diff checks passed. Installed the debug and test APKs on the connected phone and passed 12 instrumented tests across WidgetMembershipTest, WidgetDataTest, WidgetStateTest and SavedQuotesDatabaseTest, including upgrades from schema 1 and 2.
