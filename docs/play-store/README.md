# Play Store readiness — completion guide

This folder contains preparation, not evidence that a production release is complete. No Play developer account or actual publication is required by the lecturer's tips.

## 1. Coordinate the final application ID

The Play identifier is `za.co.sportsphere.app`. It is set in `gradle.properties` as `sportsphere.applicationId`. Firebase already has that Android app; `app/google-services.json` is committed and contains both this package and the older `com.example.prog7314_part2` client.

Set the deployed API's `ANDROID_APP_PACKAGE` environment variable to the same package so its verification-email fallback uses the correct Android application. The Android email flow reads `BuildConfig.APPLICATION_ID` automatically.

Build with `-Psportsphere.applicationId=YOUR_FINAL_PACKAGE` or persist `sportsphere.applicationId=YOUR_FINAL_PACKAGE` in your user `~/.gradle/gradle.properties`, the command line, or the project's `gradle.properties` after group agreement. The source namespace can remain `com.example.prog7314_part2`; the Play identifier is `applicationId`. Renaming all Kotlin packages is unnecessary.

## 2. Create and keep the release key privately

Use Android Studio: Build > Generate Signed App Bundle or APK > create a new keystore outside this repository. Keep the keystore, alias and passwords in a safe location under the group's control; losing them can prevent future updates.

Copy `keystore.properties.example` to `keystore.properties` and fill in the private values. The build reads this ignored file; there is no fallback to a debug key. The key and passwords must not be committed or attached to a GitHub Release.

The local upload keystore lives outside Git at `C:/Users/Humera Mathia/keys/sportsphere-upload.jks`. Register these fingerprints on the Firebase Android app `za.co.sportsphere.app` so Google sign-in works on the minified release:

```text
SHA-1:   8F:DB:6D:6E:59:EE:02:03:FD:27:A7:75:4E:88:92:76:82:D2:C2:BE
SHA-256: 0E:15:77:19:F5:2C:79:29:35:E7:0B:66:A2:6E:4E:D2:2C:5B:51:15:5A:F1:EB:7C:E8:07:E0:7C:B1:35:DF:E6
```

If Play App Signing is later used, register its app-signing certificate as well. Never commit `keystore.properties` or the `.jks`.

## 3. Build both release artifacts

After providing the matching Firebase configuration and private signing values:

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease bundleRelease -Psportsphere.applicationId=YOUR_FINAL_PACKAGE
```

The release is minified and uses HTTPS only. Verify the APK signature using Android SDK `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk`. Confirm it is the intended certificate, then install and test the release on a physical phone. A package-ID change installs as a separate app; do not uninstall an old build with unsynced sessions until you have uploaded them.

Expected outputs:
- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## 4. Complete this folder with real evidence

- `icon-512.png`: final 512 x 512 PNG app icon from Humera.
- `feature-graphic-1024x500.png`: final feature graphic.
- `screenshots/`: at least four real phone screenshots, including the completed app with real data. Suggested screens: Login, Home, Calendar, Performance, Graph, Learn, Settings. Include dark mode where helpful.
- `store-listing.md`: review the prepared copy and remove features that have not been verified in the final build.
- Privacy policy: prepare and host an accurate policy with the group's actual contact details and data-handling practices, then put its working public URL into the listing. No fictitious URL is supplied.
- Content rating: complete the questionnaire answers based on the finished app, including any user-created content.
- Data safety: document account data, performance information, Firebase/FCM identifiers, cloud storage, deletion, sharing and security according to the actual deployed implementation. The answers require the whole group's review.

## 5. Final release

Deploy the retry-safe API, pass Android/API CI, run the physical-device walkthrough in `../PART3-MO.md`, and complete the listing evidence. Tag the verified final commit `v1.0.0-final-poe`, create the GitHub Release and attach the signed APK and AAB. Add the actual release link, screenshot gallery and unlisted narrated video URL to the main README. Do not tag this preparatory branch as the finished POE.
