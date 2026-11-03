# Mo: Part 3 implementation and verification

## Offline performance sessions

`AddPerformanceFragment` writes through `PerformanceRepository` into Room before any network call. History and the Home latest-session tile observe Room, so pending sessions appear immediately and remain after an app/process restart. The existing local `SportMetrics` schema makes recording possible without fetching metrics first.

Each session has a local UUID, Firebase owner UID, sport, timestamp, metrics, notes and pending/synced/failed state. WorkManager uses network-connected constraints, unique per-user work, exponential backoff and a 15-minute periodic backup. Authentication changes stop the previous user's jobs and schedule the signed-in user's queue. Every Room read and mutation includes the owner UID. Requests use the token captured from that owner, never a different account's token.

The existing `POST /api/performance` now accepts an optional `clientId` UUID. A deterministic owner-scoped server ID and atomic create prevent duplicated records after a lost response or simultaneous retry. An identical retry returns the existing document (200); a new document returns 201; changed input with an already used UUID returns 409. Existing clients without clientId still work. Deploy this API change before testing sync. `/health` advertises `capabilities.performanceClientId`; the worker waits rather than uploading to an old server without retry protection.

Successful uploads become synced; input rejections become failed and remain visible for review. Timeouts, authentication failures, throttling and server failures keep their pending data. A failed row can be deleted and recorded again. Do not delete pending rows until the upload finishes; a response could have been lost after the server accepted them. Synced rows are deleted on the server before removal from Room, so deleting them requires connectivity.

Online refresh atomically replaces only synced rows for the selected user/sport and preserves pending/failed data. It skips a remote copy whose clientId is still pending locally. Server values are authoritative for synced rows. There is no offline editing or offline server deletion, and therefore no last-write-wins edit conflict policy in this scope. Calendar and Learn remain online. Monthly graph continues to use the API and includes sessions after upload.

The Performance screen shows an offline banner, pending/rejected status, refresh feedback and retry. Settings offers Sync performance now and the last successful sync timestamp. New UI text is in `values/strings.xml`; Lilitha must translate the new `performance_*` keys into Afrikaans and isiZulu.

## Integration

- Imraan: consume `app.performance.observe(uid, sportId)`, `save(uid, request)` and `delete(uid, localId)`; do not restore direct POST calls in Add.
- API owner: deploy the included route/store changes, with production Firebase verification and Cosmos configured. Check the new health capability. No new Cosmos container is needed.
- Humera: use `docs/play-store/README.md` for the final package, Firebase registrations and release certificate fingerprints.
- Lilitha: translate new performance strings and keep both Android/API CI green.

## Device verification still required

1. Sign in, choose swimming, open Performance online, and confirm cloud history appears.
2. Enable airplane mode, record a session and confirm Pending in History and the Home tile.
3. Fully close/reopen the app while offline; confirm the session remains.
4. Reconnect, wait for WorkManager (or use Sync performance now), confirm Pending disappears and exactly one Cosmos record exists.
5. Reopen/retry sync; confirm no duplicate. Check the monthly graph after upload.
6. Repeat with basketball; verify sport-specific metrics and newest-first history.
7. Sign out of A, sign into B, confirm A's sessions are hidden. Return to A and confirm its pending queue resumes.
8. With the API temporarily unavailable, confirm saved sessions remain and retries eventually succeed. A permanent input rejection must be visible without a crash.
9. Delete an acknowledged session online and confirm it disappears from cloud/history. Offline delete must keep the row and show an error.
10. Test the signed, minified release on a real phone, including Google login. Film offline creation, reconnection, the actual Cosmos record and the signed build/store materials.

## Automated checks

API tests cover identical and concurrent retries, UUID validation, changed-payload rejection, user isolation and legacy requests. Android JVM tests cover durable-row mapping, stable queued request IDs and permanent/transient error policy. Run `npm test` in `api`, then `./gradlew testDebugUnitTest assembleDebug` at the project root. Android tests must pass on CI/a configured Android SDK before integration. Device verification above remains necessary.

## Verification on 27 October 2026

- API: 12 suites, 37 tests passed.
- Android: `testDebugUnitTest` passed (37 tests, including 6 new queue/mapping/policy tests).
- Android: `assembleDebug` passed; Room generated its version-1 schema successfully.
- XML parsing and newly introduced string-resource references passed; `git diff --check` was clean.
- Android compilation used the same non-production Firebase placeholder as CI. It proves the code builds, not that real Firebase authentication/device sync works. No release or physical-device validation is claimed.

## Release status

Release signing scaffolding, HTTPS-only release networking, hosted API defaults and store copy are prepared. No real signing key, final Firebase package configuration, signed production APK/AAB, device screenshots, feature graphic, hosted privacy policy, completed content-rating/data-safety forms or physical-device evidence is provided by this change. Do not describe Play Store readiness as finished yet.

## Implementation references

- Android Developers, Room: https://developer.android.com/training/data-storage/room
- Android Developers, WorkManager unique work: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work
- Android Developers, signing: https://developer.android.com/studio/publish/app-signing
- Android Developers, AGP built-in Kotlin and KSP: https://developer.android.com/build/migrate-to-built-in-kotlin
- KSP 2.3.4 release: https://github.com/google/ksp/releases/tag/2.3.4

AI assistance: OpenAI Codex helped implement this branch, review retry/account-isolation behaviour and prepare tests/documentation. The group must include this honestly in its final AI declaration and verify the device behaviour before claiming completion.
