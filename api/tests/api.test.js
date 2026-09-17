process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");

async function testApp() {
  const store = createMemoryStore();
  await seedStore(store);
  return createApp(store);
}

describe("SportSphere API", () => {
  let app;

  beforeEach(async () => {
    app = await testApp();
  });

  test("GET /health", async () => {
    const res = await request(app).get("/health");
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });

  test("GET /api/sports returns six sports", async () => {
    const res = await request(app).get("/api/sports");
    expect(res.status).toBe(200);
    expect(res.body).toHaveLength(6);
    expect(res.body.map((s) => s.id)).toContain("football");
    const basketball = res.body.find((s) => s.id === "basketball");
    expect(basketball.metrics.map((m) => m.key)).toEqual(["points", "rebounds"]);
  });

  test("GET /api/me creates a profile", async () => {
    const res = await request(app).get("/api/me");
    expect(res.status).toBe(200);
    expect(res.body.userId).toBe("dev-user");
    expect(res.body.email).toBe("dev@sportsphere.app");
  });

  test("PATCH /api/me saves selected sport", async () => {
    const res = await request(app).patch("/api/me").send({ sportId: "tennis" });
    expect(res.status).toBe(200);
    expect(res.body.sportId).toBe("tennis");
    expect(res.body.sportName).toBe("Tennis");
  });

  test("GET /api/events requires sportId", async () => {
    const res = await request(app).get("/api/events");
    expect(res.status).toBe(400);
  });

  test("GET /api/events and /next", async () => {
    const list = await request(app).get("/api/events").query({ sportId: "football" });
    expect(list.status).toBe(200);
    expect(list.body.length).toBeGreaterThan(0);

    const next = await request(app).get("/api/events/next").query({ sportId: "football" });
    expect(next.status).toBe(200);
    expect(next.body.sportId).toBe("football");

    const detail = await request(app).get(`/api/events/${next.body.id}`);
    expect(detail.status).toBe(200);
    expect(detail.body.title).toBe(next.body.title);
  });

  test("POST /api/events creates a practice", async () => {
    const res = await request(app).post("/api/events").send({
      sportId: "cricket",
      title: "Net session",
      type: "PRACTICE",
      startsAt: Date.now() + 86_400_000,
      location: "Indoor nets"
    });
    expect(res.status).toBe(201);
    expect(res.body.id).toBeTruthy();
    expect(res.body.type).toBe("PRACTICE");
  });

  test("POST /api/performance stores sport metrics and monthly graph plots by date", async () => {
    const recordedAt = new Date(2026, 8, 12, 18, 0).getTime();
    const created = await request(app).post("/api/performance").send({
      sportId: "swimming",
      recordedAt,
      notes: "Good splits",
      metrics: { distance: 1500, time: 1260 }
    });
    expect(created.status).toBe(201);
    expect(created.body.metrics).toEqual({ distance: 1500, time: 1260 });
    expect(created.body.primaryMetric).toBe("distance");
    expect(created.body.score).toBeUndefined();

    const basketball = await request(app).post("/api/performance").send({
      sportId: "basketball",
      recordedAt,
      metrics: { points: 22, rebounds: 8 }
    });
    expect(basketball.status).toBe(201);
    expect(basketball.body.metrics).toEqual({ points: 22, rebounds: 8 });

    const rejected = await request(app).post("/api/performance").send({
      sportId: "basketball",
      score: 22
    });
    expect(rejected.status).toBe(400);

    const monthly = await request(app).get("/api/performance/monthly").query({
      sportId: "swimming",
      year: 2026,
      month: 9,
      metric: "distance"
    });
    expect(monthly.status).toBe(200);
    expect(monthly.body.year).toBe(2026);
    expect(monthly.body.month).toBe(9);
    expect(monthly.body.metric).toBe("distance");
    expect(monthly.body.points).toHaveLength(1);
    expect(monthly.body.points[0]).toMatchObject({
      date: "2026-09-12",
      day: 12,
      value: 1500
    });
  });

  test("GET /api/learn filters by category", async () => {
    const all = await request(app).get("/api/learn").query({ sportId: "basketball" });
    expect(all.status).toBe(200);
    expect(all.body).toHaveLength(4);

    const safety = await request(app).get("/api/learn").query({
      sportId: "basketball",
      category: "SAFETY"
    });
    expect(safety.body).toHaveLength(1);
    expect(safety.body[0].category).toBe("SAFETY");

    const detail = await request(app).get(`/api/learn/${safety.body[0].id}`);
    expect(detail.status).toBe(200);
    expect(detail.body.body).toContain("Warm up");
  });
});
