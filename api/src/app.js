const express = require("express");
const cors = require("cors");
const { requireAuth } = require("./middleware/auth");
const { errorHandler } = require("./httpError");
const meRouter = require("./routes/me");
const sportsRouter = require("./routes/sports");
const eventsRouter = require("./routes/events");
const performanceRouter = require("./routes/performance");
const learnRouter = require("./routes/learn");
const authRouter = require("./routes/auth");
const { verifyEmailPage } = require("./pages/verifyEmailPage");
const { verifiedPage } = require("./pages/verifiedPage");

function createApp(store) {
  const app = express();
  app.locals.store = store;
  app.use(cors({ origin: true }));
  app.use(express.json({ limit: "1mb" }));

  app.get("/health", (req, res) => {
    const { firebaseReady } = require("./firebase");
    const { version } = require("../package.json");
    res.json({
      ok: true,
      service: "sportsphere-api",
      version,
      store: store.kind,
      skipAuth: require("./config").config.skipAuth,
      firebaseAdmin: firebaseReady()
    });
  });

  const verifyEmailHtml = (req, res) => {
    const key = process.env.FIREBASE_WEB_API_KEY || "AIzaSyAGvb3cS_XxE0LCsSpVXLtVkL7c5KoC5MU";
    res.setHeader("Content-Type", "text/html; charset=utf-8");
    res.send(verifyEmailPage(key));
  };
  app.get("/verify-email", verifyEmailHtml);
  app.get("/auth/action", verifyEmailHtml);
  app.get("/verified", (_req, res) => {
    res.setHeader("Content-Type", "text/html; charset=utf-8");
    res.send(verifiedPage());
  });

  app.use("/api/auth", authRouter);
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
