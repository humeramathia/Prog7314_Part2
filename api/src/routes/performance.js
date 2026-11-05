const express = require("express");
const { randomUUID, createHash } = require("crypto");
const { asyncHandler, HttpError } = require("../httpError");
const { publicDoc, publicList, parseMillis, inYearMonth, monthByDate } = require("../store/shape");
const { parseMetrics, metricsFor, primaryMetricKey, metricValue } = require("../data/metrics");
const { LIMITS, clipped, assertLength } = require("../limits");

const router = express.Router();

function parseYearMonth(query) {
  const now = new Date();
  const year = query.year == null || query.year === "" ? now.getFullYear() : Number(query.year);
  const month = query.month == null || query.month === "" ? now.getMonth() + 1 : Number(query.month);
  if (!Number.isInteger(year) || year < 2000 || year > 2100) {
    throw new HttpError(400, "year must be a valid calendar year");
  }
  if (!Number.isInteger(month) || month < 1 || month > 12) {
    throw new HttpError(400, "month must be 1-12");
  }
  return { year, month };
}

router.get(
  "/",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    let sessions = await req.app.locals.store.listPerformance(req.user.uid, sportId || undefined);
    if (req.query.year != null || req.query.month != null) {
      const { year, month } = parseYearMonth(req.query);
      sessions = sessions.filter((session) => inYearMonth(session.recordedAt, year, month));
    }
    res.json(publicList(sessions));
  })
);

router.get(
  "/monthly",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    if (!sportId) throw new HttpError(400, "sportId is required");
    const schema = metricsFor(sportId);
    if (!schema.length) throw new HttpError(400, "Unknown sportId");

    const { year, month } = parseYearMonth(req.query);
    const metric = String(req.query.metric || primaryMetricKey(sportId)).trim();
    const field = schema.find((item) => item.key === metric);
    if (!field) {
      throw new HttpError(400, `metric must be one of: ${schema.map((item) => item.key).join(", ")}`);
    }

    const sessions = await req.app.locals.store.listPerformance(req.user.uid, sportId);
    res.json({
      sportId,
      year,
      month,
      metric: field.key,
      metricLabel: field.label,
      points: monthByDate(sessions, year, month, field.key, metricValue)
    });
  })
);

router.get(
  "/:sessionId",
  asyncHandler(async (req, res) => {
    const session = await req.app.locals.store.getPerformance(req.params.sessionId);
    if (!session || session.userId !== req.user.uid) throw new HttpError(404, "Session not found");
    res.json(publicDoc(session));
  })
);

router.post(
  "/",
  asyncHandler(async (req, res) => {
    const body = req.body || {};
    const clientId = body.clientId;
    if (clientId != null && (typeof clientId !== "string" ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(clientId))) {
      throw new HttpError(400, "clientId must be a UUID");
    }
    const sportId = String(body.sportId || "").trim();
    if (!sportId) throw new HttpError(400, "sportId is required");
    const sport = await req.app.locals.store.getSport(sportId);
    if (!sport) throw new HttpError(400, "Unknown sportId");

    const parsed = parseMetrics(sportId, body);
    if (parsed.error) throw new HttpError(400, parsed.error);

    const notes = clipped(body.notes);
    const notesError = assertLength(notes, LIMITS.notes, "notes");
    if (notesError) throw new HttpError(400, notesError);

    const recordedAt = parseMillis(body.recordedAt) ?? Date.now();
    const session = {
      // Include the authenticated owner: the same client UUID from two
      // accounts must never address the same session.
      id: clientId ? createHash("sha256").update(JSON.stringify([req.user.uid, clientId])).digest("hex") : randomUUID(),
      userId: req.user.uid,
      sportId,
      recordedAt,
      notes,
      metrics: parsed.metrics,
      primaryMetric: parsed.primaryMetric,
      primaryValue: parsed.primaryValue
    };
    if (clientId) session.clientId = clientId;
    try {
      // Atomic create (rather than upsert) also handles simultaneous retries.
      const saved = await req.app.locals.store.createPerformance(session);
      res.status(201).json(publicDoc(saved));
    } catch (error) {
      if (error.code !== 409) throw error;
      const saved = await req.app.locals.store.getPerformance(session.id);
      if (!saved || saved.userId !== req.user.uid) throw new HttpError(409, "Session conflict");
      const sameMetrics = Object.keys(saved.metrics).length === Object.keys(session.metrics).length &&
        Object.entries(session.metrics).every(([key, value]) => saved.metrics[key] === value);
      if (saved.sportId !== session.sportId || saved.recordedAt !== session.recordedAt ||
          saved.notes !== session.notes || !sameMetrics) {
        throw new HttpError(409, "clientId was already used for a different session");
      }
      res.status(200).json(publicDoc(saved));
    }
  })
);

router.delete(
  "/:sessionId",
  asyncHandler(async (req, res) => {
    const store = req.app.locals.store;
    const session = await store.getPerformance(req.params.sessionId);
    if (!session || session.userId !== req.user.uid) throw new HttpError(404, "Session not found");
    await store.deletePerformance(req.params.sessionId);
    res.status(204).send();
  })
);

module.exports = router;
