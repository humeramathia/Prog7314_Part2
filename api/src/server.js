const { createStore } = require("./store");
const { createApp } = require("./app");
const { config } = require("./config");

async function main() {
  const store = await createStore();
  const app = createApp(store);
  app.listen(config.port, () => {
    console.log(`SportSphere API listening on http://localhost:${config.port}`);
    console.log(`Store: ${store.kind}${config.skipAuth ? " | SKIP_AUTH=true" : ""}`);
  });
}

main().catch((err) => {
  console.error("Failed to start API", err);
  process.exit(1);
});
