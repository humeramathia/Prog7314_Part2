process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");

test("GET /health reports store and auth mode", async () => {
  const store = createMemoryStore();
  await seedStore(store);
  const app = createApp(store);
  const res = await request(app).get("/health");
  expect(res.status).toBe(200);
  expect(res.body.ok).toBe(true);
  expect(res.body.version).toBeTruthy();
  expect(res.body.skipAuth).toBe(true);
  expect(res.body.service).toBe("sportsphere-api");
});
