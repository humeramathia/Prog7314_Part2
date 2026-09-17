const express = require("express");
const { randomUUID } = require("crypto");
const { asyncHandler, HttpError } = require("../httpError");
const { publicDoc, publicList, monthlyAverages } = require("../store/shape");

const router = express.Router();

router.get(
  "/",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    const sessions = await req.app.locals.store.listPerformance(req.user.uid, sportId || undefined);
    res.json(publicList(sessions));
  })
);

router.get(
  "/monthly",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    const year = Number(req.query.year) || new Date().getFullYear();
    const sessions = await req.app.locals.store.listPerformance(req.user.uid, sportId || undefined);
    res.json({ year, points: monthlyAverages(sessions, year) });
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
    const sportId = String(body.sportId || "").trim();
    const score = Number(body.score);
    if (!sportId || !Number.isFinite(score)) {
      throw new HttpError(400, "sportId and numeric score are required");
    }
    const sport = await req.app.locals.store.getSport(sportId);
    if (!sport) throw new HttpError(400, "Unknown sportId");

    const session = {
      id: randomUUID(),
      userId: req.user.uid,
      sportId,
      recordedAt: Date.now(),
      score,
      notes: String(body.notes || "").trim()
    };
    res.status(201).json(publicDoc(await req.app.locals.store.upsertPerformance(session)));
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
