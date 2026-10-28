# About and legal documents

Implemented from the local `about-app-handoff` package supplied on 28 October 2026.
The four references are 390×844, 844×390, 834×1112 and 1112×834. Portrait uses
a centered column; landscape uses three columns when at least 700 dp is available.
Narrow landscape windows fall back to a scrollable column. The About and document
headers expand with text size; other service-screen headers retain their existing behavior.

The shared service-screen backgrounds, glass cards, typography and Back button
are reused. Four legal-row PNG assets are copied unchanged from the handoff.
The logo renders `R.mipmap.ic_launcher`, including its adaptive layers and mask,
instead of the handoff logo or the currently active seasonal launcher alias.
The displayed version comes from `BuildConfig.VERSION_NAME`.

Navigation is Main → About → Licences / Privacy policy / Terms. Navigation visits
and document button clicks use the existing consent-gated analytics. System Back
is handled by Navigation and does not emit an explicit Back-button event.

## Documents

`app/src/main/assets/legal/` contains Russian documents available offline, with
the developer identity and contact supplied by the owner. English UI shows an
explicit note that legal texts are Russian. Long documents use a lazy list,
selectable text, active web/email links, a contact action and loading/retry states.
Full font license texts are loaded from the existing `assets/font_licenses/`.

Privacy text reflects release-only opt-in AppMetrica, SDK queue/revocation
limitations, local database/preferences, Android backup, sharing and external
services. It does not claim that all data remains on the device or promise
automatic deletion exactly 24 hours after sharing. Terms describe the current
free version without ads. No age rating or quote edits are introduced.

Before store publication, publish the same privacy policy at a public URL and
verify the actual AppMetrica retention/settings against the policy. This task
does not configure the AppMetrica account or publish a site. Font/library notices
do not establish ownership of third-party quotes.

## Library notices

The checked-in `third_party_notices.txt` covers 147 distinct resolved runtime
coordinates from the release configuration, including transitive dependencies.
It combines POM license metadata and deduplicated embedded license/notice texts
from the actual JAR/AAR archives. AppMetrica MIT copyright/license is from:
https://raw.githubusercontent.com/appmetrica/appmetrica-sdk-android/master/LICENSE

After updating dependencies, check the packaged notices against the new resolved
release dependencies. The one-time export scripts and temporary inventory were
removed after implementation. The packaged notice has no cache paths.

## Verification (28 October 2026)

- Debug APK assembled; all 82 existing JVM tests passed.
- Release Kotlin compiled; new Android UI tests compiled and their APK assembled.
- `AboutScreenTest` covers the four reference sizes, 200% text, a 640×320 short
  window, document-button dispatch, offline document loading, end-of-document
  reachability and visible Back buttons.
- UI execution and rendered visual comparison are **not verified**. The single
  attempt to start the existing QA AVD failed on access to
  `C:\Users\missclick\.android\emu-last-feature-flags.protobuf.lock`.
  The created emulator process (PID 27336) was stopped and absence of both it and
  QEMU was checked. No snapshots or redirected emulator log files were created;
  its output was held in a bounded memory buffer. No additional AVD was created.
