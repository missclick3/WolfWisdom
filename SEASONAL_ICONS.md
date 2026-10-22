# Seasonal launcher icons

The six 512 × 512 PNGs from `wise_wolf_icons_512_archive.zip` live in
`app/src/main/res/drawable-nodpi`: five seasonal icons and `wise_wolf_base.png`.
Adaptive launcher resources let Android apply the device's icon shape. Each
colored foreground uses an inset bitmap with fill gravity, fitting the source
illustration into the inner 68dp of the 108dp adaptive layer. The shared fraction
and dark navy backdrop are defined in `res/values/launcher_icons.xml`. This keeps
the moon and seasonal decoration visible instead of enlarging and cropping the
wolf's face. The original PNG artwork is unchanged.

## Android themed icons

All seven adaptive resources (default, round, and five seasonal variants) live
in `app/src/main/res/mipmap-anydpi-v26`. Every resource also references
`@drawable/ic_wolf_monochrome` in its `<monochrome>` layer.

On Android 13+, a launcher that supports themed icons can tint this dedicated
wolf-and-crescent vector when the user enables themed icons. Its opaque white
fill is only an alpha mask; the launcher determines the final colors. The mark
traces the supplied wolf-and-crescent reference into vector contours, preserving
the flowing fur, calm eye, nose, and facial negative space. The white source
background is transparent in the drawable. The mark uses a 108dp viewport, with
all visible geometry inside a centered 62dp circle, within the 66dp safe zone.

Android 8–12 continue to use the existing colored adaptive layers. The shared
monochrome layer does not change seasonal selection or launcher aliases.

Build verification: `./gradlew.bat :app:assembleDebug` (no tests required).
For a device check, enable themed icons on an Android 13+ supporting launcher
and inspect the icon at the default launcher size in both light and dark mode.

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

The app schedules an inexact check on every resume for the next local
midnight. The receiver also checks after reboot, app updates, and clock/time-zone
changes. It reschedules using the current time zone, including daylight saving.
No network, additional libraries, or exact-alarm access is needed.

Launcher alias changes are deferred while any activity is started. After all
activities stop, a one-second grace period allows recreation and activity
transitions to finish; returning to the app cancels that pending change. This
also guards updates from receivers while the app is visible. Disabling the alias
that launched a visible task can close that task even with `DONT_KILL_APP`.

On a fresh install, the base icon (`DefaultLauncher`) is the launcher icon until the app is first
opened and then sent to the background. After that, automatic checks are scheduled. Android can defer alarms and
launchers can cache icons, so a change may appear after midnight. Force-stopping
the app stops background checks until it is opened again. The application-level
icon used by some system screens is the static base icon. The default alias is
disabled when a seasonal alias is enabled, so only one launcher entry remains.

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

First-launch regression check: on a clean test installation, open the base icon
and verify the screen stays open. Rotate the device and verify it still stays
open without changing icons. Press Home, wait at least one second (plus launcher
refresh time), and verify the seasonal icon appears and reopens the app. Repeat
with a date/time-zone broadcast while the app is visible: the icon should change
only after the app goes into the background. Test on Android 12 or older and
Android 13+ to cover both alias update paths.
