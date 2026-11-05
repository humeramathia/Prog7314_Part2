# Play Console Data safety — SportSphere

Declare **this app does not use advertising ID**. The release manifest removes `com.google.android.gms.permission.AD_ID`.

## Data collected

Collected **and required** for the app to work. Encrypted in transit. Users can request deletion (Settings → Delete account).

| Play category | Data types | Purpose | Shared with other companies? |
| --- | --- | --- | --- |
| Personal info | Email address, name | App functionality (account) | No (Google Firebase is a service provider) |
| App activity | In-app actions: chosen sport, fixtures, performance sessions | App functionality | No |
| App info and performance | Crash logs are **not** collected (no Crashlytics) | — | — |
| Device or other IDs | FCM registration token | App functionality (notifications) | No |

Not collected: location, photos, contacts, financial info, health, browsing history, advertising ID.

## Account and deletion

- Account required: **Yes**
- Users can create an account: **Yes** (email or Google)
- Users can request data deletion: **Yes** — Settings → Delete account, which calls `DELETE /api/me` and removes the Firebase user
- Data is encrypted in transit: **Yes** (HTTPS only on release)
- Data is encrypted at rest: rely on Firebase / Azure platform defaults; do not claim a custom at-rest scheme

## Independent security review

No. Leave that box unticked.

## Advertising / selling

- App does not contain ads
- App does not sell data
- App does not use advertising ID
