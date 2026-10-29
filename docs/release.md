# Debug and release builds

| Setting | Debug | Release |
| --- | --- | --- |
| AppMetrica SDK | Not packaged | Packaged; initialized only after consent |
| Custom events | Logcat after consent, tag `AppAnalytics` | Sent only while consent is active |
| Sessions/crash reports | No SDK, no transmission | Forwarded only while consent is active |
| App logs | `AppLog`, enabled | Disabled; Logcat calls removed by R8 |
| Optimization / obfuscation | Disabled | Enabled for the whole app |
| Code / resource shrinking | Disabled | Enabled by AGP 9.4 `optimization` DSL |
| Debugger / JNI debugger | Debugger enabled | Disabled |
| Version name | `1.0-debug` | `1.0` |
| Network permissions | Not requested by this app | INTERNET / ACCESS_NETWORK_STATE |
| Cleartext HTTP | Android target defaults | Explicitly forbidden |
| Production metric key | Not compiled in | Required and validated |
| Signing | Standard debug certificate | No debug signing fallback; configure production signing |

Debug and release currently use the same application ID. Switching installed
variants may require uninstalling the previous build when certificates differ.
Do not uninstall an installation with needed local data just to switch variants.
If side-by-side installs are wanted later, add an application ID suffix and adapt
seasonal launcher alias resolution and the instrumented package assertion.

## Implementation

- `src/debug/.../analytics/AnalyticsRuntime.kt` provides local logging only.
- `src/release/.../analytics/AnalyticsRuntime.kt` initializes AppMetrica and
  supplies its tracker. The SDK dependency is `releaseImplementation`.
- `METRIC_KEY` can be supplied through the environment or ignored
  `local.properties`. Validation is attached to release builds; debug does not
  require the production key. The release APK necessarily contains the SDK app
  key; it is an app identifier, not a signing credential.
- `AppLog` gates application logging using `BuildConfig.LOGGING_ENABLED`.
  `src/release/keepRules/release.keep` additionally removes Android Logcat calls
  from optimized release code and preserves line information for crash retracing.
- AGP 9.4's `optimization { enable = true }` enables shrinking of code and
  resources. The previous experimental `packageScope` restriction was removed.
  Consumer rules supplied by libraries and Android's default rules remain active.
- The debug widget receiver lives in `src/debug` and is absent from release.

## Build and checks

`base.archivesName` in `app/build.gradle.kts` sets the APK/AAB filename prefix to
`wolf-wisdom`. For example, debug builds produce `wolf-wisdom-debug.apk` and
`wolf-wisdom-debug.aab`; release bundles use `wolf-wisdom-release.aab`.
An unsigned release APK retains the `-unsigned` suffix.

```powershell
$env:ANDROID_USER_HOME = Join-Path $PWD '.gradle-user-home/android'
$env:JAVA_TOOL_OPTIONS = '-Duser.home=' + (Join-Path $PWD '.gradle-user-home')
.\gradlew.bat --gradle-user-home .gradle-user-home '-Pkotlin.compiler.execution.strategy=in-process' :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease --console=plain
python scripts/verify_build_variants.py
```

The Python check inspects the actual APK DEX tables, merged manifests,
generated debug BuildConfig and release R8 mapping/configuration. It verifies
SDK/key isolation in debug, debug logging, release Logcat removal, obfuscation,
cleartext policy and exclusion of the debug receiver. It does not launch APKs.

Verified locally: both APKs built; 69 debug JVM tests passed; Android UI tests
compiled; release vital lint passed; all artifact checks above passed. Release
was not installed/launched and the resulting release APK is unsigned.

Building, linting and JVM tests do not launch the release app or send SDK events.
Debug UI tests have no analytics SDK to initialize. A manually launched release
app does transmit statistics, including when launched for release smoke testing.

Release outputs include the unsigned APK and R8 diagnostics under
`app/build/outputs/mapping/release/`. Do not confuse the locally built unsigned
APK with an artifact ready to distribute.

## Before distributing a release

1. Configure a dedicated upload/signing key through Android Studio or CI secrets.
   Keep the keystore and passwords out of Git; never sign release with the debug
   certificate. Back up the signing credentials securely.
2. Increase `versionCode` for each published update and set `versionName`.
3. Build a signed AAB for Google Play (`:app:bundleRelease`) or a signed APK for
   the chosen distribution channel. Archive the exact artifact, version,
   source commit and `mapping.txt` together. Do not reuse a mapping from another
   build, even if its version name is identical.
4. Configure release crash deobfuscation. The AppMetrica Gradle Plugin 2.1.0
   currently documents support only through AGP 8.13.x and Gradle 9.2.1, while
   this repository uses AGP 9.4.1 and Gradle 9.6.0. It was not added automatically.
   Retain mapping files and establish a compatible upload/archive pipeline before
   relying on readable production crash reports. Local JVM tests do not check
   server-side deobfuscation.
5. Run release smoke tests on real devices, especially typed navigation,
   Room persistence/migrations, quote sharing, widget creation/replacement,
   process recreation, localization and seasonal launcher switching. Compilation
   does not prove runtime behavior after R8 optimization. Use a test telemetry
   destination when exercising release analytics to avoid polluting production.
6. Update the privacy policy and store data disclosures for AppMetrica; confirm
   the chosen consent/opt-out behavior for the distribution regions.
7. Consider Baseline/Startup Profiles and Macrobenchmark once release behavior is
   stable. Measure cold startup and scrolling before choosing optimizations.
8. Make release lint, JVM tests and artifact checks part of CI. Keep dependency
   and SDK updates reviewed; avoid adding broad package-wide keep rules simply
   to silence an R8 error.

## Sources

- [Android: prepare for release](https://developer.android.com/studio/publish/preparing)
- [Android: enable R8 optimization (AGP 9.3+ DSL)](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
- [Android: experimental packageScope limitations](https://developer.android.com/topic/performance/app-optimization/optimize-specified-packages)
- [AppMetrica Gradle Plugin compatibility and mapping upload](https://appmetrica.yandex.ru/docs/ru/sdk/android/analytics/android-gradle-plugin)
