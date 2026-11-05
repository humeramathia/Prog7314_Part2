const { requirePersistentStore } = require("../src/store");

test("production refuses to start without Cosmos", () => {
  expect(() => requirePersistentStore("production", false)).toThrow(/COSMOS_KEY/);
});

test("development can use the in-memory store", () => {
  expect(() => requirePersistentStore("development", false)).not.toThrow();
});

test("production is allowed when Cosmos is configured", () => {
  expect(() => requirePersistentStore("production", true)).not.toThrow();
});
