# Seasonal launcher icons

The five original 512 × 512 PNGs live in `app/src/main/res/drawable-nodpi`.
Adaptive launcher resources let Android apply the device's icon shape.

`SeasonalIcon.forDate` selects the icon using the device's local calendar:

| Dates (inclusive) | Icon |
| --- | --- |
| March 1 – May 31 | Spring |
| June 1 – August 31 | Summer |
| September 1 – November 30 | Autumn |
| December 1 – December 23, January 9 – February's last day | Winter |
| December 24 – January 8 | New Year |

`SeasonalIconManager` enables one launcher activity alias and disables the others.
The real activity stays enabled. Android 13+ applies alias changes in one batch;
older versions enable the next icon before disabling the previous one.

The app checks on every resume and schedules an inexact check for the next local
midnight. The receiver also checks after reboot, app updates, and clock/time-zone
changes. It reschedules using the current time zone, including daylight saving.
No network, additional libraries, or exact-alarm access is needed.

On a fresh install, Winter is the initial launcher icon until the app is first
opened. After that, automatic checks are scheduled. Android can defer alarms and
launchers can cache icons, so a change may appear after midnight. Force-stopping
the app stops background checks until it is opened again. The application-level
icon used by some system screens is the static Winter fallback.

Validation: `./gradlew.bat testDebugUnitTest assembleDebug lintDebug`.
Date tests cover every season, the holiday boundaries across years, and leap day.
In this session, the three new production Kotlin files compiled against Android
API 37 using the cached Kotlin 2.3.21 compiler, and all 21 date tests passed via
JUnit. The full Gradle build/lint could not run because the configured Kotlin
2.2.10 compiler dependencies were unavailable offline. Device behavior has not
been verified here.
On a device, open the app once, change the local date across each boundary, and
check that one launcher icon remains and opens the app. Also check a midnight
transition, reboot, and time-zone changes; allow the launcher time to refresh.
