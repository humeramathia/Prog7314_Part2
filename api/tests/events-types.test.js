process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");

test("POST /api/events accepts ANNOUNCEMENT", async () => {
  const store = createMemoryStore();
  await seedStore(store);
  const app = createApp(store);
  const res = await request(app).post("/api/events").send({
    sportId: "tennis",
    title: "Court closed Saturday",
    type: "ANNOUNCEMENT",
    startsAt: Date.now() + 86_400_000,
    description: "Resurface work."
  });
  expect(res.status).toBe(201);
  expect(res.body.type).toBe("ANNOUNCEMENT");
});
