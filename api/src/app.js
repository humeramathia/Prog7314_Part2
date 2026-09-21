const express = require("express");
const cors = require("cors");
const { requireAuth } = require("./middleware/auth");
const { errorHandler } = require("./httpError");
const meRouter = require("./routes/me");
const sportsRouter = require("./routes/sports");
const eventsRouter = require("./routes/events");
const performanceRouter = require("./routes/performance");
const learnRouter = require("./routes/learn");

function createApp(store) {
  const app = express();
  app.locals.store = store;
  app.use(cors());
  app.use(express.json({ limit: "1mb" }));

  app.get("/health", (req, res) => {
    const { firebaseReady } = require("./firebase");
    res.json({
      ok: true,
      service: "sportsphere-api",
      store: store.kind,
      skipAuth: require("./config").config.skipAuth,
      firebaseAdmin: firebaseReady()
    });
  });

  app.use("/api/me", requireAuth, meRouter);
  app.use("/api/sports", requireAuth, sportsRouter);
  app.use("/api/events", requireAuth, eventsRouter);
  app.use("/api/performance", requireAuth, performanceRouter);
  app.use("/api/learn", requireAuth, learnRouter);

  app.use((req, res) => {
    res.status(404).json({ error: "Not found" });
  });
  app.use(errorHandler);
  return app;
}

module.exports = { createApp };
