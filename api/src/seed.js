const { createStore, seedStore } = require("./store");

async function main() {
  const store = await createStore();
  await seedStore(store);
  const sports = await store.listSports();
  const footballEvents = await store.listEvents("football");
  const footballGuides = await store.listLearning("football");
  const techniques = footballGuides.find((guide) => guide.category === "TECHNIQUES");
  console.log(
    `Seeded ${sports.length} sports, ${footballEvents.length} football events, ${footballGuides.length} football guides into the ${store.kind} store.`
  );
  if (techniques?.mediaUrl) {
    console.log(`Football techniques image: ${techniques.mediaUrl}`);
  }
}

main().catch((err) => {
  console.error("Seed failed", err);
  process.exit(1);
});
