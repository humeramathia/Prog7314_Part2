const { config } = require("../config");
const { createCosmosStore } = require("./cosmosStore");
const { createMemoryStore } = require("./memoryStore");
const { seedDocuments } = require("../data/catalog");

async function createStore() {
  if (config.cosmos.enabled) {
    return createCosmosStore();
  }
  console.warn("COSMOS_KEY is missing. Using in-memory store (data resets when the process stops).");
  const store = createMemoryStore();
  await seedStore(store);
  return store;
}

async function seedStore(store) {
  const { sports, events, learning } = seedDocuments();
  for (const sport of sports) await store.upsertSport(sport);
  for (const event of events) await store.upsertEvent(event);
  for (const guide of learning) await store.upsertLearning(guide);
}

module.exports = { createStore, seedStore };
