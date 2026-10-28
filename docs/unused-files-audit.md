# Unused files and assets audit

Audit date: 2026-10-29. The findings below describe the state before cleanup.

## Cleanup completed 2026-10-29

At the user's request, removed the two unused launcher vector files, all 18 SVG originals under `app/src/main/artwork/`, and the two template tests listed below (22 files total). Removed seven unused color definitions and both unused strings from all three language configurations (six string definitions).

Kept all referenced app assets, production Kotlin files, database schemas, caches, AVD data, release artifacts, and existing reports. The pre-existing Git entry described at the end was left untouched.

Validation: all remaining source XML parses successfully, and no source/build references remain to the removed resources, SVG source directory, or template test classes. `git diff --check` passes. No build or emulator was launched for this cleanup.

## Method and limits

- Scanned Kotlin, manifests, resource XML, build configuration, and asset paths across main, debug, release, unit-test, and instrumentation-test sources.
- Compared current resource references with the existing Android lint report (`app/build/reports/lint-results-debug.xml`, last modified 2026-10-28 23:52 local time). Its 11 `UnusedResources` findings match the current scan. Lint was not rerun.
- Checked Kotlin declarations rather than relying on filenames: many files contain extension functions or declarations whose names differ from their filenames. Manifest entry points were checked separately.
- Compared image/font/audio/SVG files by SHA-256 for exact duplicates. Measured existing generated directories separately from project sources.
- This is a static audit, not proof of runtime reachability. No builds, tests, emulators, downloads, or background processes were started.

## High-confidence unused Android resources

| Location under `app/src/main/res/` | Finding | Suggested cleanup |
| --- | --- | --- |
| `drawable/ic_launcher_background.xml` | No references; lint agrees. Current adaptive icons use `launcher_icon_background` instead. | Remove file (5,606 bytes). |
| `drawable/ic_launcher_foreground.xml` | No references; lint agrees. Current adaptive icons use `wise_wolf_base` and seasonal variants instead. | Remove file (1,702 bytes). |
| `values/colors.xml` | Seven unused template colors: `purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`, `black`, `white`. | Remove only those entries. Keep `wolf_window_background`, which is used. |
| `values/strings.xml`, `values-en/strings.xml`, `values-ru/strings.xml` | `loading_quote` and `quote_is_set_to_widget` have no references. | Remove both entries in all three configurations (six XML entries). |

These are 11 distinct resource names: two drawable files, seven colors, and two strings. The two drawable files total 7,308 bytes before packaging; this is not an APK size estimate.

## SVG originals: unused by the build, potentially useful design sources

`app/src/main/artwork/` contains 18 SVGs totaling 10,422 bytes (10.18 KiB). No configured source set or source/build reference consumes this directory. Android vector XML counterparts exist under `res/drawable/`.

| Folder | Files |
| --- | --- |
| `contacts/` | `boosty.svg`, `external.svg`, `mail.svg`, `telegram.svg` |
| `drawer/` | `drawer_chevron.svg`, `drawer_heart.svg`, `drawer_settings.svg` |
| `gesture/` | `swipe_up.svg` |
| `menu/` | `about.svg`, `contacts.svg`, `support.svg` |
| `quote/` | `mountain.svg`, `saved.svg`, `share.svg`, `widget.svg` |
| `settings/` | `sound.svg`, `toggle_on.svg`, `vibration.svg` |

`contacts/mail.svg` and `menu/contacts.svg` are byte-identical (545 bytes each). These originals can be retained for design maintenance or moved to a clearly documented design-source folder. Removing them would not reduce the packaged app because this directory is not packaged.

## Template tests worth reviewing

- `app/src/test/java/ru/missclick/wolfwisdom/ExampleUnitTest.kt`: only checks `2 + 2 == 4`; reasonable to remove.
- `app/src/androidTest/java/ru/missclick/wolfwisdom/ExampleInstrumentedTest.kt`: checks the application package name; optional to keep as an installation smoke test.

These tests are discovered by their test runners; absence of source references does not make them unused. No whole production Kotlin file was identified as an unused-file candidate by this audit.

## Assets that should remain

- All 21 PNGs and 10 WebPs have Kotlin or XML references. No byte-identical duplicates were found among these images. Launcher density/configuration alternatives are intentional resource variants.
- All three TTF fonts and the WAV sound have resource references.
- All eight files in `assets/` have explicit runtime paths: quote seed data, legal documents, third-party notices, and font licenses.
- Room schemas 1–5 are configured as instrumentation-test assets; older versions support migration verification.
- `WolfWisdomApplication` and debug `WidgetDebugReceiver` are manifest entry points.
- No unused library/plugin alias was found in the version catalog.

## Existing generated data: largest potential disk cleanup

Sizes below are sums of file lengths, not physical allocated disk usage. AVD sparse disk images can occupy a different amount on disk. These are existing files, not created by this audit.

| Location | Size | Assessment |
| --- | ---: | --- |
| `build/` total | 11.04 GiB | Old QA environments, screenshots, reports, tools, logs, and APKs; inspect selectively. |
| `build/qa-avd/` | 5.77 GiB | Test AVD, including a 3,072 MiB saved RAM snapshot and a 2,739 MiB writable disk image. |
| `build/drawer-tools/` | 4.54 GiB | Another test AVD plus tools/logs; includes a 2,048 MiB RAM snapshot and a 2,247 MiB writable disk image. |
| `build/qa-android-home/` | 269.43 MiB | Almost entirely an old emulator log. |
| `.gradle-user-home/` | 9.43 GiB | Reusable Gradle cache; do not classify as unused merely because of size. |
| `app/build/` | 0.57 GiB | Generated build outputs; preserve needed release artifacts, mappings, and reports before cleaning. |
| `artifacts/cleanup-2026-10-27/` | 75.26 MiB | Previously preserved artifacts; retain pending review. |

Subdirectories in this table are included in their parent totals; do not add them twice.

Five existing logs exceed the project's 10 MiB temporary-log limit:

| Log | Size |
| --- | ---: |
| `build/qa-android-home/emulator-out.log` | 269.32 MiB |
| `build/snackbar-emulator.log` | 237.13 MiB |
| `build/drawer-tools/emulator3.log` | 100.04 MiB |
| `build/drawer-tools/emulator.log` | 98.09 MiB |
| `build/drawer-tools/emulator2.log` | 59.68 MiB |

Their last-write times are October 22–26. Process inspection was unavailable in this environment, so this audit does not certify that either AVD is inactive. Before deleting AVD data or logs, establish the owning process state and verify absolute paths per `AGENTS.md`. No existing process was stopped.

## Existing worktree state

The audit began with an existing staged-added/working-tree-deleted entry for `app/src/main/res/drawable/Moonlit wolf · clean background.svg`. The file is absent from the working tree and was not changed by this audit. Resolve that Git state separately from unused-resource cleanup.
