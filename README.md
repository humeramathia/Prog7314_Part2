# SportSphere

Native **Kotlin** Android app for PROG7314 / OPSC7312 Part 2. Athletes pick one sport, then use Home, Calendar, Performance (monthly graph), Learn, and Settings. The app talks to **our** hosted REST API (not a public sports API). Data lives in **Azure Cosmos DB**. Sign-in is **Firebase Auth** (email/password + Google).

| | |
| --- | --- |
| App package | `com.example.prog7314_part2` |
| minSdk | 24 |
| API (hosted) | https://sportsphere-st10276384.onrender.com/ |
| Health check | https://sportsphere-st10276384.onrender.com/health |
| GitHub | https://github.com/humeramathia/Prog7314_Part2 |

---

## 1. Physical Android device (POE)

This is the build a marker / teammate should run on a **real phone**. Debug builds still point at the emulator (`http://10.0.2.2:3000/`). A phone cannot reach that address, so **do not Run the debug variant on a device**.

The release variant is already wired to the public HTTPS API, is allowed to use the internet, and is signed so the APK can be installed.

### 1.1 Why it works on a phone (this is the code)

**1. Release builds use the hosted HTTPS API, not localhost**

`app/build.gradle.kts` writes a different `BuildConfig.API_BASE_URL` per variant:

```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/\"")
}
release {
    isMinifyEnabled = false
    signingConfig = signingConfigs.getByName("debug")
    buildConfigField("String", "API_BASE_URL", "\"https://sportsphere-st10276384.onrender.com/\"")
}
```

`10.0.2.2` is only the emulator’s alias for the PC. A physical phone uses the **release** URL, which is public HTTPS on Render.

**2. Retrofit actually uses that URL**

```kotlin
// app/src/main/java/.../data/remote/ApiClient.kt
Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .client(httpClient)
```

If you install the **release** APK, every call goes to Render. You do not type a URL in the UI.

**3. The phone is allowed to use the internet**

```xml
<!-- app/src/main/AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
```

**4. HTTPS is allowed in release; HTTP is only for debug/emulator**

```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <debug-overrides>
        <base-config cleartextTrafficPermitted="true" />
    </debug-overrides>
</network-security-config>
```

Release → HTTPS to Render. Debug → HTTP to `10.0.2.2` is still allowed for the emulator.

**5. The hosted API listens on all interfaces and requires Firebase tokens**

```js
// api/src/server.js
const host = process.env.HOST || "0.0.0.0";
app.listen(config.port, host, () => { ... });
```

```yaml
# render.yaml (production)
SKIP_AUTH: "false"
```

Render injects Cosmos + Firebase Admin secrets. After login, the app attaches the ID token:

```kotlin
// FirebaseAuthInterceptor.kt
.header("Authorization", "Bearer $token")
```

**6. The release APK is signed so Android will install it**

```kotlin
signingConfig = signingConfigs.getByName("debug")
```

That uses the Android debug keystore (fine for a class demo / POE, not Play Store). Without this line, `assembleRelease` produces an unsigned APK and a phone will refuse the install.

### 1.2 Before you plug in the phone

1. Confirm the API is awake (free Render apps sleep). Open this in a browser:

   https://sportsphere-st10276384.onrender.com/health

   You want:

   ```json
   {
     "ok": true,
     "service": "sportsphere-api",
     "version": "1.0.0",
     "store": "cosmos",
     "skipAuth": false,
     "firebaseAdmin": true
   }
   ```

2. Put Firebase `google-services.json` in `app/` (gitignored). Package name must be `com.example.prog7314_part2`. Project id: `sportsphere-st10276384`.

3. USB: enable **Developer options** → **USB debugging**. Accept the RSA prompt on the phone.

### 1.3 Android Studio (easiest)

1. Open this repo as an Android Studio project.
2. **Build → Select Build Variant…** → `app` → **release**.
3. Toolbar device dropdown → your physical phone (not an emulator).
4. Run (green triangle).

The phone installs SportSphere and talks to `https://sportsphere-st10276384.onrender.com/`.

Sign in with an account from **this** Firebase project (`sportsphere-st10276384`). Older accounts from a different Firebase project will fail.

### 1.4 Command line (same APK)

From the repo root (Windows PowerShell):

```powershell
# Wake the API
curl https://sportsphere-st10276384.onrender.com/health

# Signed release APK that points at Render
.\gradlew.bat assembleRelease

# Install on the USB phone
adb devices
adb install -r app\build\outputs\apk\release\app-release.apk
```

On macOS / Linux:

```bash
curl https://sportsphere-st10276384.onrender.com/health
./gradlew assembleRelease
adb devices
adb install -r app/build/outputs/apk/release/app-release.apk
```

APK path: `app/build/outputs/apk/release/app-release.apk`.

### 1.5 What you should see on the phone

Welcome → Register or Google sign-in → confirm email if asked → choose a sport → **Home / Calendar / Performance / Learn** bottom nav. Settings is the gear on Home. Performance uses sport-specific metrics (not a generic score) and a monthly graph. Calendar types are `PRACTICE`, `SOCIAL_EVENT`, `ANNOUNCEMENT`. Learn has Rules / Techniques / Training / Safety.

If a list is empty, the API may still be waking. Open `/health` again, then tap **Retry**.

> Do **not** run the **debug** variant on a physical phone. Debug is `http://10.0.2.2:3000/` (emulator only). Use **release** for a device.

---

## 2. Folder structure

