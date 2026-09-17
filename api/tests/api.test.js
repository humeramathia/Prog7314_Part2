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

  test("POST /api/performance and monthly graph", async () => {
    const created = await request(app).post("/api/performance").send({
      sportId: "swimming",
      score: 81,
      notes: "Good splits"
    });
    expect(created.status).toBe(201);
    expect(created.body.userId).toBe("dev-user");

    const list = await request(app).get("/api/performance").query({ sportId: "swimming" });
    expect(list.body).toHaveLength(1);

    const monthly = await request(app).get("/api/performance/monthly").query({
      sportId: "swimming",
      year: new Date().getFullYear()
    });
    expect(monthly.status).toBe(200);
    expect(monthly.body.points).toHaveLength(12);
    const thisMonth = monthly.body.points[new Date().getMonth()];
    expect(thisMonth.average).toBe(81);
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
