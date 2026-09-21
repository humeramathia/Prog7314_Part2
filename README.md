# Prog7314_Part2

SportSphere — a native Android app for PROG7314 Part 2, backed by a self-hosted REST API (`api/`) on Azure with Cosmos DB.

## Run locally

**API** (keep this running while you use the emulator):

```bash
cd api
copy .env.example .env
npm install
npm test
npm start
```

`GET http://localhost:3000/health` should return `{ "ok": true, "version": "1.0.0" }`.

For a first local run, keep `SKIP_AUTH=true` in `api/.env`.

**Hosted POE API (Render):** `https://sportsphere-st10276384.onrender.com/`

1. Push the `Humera` branch to GitHub.
2. Sign in at [https://dashboard.render.com](https://dashboard.render.com) with GitHub (not the campus Azure login).
3. New → Blueprint → this repo → `render.yaml`.
4. When Render asks for secret env vars, paste from your local `api/.env` (do not commit them):
   - `COSMOS_ENDPOINT`
   - `COSMOS_KEY`
   - `FIREBASE_SERVICE_ACCOUNT` (Firebase Console → Project settings → Service accounts → Generate new private key; paste the JSON as one line)
5. After deploy, open `https://sportsphere-st10276384.onrender.com/health`. You want `"ok": true` and `"skipAuth": false`.
6. Free Render apps sleep when idle. Open `/health` once before a demo so it wakes up.

Debug (emulator) still uses `http://10.0.2.2:3000/`. Release builds use the Render URL.

**Android**

1. Put your Firebase `google-services.json` in `app/` (gitignored).
2. Keep the API running (`npm start` in `api/`) when you test on the emulator.
3. Run the `app` configuration from Android Studio.

## Getting your own branch up to date

If you already cloned the repo before your branch existed, pull the latest `main` and branch from it:

```bash
git checkout main
git pull origin main
git checkout -b <your-branch-name>
git push -u origin <your-branch-name>
```

If your branch already exists on GitHub, fetch and check it out instead:

```bash
git fetch origin
git checkout <your-branch-name>
git merge origin/main
```

## Auth and API client

`data/remote/ApiClient.kt` attaches a Firebase ID token through `FirebaseAuthInterceptor` on every Retrofit call. The API verifies that token unless `SKIP_AUTH=true` in `api/.env`. Settings logout calls `FirebaseAuth.signOut()` as well as clearing local session prefs.

## Learn

`ui/learn/LearnFragment` and `ui/learn/LearnDetailFragment` implement the Learn tab:

- Four category chips (Rules / Techniques / Training / Safety) plus "All", amber when selected.
- Sport-filtered list from `GET /api/learn?sportId=&category=`.
- Detail from `GET /api/learn/:guideId`, with optional image or "Open video" when `mediaUrl` is set.
- Loading, empty, and error-with-Retry states.

`api/src/data/catalog.js` seeds a `mediaUrl` on Techniques (image) and Safety (video) guides. Run `npm run seed` in `api/` against Cosmos after changing seed data.

## Calendar

Events load from `GET /api/events?sportId=`. Types are `PRACTICE`, `SOCIAL_EVENT`, and `ANNOUNCEMENT`. Detail shows start, optional end, venue, description, notes, and an optional add-to-phone-calendar action.

## Tests

Unit tests for Learn filtering live in `app/src/test/java/.../ui/learn/LearnSupportTest.kt`. `.github/workflows/android.yml` runs them (and a debug build) on pushes/PRs that touch `app/**`. API tests run from `.github/workflows/api.yml`.
