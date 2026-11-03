process.env.SKIP_AUTH = "true";
const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");
const { config } = require("../src/config");

const body = {
  clientId: "8c4a0eed-7964-4c8a-b2ed-96cb6b56f8bd",
  sportId: "swimming", recordedAt: 1793100000000,
  notes: "Offline practice", metrics: { distance: 800, time: 900 }
};
let app;
beforeEach(async () => {
  const store = createMemoryStore();
  await seedStore(store);
  app = createApp(store);
});

test("health advertises retry-safe uploads", async () => {
  const res = await request(app).get("/health");
  expect(res.body.capabilities.performanceClientId).toBe(true);
});

test("retry after a lost response returns the same document", async () => {
  const first = await request(app).post("/api/performance").send(body);
  const retry = await request(app).post("/api/performance").send(body);
  expect(first.status).toBe(201);
  expect(retry.status).toBe(200);
  expect(retry.body.id).toBe(first.body.id);
  expect(retry.body.clientId).toBe(body.clientId);
  const list = await request(app).get("/api/performance?sportId=swimming");
  expect(list.body.filter(x => x.clientId === body.clientId)).toHaveLength(1);
});

test("concurrent retries create only one session", async () => {
  const responses = await Promise.all(Array.from({ length: 5 }, () =>
    request(app).post("/api/performance").send(body)));
  expect(responses.map(x => x.status).sort()).toEqual([200, 200, 200, 200, 201]);
  expect(new Set(responses.map(x => x.body.id)).size).toBe(1);
});

test("reusing a UUID for changed input rejects the change", async () => {
  const first = await request(app).post("/api/performance").send(body);
  const retry = await request(app).post("/api/performance").send({ ...body, notes: "Changed" });
  expect(retry.status).toBe(409);
  const saved = await request(app).get(`/api/performance/${first.body.id}`);
  expect(saved.body.notes).toBe(body.notes);
});

test("different users can use the same client UUID without sharing data", async () => {
  const original = config.devUser.uid;
  try {
    const first = await request(app).post("/api/performance").send(body);
    config.devUser.uid = "another-user";
    const second = await request(app).post("/api/performance").send(body);
    expect(second.status).toBe(201);
    expect(second.body.id).not.toBe(first.body.id);
    expect(second.body.userId).toBe("another-user");
    expect((await request(app).get(`/api/performance/${first.body.id}`)).status).toBe(404);
    expect((await request(app).delete(`/api/performance/${first.body.id}`)).status).toBe(404);
  } finally { config.devUser.uid = original; }
});

test.each(["not-a-uuid", "", 9, {}, []])("rejects invalid client UUID %j", async clientId => {
  const res = await request(app).post("/api/performance").send({ ...body, clientId });
  expect(res.status).toBe(400);
});

test("legacy online callers may omit clientId", async () => {
  const { clientId, ...legacy } = body;
  expect((await request(app).post("/api/performance").send(legacy)).status).toBe(201);
});
