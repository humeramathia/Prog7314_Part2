process.env.SKIP_AUTH = "true";

const request = require("supertest");
const { createApp } = require("../src/app");
const { createMemoryStore } = require("../src/store/memoryStore");
const { seedStore } = require("../src/store");

async function appWithStore() {
  const store = createMemoryStore();
  await seedStore(store);
  return { app: createApp(store), store };
}

test("seed events cannot be edited or deleted by a signed-in user", async () => {
  const { app } = await appWithStore();
  const list = await request(app).get("/api/events").query({ sportId: "football" });
  const seedEvent = list.body.find((event) => event.createdBy === "seed");
  expect(seedEvent).toBeTruthy();

  const edited = await request(app).put(`/api/events/${seedEvent.id}`).send({ title: "Hacked title" });
  expect(edited.status).toBe(403);

  const removed = await request(app).delete(`/api/events/${seedEvent.id}`);
  expect(removed.status).toBe(403);
});

test("only the organiser can edit or delete a user-created event", async () => {
  const { app, store } = await appWithStore();
  await store.upsertEvent({
    id: "foreign-event",
    sportId: "football",
    title: "Private session",
    type: "PRACTICE",
    startsAt: Date.now() + 86_400_000,
    location: "Back pitch",
    description: "",
    notes: "",
    createdBy: "another-user"
  });

  const edited = await request(app).put("/api/events/foreign-event").send({ title: "Taken over" });
  expect(edited.status).toBe(403);

  const removed = await request(app).delete("/api/events/foreign-event");
  expect(removed.status).toBe(403);
});

test("the organiser can update and delete their own event", async () => {
  const { app } = await appWithStore();
  const created = await request(app).post("/api/events").send({
    sportId: "tennis",
    title: "Hit with a partner",
    type: "PRACTICE",
    startsAt: Date.now() + 86_400_000
  });
  expect(created.status).toBe(201);
  expect(created.body.createdBy).toBe("dev-user");

  const edited = await request(app)
    .put(`/api/events/${created.body.id}`)
    .send({ title: "Hit with two partners" });
  expect(edited.status).toBe(200);
  expect(edited.body.title).toBe("Hit with two partners");

  const removed = await request(app).delete(`/api/events/${created.body.id}`);
  expect(removed.status).toBe(204);
});

test("event text fields have length limits", async () => {
  const { app } = await appWithStore();
  const res = await request(app).post("/api/events").send({
    sportId: "football",
    title: "x".repeat(81),
    type: "PRACTICE",
    startsAt: Date.now() + 86_400_000
  });
  expect(res.status).toBe(400);
  expect(res.body.error).toMatch(/title/);
});
