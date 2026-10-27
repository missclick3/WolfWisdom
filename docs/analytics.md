# AppMetrica events

`METRIC_KEY` is read from ignored `local.properties` (or the `METRIC_KEY`
environment variable on CI) into **release-only** `BuildConfig.METRIC_KEY`.
Debug builds do not need this key and do not contain AppMetrica SDK.

In debug, `AnalyticsRuntime` does not initialize an SDK. Its
`LoggingAnalyticsTracker` writes custom events to Logcat under `AppAnalytics`,
without transmitting them. This also applies to the default debug instrumented
test APK. In release, SDK 8.6.0 starts in `WolfWisdomApplication`; Activity
sessions and crash reports are enabled. Location, advertising identifiers and
automatic revenue tracking are disabled; the transitive `AD_ID` permission is
removed from the app manifest. Release statistics start after initialization;
no consent UI has been added. Update the privacy policy and store data
disclosures before distribution. See [release configuration](release.md).

## Architecture

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
`main`, `saved`, `settings`, `contacts`, `error`, `widget_configuration`.
`widget_placement` is an overlay and uses its own opened event.

## Prepared for future UI

`AboutAnalytics` and `AboutDocumentAnalytics` define `about`, `licenses`,
`privacy_policy`, `terms` visits and `about_document_clicked(document=...)`.
These screens are not implemented, so these events are not emitted yet.
`SavedAnalytics.searchClearClicked()` defines `search_clear_clicked`; there is
currently no separate clear-search button. Clearing text by typing is not a click.
Outside taps, system Back and selecting a menu item do not emit
`menu_close_clicked`; there is currently no separate close button in the drawer.

## Verification

Run from PowerShell (the local directories avoid writes outside this workspace):

```powershell
$env:ANDROID_USER_HOME = Join-Path $PWD '.gradle-user-home/android'
.\gradlew.bat --gradle-user-home .gradle-user-home '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --console=plain
```

Verified: debug APK built, all 69 JVM tests passed (including 10 new analytics
tests), and existing Android UI tests compiled. UI tests were not run on a device.
Unit tests use recording trackers, without live SDK calls.
Before release, verify event arrival in the AppMetrica dashboard on a device and
update disclosure text for the configured SDK. Production dashboard delivery
cannot be established by a local build or unit tests.

Official SDK integration documentation:
https://appmetrica.yandex.ru/docs/en/sdk/android/analytics/quick-start
