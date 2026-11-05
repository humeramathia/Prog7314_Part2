# SportSphere privacy policy

Last updated: 5 November 2026

SportSphere is a campus sports-club coursework app for PROG7314. This policy describes what the Android app (`za.co.sportsphere.app`) and its hosted API collect, why they collect it, and how you can delete it.

## Who we are

SportSphere is built by:

- Humera Mathia (ST10276384), group leader
- Mohammed Ba Yazed (ST10174227)
- Imraan Nordien (ST10434419)
- Lilitha Mlobi (ST10454944)

Contact: open an issue on the project repository  
<https://github.com/EMWCCN/prog7314-g1-2026-formative-2-part-2-humeramathia/issues>

## What the app collects

| Data | Where it is stored | Why |
| --- | --- | --- |
| Email address, password (or Google account) | Firebase Authentication | Create and sign in to an account |
| Display name, chosen sport, language, notification switches | Azure Cosmos DB via the SportSphere API | Profile, Home, Settings, and fixture alerts |
| Firebase Cloud Messaging token | Azure Cosmos DB | Send fixture pushes to this phone |
| Performance sessions (metrics, notes, dates) | Room on the phone, then Azure Cosmos DB after upload | History, Home tile, and monthly graph |
| Calendar fixtures you create | Azure Cosmos DB | Club calendar for that sport |
| Biometric unlock switch | DataStore on the phone | Optional fingerprint / face / PIN lock. The app never stores your fingerprint, face, or PIN. |
| App language | Android per-app locale | English, Afrikaans, or isiXhosa UI |

Learn stills are public images on Azure Blob Storage. They are not photos you upload.

## What we do not collect

- Location
- Contacts, photos, microphone, or camera
- Advertising ID
- Payment details
- Precise device identifiers beyond the FCM token needed for pushes

## Who can see your data

Club fixtures for a sport are visible to other signed-in SportSphere users who follow that sport. Performance notes and session metrics belong to your account. They are sent to our API so they can sync across that account; they are not shown as a public feed.

We use these processors to run the app:

- Google Firebase (Authentication and Cloud Messaging)
- Render (API host)
- Microsoft Azure (Cosmos DB and Blob Storage)

We do not sell personal data. We do not use advertising SDKs.

## Security

Production traffic uses HTTPS. Release builds refuse cleartext HTTP. Passwords are handled by Firebase, not stored in SportSphere. Offline performance rows sit in a local Room database that is excluded from Android backup.

## Retention and deletion

Account data stays for as long as the account exists. In the app go to **Settings → Delete account**. That removes the API profile, stored sessions, and the Firebase Auth user. Local Room rows and the biometric switch are cleared on that device when the account is removed.

You can also turn off fixture alerts per type, or disable notifications entirely, in Settings.

## Children

SportSphere is a higher-education coursework app. It is not directed at children under 13.

## Changes

If this policy changes, we will update the date at the top of this page.
