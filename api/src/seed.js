const { createStore, seedStore } = require("./store");

async function main() {
  const store = await createStore();
  await seedStore(store);
  console.log(`Seeded sports, events and learning guides into the ${store.kind} store.`);
}

main().catch((err) => {
  console.error("Seed failed", err);
  process.exit(1);
});
