process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { verifiedPage } = require("../src/pages/verifiedPage");

test("POST /api/auth/send-verification requires a Bearer token", async () => {
  const app = createApp(createMemoryStore());
  const res = await request(app).post("/api/auth/send-verification");
  expect(res.status).toBe(401);
  expect(res.body.error).toBe("Missing Bearer token");
});

test("GET /verified tells the user to return to the app", async () => {
  const app = createApp(createMemoryStore());
  const res = await request(app).get("/verified");
  expect(res.status).toBe(200);
  expect(res.headers["content-type"]).toMatch(/html/);
  expect(res.text).toContain("Email verified");
  expect(res.text).toContain("I have confirmed");
});

test("verified page HTML is self-contained", () => {
  expect(verifiedPage()).toContain("Return to SportSphere");
});
