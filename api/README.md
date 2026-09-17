# SportSphere REST API

Express API for PROG7314 Part 2. The Android app sends a Firebase ID token as `Authorization: Bearer <token>`. Data is stored in Azure Cosmos DB (`sportsphere`). If `COSMOS_KEY` is not set, the API uses in-memory data so you can develop offline.

## Run locally

```
cd api
copy .env.example .env
npm install
npm test
npm run seed
npm start
```

`GET http://localhost:3000/health` should return `{ "ok": true }`.

For a first run without Firebase, keep `SKIP_AUTH=true` in `.env`. For the POE demo, set `SKIP_AUTH=false`, add `firebase-service-account.json`, and send a real ID token from the app.

## Endpoints

Public:

- `GET /health`

Authenticated (`Authorization: Bearer <Firebase ID token>`):

- `GET /api/me`
- `PATCH /api/me`
- `GET /api/sports`
- `GET /api/sports/:sportId`
- `GET /api/events`
- `GET /api/events/next`
- `GET /api/events/:eventId`
- `POST /api/events`
- `PUT /api/events/:eventId`
- `DELETE /api/events/:eventId`
- `GET /api/performance`
- `GET /api/performance/monthly`
- `GET /api/performance/:sessionId`
- `POST /api/performance`
- `DELETE /api/performance/:sessionId`
- `GET /api/learn`
- `GET /api/learn/:guideId`
