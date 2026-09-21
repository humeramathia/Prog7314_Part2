process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");

test("unknown routes return JSON 404", async () => {
  const app = createApp(createMemoryStore());
  const res = await request(app).get("/does-not-exist");
  expect(res.status).toBe(404);
  expect(res.body.error).toBe("Not found");
});
