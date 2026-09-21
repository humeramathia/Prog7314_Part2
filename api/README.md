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

For a first run without Firebase, keep `SKIP_AUTH=true` in `.env`. Hosted Render uses `SKIP_AUTH=false`. After you push `Humera`, connect the repo in Render (`render.yaml`) and set `COSMOS_*` plus `FIREBASE_SERVICE_ACCOUNT` in the dashboard. Health: `https://sportsphere-st10276384.onrender.com/health`.

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

## Performance body

`POST /api/performance` stores sport-specific metrics, not a generic score.

```json
{
  "sportId": "basketball",
  "recordedAt": 1789228800000,
  "notes": "Home fixture",
  "metrics": { "points": 22, "rebounds": 8 }
}
```

Swimming example: `{ "distance": 1500, "time": 1260 }` (metres and seconds).

`GET /api/performance/monthly?sportId=basketball&year=2026&month=9&metric=points` returns that month’s sessions plotted by date. `month` is 1-12. `metric` defaults to the sport’s first field.

## Event types

`type` must be `PRACTICE`, `SOCIAL_EVENT`, or `ANNOUNCEMENT`. Legacy `EVENT` is stored as `SOCIAL_EVENT`. Optional fields: `endsAt` (epoch millis), `description`.
