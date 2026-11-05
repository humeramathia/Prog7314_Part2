# Part 3: Push notifications, multi-language support and logging

Owner: Lilitha Mlobi. Branches, which merge in this order: `feature/logging` → `feature/notifications` → `feature/languages`.

This page is written so its sections can be copied straight into the main README (features, release notes and references).

---

## 1. Real-time push notifications (Firebase Cloud Messaging)

### What the user sees

- The first time a signed-in user reaches **Home** on Android 13 or newer, SportSphere asks for permission to show notifications. It asks only once, after the user knows what the app does.
- When anyone adds or edits a practice, event or announcement for a sport, every phone following that sport gets an alert, for example: *Archery: Practice added · Range practice — Thu 5 Nov, 16:00 at Main field*.
- **Settings → Notifications** has:
  - a **Fixture alerts** switch, saved on the phone and on the user's profile,
  - a status line in plain words (on, off, or "Blocked by Android. Tap here…", which opens the system notification settings),
  - a **Send test notification** button that asks the API to push to this phone, which proves the whole round trip in the demo.
- Alerts show while the app is open as well as when it is closed.

### How it works

```text
Android app                         SportSphere API (Render)             Firebase
───────────                         ────────────────────────             ────────
PushRegistrar.sync()
  ├─ subscribeToTopic("sport_archery") ───────────────────────────────▶  topic list
  └─ PATCH /api/me { fcmToken } ─────▶ saves token on user (Cosmos DB)

Add fixture  ─ POST /api/events ────▶ saves event
                                      notifyFixture() ─ send(topic) ──▶  fans out to
                                                                         every subscriber
SportSphereMessagingService ◀─────────────────────────────────────────── message
  └─ shows notification on the "fixtures" channel

Settings → Send test ─ POST /api/me/test-notification ─ send(token) ─▶  this phone only
```

- **One topic per sport** (`sport_<sportId>`): the API sends one message and Firebase delivers it to every phone, so the server never loops over users. The topic name rule is the same on both sides, and both test suites check the same example.
- **Token per user**: stored on the user document so the API can message one phone directly. The token is never returned in API responses; the app only sees `hasDeviceToken`.
- **Sending never blocks saving.** A fixture is saved even if Firebase is slow or down; push errors are only logged.
- **Logout** unsubscribes the topic and deletes the FCM token, so the next account on the phone never receives the previous account's alerts.

### Files

| Layer | File | Purpose |
| --- | --- | --- |
| API | `api/src/notify.js` | Topic names, message text, FCM and in-memory notifiers |
| API | `api/src/routes/deviceSettings.js` | Validates `fcmToken`, `notificationsEnabled`, `language` |
| API | `api/src/routes/me.js` | Saves device settings; `POST /api/me/test-notification` |
| API | `api/src/routes/events.js` | Pushes on fixture create and update |
| Android | `notifications/PushRegistrar.kt` | Keeps topic and token in step with the signed-in user |
| Android | `notifications/SportSphereMessagingService.kt` | Shows foreground pushes, handles new tokens |
| Android | `notifications/NotificationChannels.kt` | Creates the `fixtures` channel, checks permission |
| Android | `notifications/NotificationPermissionPrompt.kt` | One-time permission prompt on Home |
| Android | `ui/settings/NotificationSettingsController.kt` | Settings card logic |

### New API endpoints and fields

| Method | Path | Body / result |
| --- | --- | --- |
| `PATCH` | `/api/me` | Also accepts `fcmToken` (string, `""` removes it), `notificationsEnabled` (bool), `language` (`en`/`af`/`xh`) |
| `GET`/`PATCH` | `/api/me` | Responses now include `language`, `notificationsEnabled`, `hasDeviceToken` |
| `POST` | `/api/me/test-notification` | `200 {ok, messageId}` · `409` no device registered or switched off · `410` token expired · `503` FCM not configured |

### One-time setup (before the demo)

1. **Firebase console → Project settings → Cloud Messaging:** check that *Firebase Cloud Messaging API (V1)* is **Enabled**.
2. The Render service already has `FIREBASE_SERVICE_ACCOUNT`, so no new secret is needed. The new API code goes live when this branch is merged to `main` (Render auto-deploys). Check with `GET /health`, which now shows `"notifier": "fcm"` and `"firebaseAdmin": true`.
3. Install the build on a physical phone and allow notifications when asked.

---

## 2. Multi-language support (English, Afrikaans, isiXhosa)

- **Settings → Language** has a three-way toggle: **English · Afrikaans · isiXhosa**. Each language name is written in its own language, so users can always find theirs.
- Tapping a language switches the whole app at once, with no restart. The screen stays on Settings, and the bottom navigation, toolbar titles, validation messages, graph labels and notification settings all change.
- The choice is kept after the app is closed (AndroidX per-app languages). On Android 13+ it also appears under *Settings → Apps → SportSphere → Language*.
- The choice is also saved on the user's profile (`language` field), so it can be shown changing in Cosmos DB during the demo, and the test notification is sent in that language.
- Club content created by users (fixture titles, Learn guides) stays in the language it was written in. A note on the Settings card says so.

| File | Purpose |
| --- | --- |
| `res/values-af/strings.xml` | 137 Afrikaans strings |
| `res/values-xh/strings.xml` | 137 isiXhosa strings |
| `res/xml/locales_config.xml` + manifest | Declares the three languages; stores the choice on Android 12 and older |
| `ui/settings/AppLanguage.kt` | Supported languages and switching |
| `ui/settings/LanguageSettingsController.kt` | Settings card logic |
| `app/lint.xml` | A string added later without a translation falls back to English as a warning, not a build error |

