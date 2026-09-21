process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");

test("POST /api/events maps legacy EVENT to SOCIAL_EVENT", async () => {
  const store = createMemoryStore();
  await seedStore(store);
  const app = createApp(store);
  const res = await request(app).post("/api/events").send({
    sportId: "football",
    title: "Alumni match",
    type: "EVENT",
    startsAt: Date.now() + 86_400_000
  });
  expect(res.status).toBe(201);
  expect(res.body.type).toBe("SOCIAL_EVENT");
});
