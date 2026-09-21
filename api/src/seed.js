const { createStore, seedStore } = require("./store");

async function main() {
  const store = await createStore();
  await seedStore(store);
  const sports = await store.listSports();
  const footballEvents = await store.listEvents("football");
  const footballGuides = await store.listLearning("football");
  console.log(
    `Seeded ${sports.length} sports, ${footballEvents.length} football events, ${footballGuides.length} football guides into the ${store.kind} store.`
  );
}

main().catch((err) => {
  console.error("Seed failed", err);
  process.exit(1);
});
