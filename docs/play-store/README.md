# Play Store pack — SportSphere

This folder is the listing evidence for `za.co.sportsphere.app`. It is a signed, minified coursework release pack, not a live Google Play listing.

## Ready in the app

| Item | Status |
| --- | --- |
| Application ID | `za.co.sportsphere.app` in `gradle.properties` |
| Kotlin namespace | still `com.example.prog7314_part2` (Play uses `applicationId`) |
| Firebase clients | `app/google-services.json` includes `za.co.sportsphere.app` |
| Release minify / shrink | on |
| Cleartext HTTP | off on release |
| Advertising ID | permission removed in the manifest |
| Upload keystore | private `.jks` + ignored `keystore.properties` (not in Git) |
| High-res icon | `icon-512.png` (512 × 512) |
| Feature graphic | `feature-graphic-1024x500.png` (1024 × 500) |
| Listing copy | `store-listing.md` |
| Privacy policy | `privacy-policy.md` |
| Content rating answers | `content-rating.md` |
| Data safety answers | `data-safety.md` |
| Phone screenshots | `screenshots/` — welcome, Home, Calendar, Learn, confirm email |

## Build the signed AAB and APK

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease bundleRelease
```

Outputs:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Verify the APK certificate, then install that APK on a phone:

```text
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Expected upload-key SHA-1:

```text
8F:DB:6D:6E:59:EE:02:03:FD:27:A7:75:4E:88:92:76:82:D2:C2:BE
```

Add that SHA-1 (and SHA-256) on the Firebase Android app `za.co.sportsphere.app` if Google sign-in fails on the minified release. If Play App Signing is used later, register Play’s app-signing certificate as well.

Set Render `ANDROID_APP_PACKAGE` to `za.co.sportsphere.app`.

## Still done on the phone / Play Console

1. Paste listing copy, privacy URL, content rating, data safety, and the five phone screenshots into Play Console.
2. Upload the AAB. Keep the `.jks` and `keystore.properties` off Git and off the GitHub Release.
3. Do not tag `v1.0.0-final-poe` until this branch is on `main`.