Hard-coded text in Register, the Home greeting and the monthly graph was moved into `strings.xml` so it translates too.

> **Before submission:** strings added by the biometrics and offline branches are English only until they are added to `values-af` and `values-xh`. They still work, falling back to English.

---

## 3. System logging

- `logging/LifecycleLogger.kt` is registered once in `SportSphereApp` and logs **every activity and fragment lifecycle step**, as well as when the app as a whole moves to the foreground or background. It also tells a configuration change (rotation, theme or language switch) apart from a real close.
- `logging/AppLog.kt` gives every log line a shared `SS.` tag prefix.
- Notifications (`SS.Push`) and language changes (`SS.Language`) log each state change: token upload, topic subscribe/unsubscribe, permission granted/denied, push received, language picked and saved.

**Logcat filter for the demo:** `tag:SS.`, or `tag:SS.Lifecycle`, `tag:SS.Push`, `tag:SS.Language` for one area.

Example output when switching to Afrikaans:

```text
I SS.Language  User picked af
I SS.Language  Switching app language to af
D SS.Lifecycle MainActivity onPause
D SS.Lifecycle MainActivity onDestroy (configuration change)
D SS.Lifecycle MainActivity onCreate (restored)
D SS.Lifecycle   SettingsFragment onViewCreated
I SS.Language  Language af saved on profile (HTTP 200)
```

---

## 4. Tests

| Suite | Count | Covers |
| --- | --- | --- |
| `api/tests/notify.test.js` | 10 | Topic names, message text, FCM channel, failures never throw |
| `api/tests/device-settings.test.js` | 12 | Token stored and hidden, language validation, test push (200/409/503), fixture pushes |
| `app/.../notifications/PushTopicsTest.kt` | 7 | Topic names match the API; subscribe/unsubscribe planning |
| `app/.../ui/settings/AppLanguageTest.kt` | 4 | Language tag mapping; codes match the API |
| `app/.../logging/AppLogTest.kt` | 2 | Tag prefix and Android's 23-character limit |

---

## 5. Demo video script (my sections)

1. **Notifications permission:** sign in on the physical phone, land on Home, accept the permission prompt.
2. **Test push:** Settings → Notifications → *Send test notification*. Pull down the notification shade.
3. **Real-time fixture alert:** with a second phone (or the app in the background), add a fixture on the Calendar. The alert arrives. Show the event in Cosmos DB and the Render log line `[notify] sent … to topic sport_<id>`.
4. **Switch off:** turn *Fixture alerts* off, add another fixture, and show that no alert arrives. Show `notificationsEnabled: false` and the token removed in Cosmos DB.
5. **Languages:** Settings → Language → Afrikaans, then isiXhosa. Walk through Home, Calendar, Performance and Settings. Close and reopen the app to show the choice is kept. Show `language: "xh"` on the user document.
6. **Logging:** show Logcat filtered on `tag:SS.` while doing steps 2 and 5.

---

## 6. Release notes (added since the prototype)

- **New: Real-time push notifications.** Fixture alerts per sport through Firebase Cloud Messaging, a Settings switch, a status line that explains blocked permission, and a test-notification button. The API now pushes on fixture create and edit.
- **New: Multi-language support.** English, Afrikaans and isiXhosa, switchable in Settings without a restart, kept between launches and saved on the user profile.
- **New: Lifecycle and state logging.** Central activity and fragment lifecycle logging plus feature logs under a shared `SS.` Logcat tag.
- **Improved:** Register, Home and the monthly graph no longer have hard-coded English text.
- **API:** `PATCH /api/me` accepts device settings; new `POST /api/me/test-notification`; `/health` reports the notifier.

---

## References

Android Developers, [s.a.c]. *Notification runtime permission*. [online] Available at: https://developer.android.com/develop/ui/views/notifications/notification-permission [Accessed 4 November 2026].

Android Developers, [s.a.d]. *Create and manage notification channels*. [online] Available at: https://developer.android.com/develop/ui/views/notifications/channels [Accessed 4 November 2026].

Android Developers, [s.a.e]. *Per-app language preferences*. [online] Available at: https://developer.android.com/guide/topics/resources/app-languages [Accessed 4 November 2026].

Android Developers, [s.a.f]. *Localize your app*. [online] Available at: https://developer.android.com/guide/topics/resources/localization [Accessed 4 November 2026].

Android Developers, [s.a.g]. *The activity lifecycle*. [online] Available at: https://developer.android.com/guide/components/activities/activity-lifecycle [Accessed 4 November 2026].

Android Developers, [s.a.h]. *Write and view logs with Logcat*. [online] Available at: https://developer.android.com/studio/debug/logcat [Accessed 4 November 2026].

Firebase, [s.a.a]. *Set up a Firebase Cloud Messaging client app on Android*. [online] Available at: https://firebase.google.com/docs/cloud-messaging/android/client [Accessed 4 November 2026].

Firebase, [s.a.b]. *Topic messaging on Android*. [online] Available at: https://firebase.google.com/docs/cloud-messaging/android/topic-messaging [Accessed 4 November 2026].

Firebase, [s.a.c]. *Send messages with the Firebase Admin SDK*. [online] Available at: https://firebase.google.com/docs/cloud-messaging/send-message [Accessed 4 November 2026].

### AI declaration note (for the group write-up)

Claude (Anthropic) was used to help draft the notification, language and logging code, the API tests and this document, and to produce first-draft Afrikaans and isiXhosa translations, which were reviewed by [name] before submission. Commits carry a `Co-Authored-By: Claude` line. *(Fill in who reviewed the translations, or change this sentence, before it goes into the AI write-up.)*
