# AppMetrica events

`METRIC_KEY` is read from ignored `local.properties` (or the `METRIC_KEY`
environment variable on CI) into **release-only** `BuildConfig.METRIC_KEY`.
Debug builds do not need this key and do not contain AppMetrica SDK.

In debug, `AnalyticsRuntime` does not initialize an SDK. After consent its
`LoggingAnalyticsTracker` writes custom events to Logcat under `AppAnalytics`,
without transmitting them. This also applies to the default debug instrumented
test APK. In release, SDK 8.6.0 starts only after an explicit analytics grant;
sessions and crash reports are forwarded only while consent is active. Location, advertising identifiers and
automatic revenue tracking are disabled; the transitive `AD_ID` permission is
removed from the app manifest. The first main-screen launch displays a styled,
optional consent dialog; Back/outside dismissal is a saved refusal. Settings
contains an Analytics and diagnostics switch. Update the privacy policy and store data
disclosures before distribution. See [release configuration](release.md).

## Architecture

- `AnalyticsConsentController` gates every custom event, including debug logging.
  Unknown/declined events are discarded, never replayed on a later grant.
- `AnalyticsConsent` restores the installation's choice from `analytics_consent`
  preferences. These preferences are excluded from cloud backup/device transfer.
- Release does not call AppMetrica before the first grant. SDK automatic sessions,
  app-open events, crashes, native crashes and ANR monitoring are disabled.
  Activity lifecycle callbacks and a consent-gated uncaught-exception handler
  forward sessions/JVM crashes while enabled. The original crash handler is always
  called, preserving Android's crash handling.
- Revocation closes the custom-event gate, disables SDK data transmission and
  pauses the session. No subsequent app UI events or JVM crashes are forwarded.
  The SDK remains initialized until process exit; already queued SDK data is not
  deleted by this switch, and withdrawal does not delete server-side data.
- SDK activation starts with data sending disabled; it is enabled only after
  activation completes under an existing grant. `AnalyticsCollectionController`
  contains ordinary failures during activation, session changes, event/crash
  forwarding and withdrawal. A failure closes the local reporting gate and
  independently attempts to disable SDK transmission and pause the session.
  Cleanup failures are contained too. There are no repeated activation attempts
  in that process; a new process can try again if consent is still granted.
  The saved choice represents the user's consent, not SDK availability, and is
  not changed by an SDK failure. Cancellation and fatal errors propagate.
  SDK failure containment cannot guarantee shutdown of a malfunctioning SDK;
  already queued/server-side data is not deleted. The original uncaught-exception
  handler still runs even when forwarding a crash fails.
- The dialog includes expandable data details. It does not link to a placeholder
  privacy-policy URL; the full published policy still belongs in About/store metadata.

- `AnalyticsTracker` is the transport interface. Only the release source set's
  `AppMetricaAnalyticsTracker` imports the SDK, declared as `releaseImplementation`.
  Each build type defines `AnalyticsRuntime`; Koin supplies the variant's tracker
  to `ProvideAnalytics`.
- `ScreenAnalytics` defines screen visits and Back button clicks.
  `BaseScreenAnalytics` adds the `screen` parameter to every event.
- Each screen directory contains its own class (`MainAnalytics`,
  `SavedAnalytics`, `SettingsAnalytics`, `ContactsAnalytics`, `ErrorAnalytics`).
  Widget analytics live beside widget UI code.
- Screen Roots report UI actions before dispatching them. Repository retries,
  animation completion and search text changes do not report clicks.
- `NavigationAnalytics` observes navigation entry changes. The last reported
  entry ID is saved across recreation. Returning to another destination reports
  a new visit; rotation, recomposition and foreground resume do not.
- Widget configuration uses its own saved visit flag. Placement reports only
  when the overlay is actually shown, not when it directly opens configuration.
- Previews and isolated UI tests use `NoOpAnalyticsTracker` by default. Tests can
  provide a recording implementation through `LocalAnalyticsTracker`.

