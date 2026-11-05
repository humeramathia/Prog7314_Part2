process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");
const { createMemoryNotifier } = require("../src/notify");

async function setup(options = {}) {
  const store = createMemoryStore();
  await seedStore(store);
  const notifier = options.noNotifier ? undefined : createMemoryNotifier();
  return { app: createApp(store, { notifier }), store, notifier };
}

describe("PATCH /api/me device settings", () => {
  test("stores the push token without echoing it back", async () => {
    const { app, store } = await setup();
    const res = await request(app).patch("/api/me").send({ fcmToken: "token-abc" });
    expect(res.status).toBe(200);
    expect(res.body.fcmToken).toBeUndefined();
    expect(res.body.hasDeviceToken).toBe(true);
    expect((await store.getUser("dev-user")).fcmToken).toBe("token-abc");
  });

  test("an empty token removes the registration", async () => {
    const { app, store } = await setup();
    await request(app).patch("/api/me").send({ fcmToken: "token-abc" });
    const res = await request(app).patch("/api/me").send({ fcmToken: "" });
    expect(res.body.hasDeviceToken).toBe(false);
    expect((await store.getUser("dev-user")).fcmToken).toBeUndefined();
  });

  test("saves language and notification preferences", async () => {
    const { app } = await setup();
    const res = await request(app).patch("/api/me").send({
      language: "XH",
      notificationsEnabled: false,
      notifyPractice: false,
      notifyAnnouncements: true
    });
    expect(res.status).toBe(200);
    expect(res.body.language).toBe("xh");
    expect(res.body.notificationsEnabled).toBe(false);
    expect(res.body.notifyPractice).toBe(false);
    expect(res.body.notifyAnnouncements).toBe(true);
    expect(res.body.notifyEvents).toBe(true);
  });

  test("defaults language to English and notifications on", async () => {
    const { app } = await setup();
    const res = await request(app).get("/api/me");
    expect(res.body.language).toBe("en");
    expect(res.body.notificationsEnabled).toBe(true);
    expect(res.body.notifyPractice).toBe(true);
    expect(res.body.notifyEvents).toBe(true);
    expect(res.body.notifyAnnouncements).toBe(true);
  });

  test("DELETE /api/me removes the profile", async () => {
    const { app, store } = await setup();
    await request(app).patch("/api/me").send({ displayName: "Athlete" });
    const res = await request(app).delete("/api/me");
    expect(res.status).toBe(204);
    expect(await store.getUser("dev-user")).toBeNull();
  });

  test("rejects an unsupported language", async () => {
    const { app } = await setup();
    const res = await request(app).patch("/api/me").send({ language: "fr" });
    expect(res.status).toBe(400);
  });

  test("rejects a non-string token", async () => {
    const { app } = await setup();
    const res = await request(app).patch("/api/me").send({ fcmToken: 42 });
    expect(res.status).toBe(400);
  });
});

describe("POST /api/me/test-notification", () => {
  test("returns 409 before the phone has registered", async () => {
    const { app } = await setup();
    const res = await request(app).post("/api/me/test-notification");
    expect(res.status).toBe(409);
  });

  test("sends to the caller's own device in their language", async () => {
    const { app, notifier } = await setup();
    await request(app).patch("/api/me").send({ fcmToken: "token-abc", language: "af" });
    const res = await request(app).post("/api/me/test-notification");
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
    expect(notifier.sent[0]).toEqual(expect.objectContaining({ token: "token-abc", title: "SportSphere-toets" }));
  });

  test("returns 409 when notifications are switched off", async () => {
    const { app } = await setup();
    await request(app).patch("/api/me").send({ fcmToken: "token-abc", notificationsEnabled: false });
    const res = await request(app).post("/api/me/test-notification");
    expect(res.status).toBe(409);
  });

  test("returns 503 when the server has no Firebase credentials", async () => {
    const { app } = await setup({ noNotifier: true });
    await request(app).patch("/api/me").send({ fcmToken: "token-abc" });
    const res = await request(app).post("/api/me/test-notification");
    expect(res.status).toBe(503);
  });
});

describe("fixture pushes", () => {
  test("creating a fixture notifies the sport topic", async () => {
    const { app, notifier } = await setup();
    const res = await request(app)
      .post("/api/events")
      .send({ sportId: "football", title: "Derby", type: "SOCIAL_EVENT", startsAt: Date.now() + 86_400_000 });
    expect(res.status).toBe(201);
    expect(notifier.sent).toHaveLength(1);
    expect(notifier.sent[0].topic).toBe("sport_football_event");
    expect(notifier.sent[0].data.eventId).toBe(res.body.id);
  });

  test("editing a fixture sends an update", async () => {
    const { app, notifier } = await setup();
    const created = await request(app)
      .post("/api/events")
      .send({ sportId: "football", title: "Derby", type: "PRACTICE", startsAt: Date.now() + 86_400_000 });
    await request(app).put(`/api/events/${created.body.id}`).send({ location: "Pitch 2" });
    expect(notifier.sent).toHaveLength(2);
    expect(notifier.sent[1].data.action).toBe("updated");
  });
});
