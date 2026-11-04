process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { verifiedPage } = require("../src/pages/verifiedPage");
const { sendVerificationEmail } = require("../src/auth/sendVerification");

test("verification fallback returns to the browser success page without an Android app link", async () => {
  const previousPackage = process.env.ANDROID_APP_PACKAGE;
  const previousFetch = global.fetch;
  process.env.ANDROID_APP_PACKAGE = "za.co.sportsphere.app";
  global.fetch = jest.fn().mockResolvedValue({ ok: true, json: async () => ({ email: "athlete@example.test" }) });
  try {
    await sendVerificationEmail("test-id-token");
    const sent = JSON.parse(global.fetch.mock.calls[0][1].body);
    expect(sent).toEqual({
      requestType: "VERIFY_EMAIL",
      idToken: "test-id-token",
      continueUrl: "https://sportsphere-st10276384.onrender.com/verified",
      canHandleCodeInApp: false
    });
  } finally {
    global.fetch = previousFetch;
    if (previousPackage == null) delete process.env.ANDROID_APP_PACKAGE;
    else process.env.ANDROID_APP_PACKAGE = previousPackage;
  }
});

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