## Contract

All names and enum parameter values are stable English `snake_case`. Every event
has `screen`. Quote text, quote IDs, search input, contact URLs and widget instance
IDs are not included. Click events represent intent, not completed operations.

| Event | Additional parameters / trigger |
| --- | --- |
| `screen_opened` | `source=app_launch/menu/back/internal/launcher` |
| `menu_open_clicked` | Main header menu button |
| `menu_close_clicked` | Explicit menu toggle while open, if reachable |
| `menu_item_clicked` | `item=saved/settings/contacts/about/support` |
| `back_clicked` | Visible Back button; system Back is excluded |
| `quote_save_clicked` | `action=save/remove`, main |
| `quote_remove_clicked` | Saved quote remove button |
| `quote_share_clicked` | Main or saved |
| `quote_widget_clicked` | Main or saved |
| `quote_swiped` | `direction=next/previous`; recognized gesture, not operation success |
| `search_open_clicked` | Saved search button |
| `search_close_clicked` | Search close button; system Back is excluded |
| `sound_toggle_clicked` | `enabled` boolean, requested new value |
| `vibration_toggle_clicked` | `enabled` boolean, requested new value |
| `language_option_clicked` | `previous_language`, `language=ru/en` |
| `contact_clicked` | `target=boosty/telegram_channel/telegram_personal/email` |
| `widget_placement_opened` | `source_screen=main/saved/internal` |
| `widget_create_clicked` | `surface=placement/configuration` |
| `widget_theme_clicked` | `theme=dark/light/android`; changed selection |
| `widget_target_clicked` | Existing widget selected |
| `widget_replace_clicked` | Replace button |
| `widget_retry_clicked` | `surface=placement/configuration` |
| `widget_close_clicked` | `surface=placement`, explicit cross button only |
| `error_retry_clicked` | `error_source=main/saved` |

`screen_opened` on `error` also includes `error_source`. Implemented screens are
`main`, `saved`, `settings`, `contacts`, `about`, `licenses`, `privacy_policy`,
`terms`, `error`, `widget_configuration`.
`widget_placement` is an overlay and uses its own opened event.

`AboutAnalytics` and `AboutDocumentAnalytics` define `about`, `licenses`,
`privacy_policy`, `terms` visits and `about_document_clicked(document=...)`.
These events are connected to the About/document routes. Document contents,
selection and scrolling are not tracked; all events use the existing consent gate.

## Prepared for future UI

`SavedAnalytics.searchClearClicked()` defines `search_clear_clicked`; there is
currently no separate clear-search button. Clearing text by typing is not a click.
Outside taps, system Back and selecting a menu item do not emit
`menu_close_clicked`; there is currently no separate close button in the drawer.

## Verification

Run from PowerShell (the local directories avoid writes outside this workspace):

```powershell
$env:ANDROID_USER_HOME = Join-Path $PWD '.gradle-user-home/android'
$env:JAVA_TOOL_OPTIONS = '-Duser.home=' + (Join-Path $PWD '.gradle-user-home')
.\gradlew.bat --gradle-user-home .gradle-user-home '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --console=plain
```

Verified: debug APK built, all 82 JVM tests passed (including six consent
controller tests), release Kotlin compiled, and Android UI tests compiled.
The six new consent UI tests could not complete on the local API 35 AVD:
Android killed instrumentation during application startup with an ANR;
System UI also displayed an ANR. The dialog rendered in the actual debug app,
but landscape/large-font UI checks are not recorded as passing. All QA emulator
processes were stopped; no snapshots or unbounded redirected logs were created.
Unit tests use recording trackers, without live SDK calls.
Before release, verify event arrival in the AppMetrica dashboard on a device and
update disclosure text for the configured SDK. Production dashboard delivery
cannot be established by a local build or unit tests.

Official SDK integration documentation:
https://appmetrica.yandex.ru/docs/en/sdk/android/analytics/quick-start