```
Prog7314_Part2/
├── README.md                          ← this file
├── render.yaml                        ← Render Blueprint (hosts api/)
├── firebase.json / .firebaserc        ← Firebase project sportsphere-st10276384
├── .github/workflows/
│   ├── android.yml                    ← unit tests + debug APK
│   └── api.yml                        ← Jest API tests
│
├── app/                               ← Android (Kotlin)
│   ├── google-services.json           ← local only (gitignored)
│   ├── build.gradle.kts               ← minSdk 24, debug/release API URLs
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    ← INTERNET + network security
│       │   ├── java/com/example/prog7314_part2/
│       │   │   ├── MainActivity.kt
│       │   │   ├── SportSphereApp.kt
│       │   │   ├── data/              ← models, metrics, FakeRepository fallback
│       │   │   ├── data/remote/       ← Retrofit, interceptor, mappers
│       │   │   └── ui/
│       │   │       ├── auth/          ← Welcome, Login, Register, Confirm, Sport
│       │   │       ├── home/
│       │   │       ├── calendar/
│       │   │       ├── performance/   ← sessions + monthly graph
│       │   │       ├── learn/
│       │   │       └── settings/
│       │   └── res/
│       │       ├── layout/            ← fragments + list rows
│       │       ├── navigation/nav_graph.xml
│       │       ├── menu/menu_bottom_nav.xml
│       │       └── xml/network_security_config.xml
│       └── test/                      ← JUnit (metrics, mappers, Learn, FakeRepository)
│
└── api/                               ← Express REST API (Node 20)
    ├── .env / .env.example            ← .env is gitignored
    ├── package.json
    ├── Procfile / .nvmrc
    ├── src/
    │   ├── server.js                  ← listen 0.0.0.0
    │   ├── app.js                     ← /health + /api/*
    │   ├── firebase.js               ← Admin SDK token verify
    │   ├── routes/                    ← me, sports, events, performance, learn
    │   ├── data/                      ← catalog seed + sport metrics
    │   └── store/                     ← Cosmos (prod) / memory (local fallback)
    └── tests/                         ← Jest + SuperTest
```

Secrets that must **never** be committed: `api/.env`, `app/google-services.json`, `api/firebase-service-account.json`.

---

## 3. Architecture (short)

```
Physical phone (release APK)
    → HTTPS https://sportsphere-st10276384.onrender.com/
        → Express (api/)
            → Firebase Admin verifies Bearer token
            → Azure Cosmos DB (database sportsphere)
```

Emulator (debug APK) → `http://10.0.2.2:3000/` → API on your PC (`npm start` in `api/`).

Screens: single `MainActivity` + Navigation Component fragments. ViewBinding. Bottom nav: Home, Calendar, Performance, Learn.

---

## 4. Run locally (emulator + API on this PC)

Use this when you **do not** have a phone. The emulator can reach your PC at `10.0.2.2`.

```bash
cd api
copy .env.example .env
npm install
npm test
npm start
```

`GET http://localhost:3000/health` should include `"ok": true`. For a first local run, `SKIP_AUTH=true` in `api/.env` is fine. **Never** set that on Render.

Android Studio: leave the build variant on **debug**, put `google-services.json` in `app/`, Run on an emulator.

---

## 5. Hosted API (already deployed)

Live URL: https://sportsphere-st10276384.onrender.com/

`render.yaml` builds `api/` on Node 20, `SKIP_AUTH=false`, health path `/health`. Dashboard secrets (not in git): `COSMOS_ENDPOINT`, `COSMOS_KEY`, `FIREBASE_SERVICE_ACCOUNT`.

Authenticated routes (`Authorization: Bearer <Firebase ID token>`):

- `GET/PATCH /api/me`
- `GET /api/sports`
- `GET/POST /api/events`, `GET /api/events/next`, `GET /api/events/:eventId`
- `GET/POST/DELETE /api/performance`, `GET /api/performance/monthly`
- `GET /api/learn`, `GET /api/learn/:guideId`

`POST /api/performance` stores sport-specific metrics, not a generic `score`:

```json
{
  "sportId": "basketball",
  "recordedAt": 1789228800000,
  "notes": "Home fixture",
  "metrics": { "points": 22, "rebounds": 8 }
}
```

---

## 6. Features

**Auth / Settings** — Email/password + Google (Credential Manager). Logout calls `FirebaseAuth.signOut()` and clears local session. Dark mode is PATCHed to `/api/me`.

**Calendar** — `GET /api/events?sportId=`. Types: `PRACTICE`, `SOCIAL_EVENT`, `ANNOUNCEMENT`. Detail can add the event to the phone calendar.

**Performance** — Sport fields (e.g. basketball points/rebounds, swimming distance/time). Monthly graph: `GET /api/performance/monthly?sportId=&year=&month=`.

**Learn** — Chips Rules / Techniques / Training / Safety. `GET /api/learn?sportId=&category=`. Detail can show image or open video from `mediaUrl`.

Sport list on a failed network falls back to `FakeRepository` (six sports) so Sport Select is not a blank screen.

---

## 7. Tests and GitHub Actions

```bash
cd api && npm test
```

Android: `.github/workflows/android.yml` runs `./gradlew testDebugUnitTest` and `assembleDebug`. API: `.github/workflows/api.yml` runs Jest with `SKIP_AUTH=true`.

---

## 8. Team branches

```bash
git checkout main
git pull origin main
git checkout -b <your-branch-name>
git push -u origin <your-branch-name>
```
