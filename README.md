# Prog7314_Part2

SportSphere — a native Android app for PROG7314 Part 2, backed by a self-hosted REST API (`api/`) on Azure with Cosmos DB.

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
git merge origin/main   # bring in the latest API/auth work from main
```

## Learn feature (this branch)

`ui/learn/LearnFragment` and `ui/learn/LearnDetailFragment` implement the Learn tab:

- Four category chips (Rules / Techniques / Training / Safety) plus "All", amber when selected.
- Sport-filtered list loaded from `GET /api/learn?sportId=&category=`.
- Resource detail screen loaded from `GET /api/learn/:guideId`, with an optional image (`mediaUrl` pointing at a still image) or an "Open video" button (`mediaUrl` pointing at a video host/file) when the guide has one.
- Loading, empty ("No guides in this category yet."), and error-with-Retry states, so a failed load never crashes the screen.

The Retrofit client (`data/remote/ApiClient.kt`) does **not** attach a Firebase ID token yet, because `main` doesn't have Firebase Auth wired up — that's on the `Mo` branch. The Learn endpoints are read-only and work fine against a deployed API running with `SKIP_AUTH=true`. Once real auth merges into `main`, add `FirebaseAuthInterceptor` to `ApiClient`'s `OkHttpClient` to match.

`api/src/data/catalog.js` seeds a `mediaUrl` on the Techniques (image) and Safety (video) guides for every sport so both states are exercised. Run `npm run seed` in `api/` against your deployed Cosmos instance to pick this up there.

Unit tests for the pure filtering/formatting logic are in `app/src/test/java/.../ui/learn/LearnSupportTest.kt`. `.github/workflows/android.yml` runs them (and a debug build) on every push/PR touching `app/**`.
