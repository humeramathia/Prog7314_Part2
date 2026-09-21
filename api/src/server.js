const { createStore, seedStore } = require("./store");
const { createApp } = require("./app");
const { config } = require("./config");

async function main() {
  const store = await createStore();
  const existing = await store.listSports();
  if (!existing.length) {
    await seedStore(store);
    console.log(`Seeded catalog into the ${store.kind} store.`);
  }
  const app = createApp(store);
  const host = process.env.HOST || "0.0.0.0";
  app.listen(config.port, host, () => {
    console.log(`SportSphere API listening on ${host}:${config.port}`);
    console.log(`Store: ${store.kind}${config.skipAuth ? " | SKIP_AUTH=true" : " | Firebase tokens required"}`);
  });
}

main().catch((err) => {
  console.error("Failed to start API", err);
  process.exit(1);
});
