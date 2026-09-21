process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");

test("GET /api/learn requires sportId", async () => {
  const store = createMemoryStore();
  await seedStore(store);
  const app = createApp(store);
  const res = await request(app).get("/api/learn");
  expect(res.status).toBe(400);
});

test("GET /api/learn filters by category", async () => {
  const store = createMemoryStore();
  await seedStore(store);
  const app = createApp(store);
  const res = await request(app).get("/api/learn").query({ sportId: "tennis", category: "SAFETY" });
  expect(res.status).toBe(200);
  expect(res.body.length).toBeGreaterThan(0);
  expect(res.body.every((guide) => guide.category === "SAFETY")).toBe(true);
});
