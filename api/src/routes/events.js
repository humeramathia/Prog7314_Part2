const express = require("express");
const { randomUUID } = require("crypto");
const { asyncHandler, HttpError } = require("../httpError");
const { publicDoc, publicList, parseMillis } = require("../store/shape");

const TYPES = new Set(["PRACTICE", "SOCIAL_EVENT", "ANNOUNCEMENT"]);

function normalizeType(value) {
  const type = String(value || "").toUpperCase();
  if (type === "EVENT") return "SOCIAL_EVENT";
  return type;
}

const router = express.Router();

router.get(
  "/",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    if (!sportId) throw new HttpError(400, "sportId is required");
    const from = parseMillis(req.query.from);
    const to = parseMillis(req.query.to);
    const events = await req.app.locals.store.listEvents(sportId, from, to);
    res.json(publicList(events));
  })
);

router.get(
  "/next",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    if (!sportId) throw new HttpError(400, "sportId is required");
    const now = Date.now();
    const events = await req.app.locals.store.listEvents(sportId);
    const upcoming = events.find((event) => event.startsAt >= now) || events[events.length - 1] || null;
    if (!upcoming) throw new HttpError(404, "No events found");
    res.json(publicDoc(upcoming));
  })
);

router.get(
  "/:eventId",
  asyncHandler(async (req, res) => {
    const event = await req.app.locals.store.getEvent(req.params.eventId);
    if (!event) throw new HttpError(404, "Event not found");
    res.json(publicDoc(event));
  })
);

router.post(
  "/",
  asyncHandler(async (req, res) => {
    const body = req.body || {};
    const sportId = String(body.sportId || "").trim();
    const title = String(body.title || "").trim();
    const type = normalizeType(body.type);
    const startsAt = parseMillis(body.startsAt);
    if (!sportId || !title || !TYPES.has(type) || startsAt == null) {
      throw new HttpError(400, "sportId, title, type (PRACTICE|SOCIAL_EVENT|ANNOUNCEMENT) and startsAt are required");
    }
    const sport = await req.app.locals.store.getSport(sportId);
    if (!sport) throw new HttpError(400, "Unknown sportId");

    const event = {
      id: randomUUID(),
      sportId,
      title,
      type,
      startsAt,
      endsAt: parseMillis(body.endsAt),
      location: String(body.location || "").trim(),
      description: String(body.description || "").trim(),
      notes: String(body.notes || "").trim(),
      createdBy: req.user.uid
    };
    res.status(201).json(publicDoc(await req.app.locals.store.upsertEvent(event)));
  })
);

router.put(
  "/:eventId",
  asyncHandler(async (req, res) => {
    const store = req.app.locals.store;
    const existing = await store.getEvent(req.params.eventId);
    if (!existing) throw new HttpError(404, "Event not found");
    const body = req.body || {};
    if (body.title !== undefined) existing.title = String(body.title).trim();
    if (body.location !== undefined) existing.location = String(body.location).trim();
    if (body.description !== undefined) existing.description = String(body.description).trim();
    if (body.notes !== undefined) existing.notes = String(body.notes).trim();
    if (body.startsAt !== undefined) {
      const startsAt = parseMillis(body.startsAt);
      if (startsAt == null) throw new HttpError(400, "startsAt must be epoch millis");
      existing.startsAt = startsAt;
    }
    if (body.endsAt !== undefined) {
      existing.endsAt = body.endsAt == null || body.endsAt === "" ? null : parseMillis(body.endsAt);
    }
    if (body.type !== undefined) {
      const type = normalizeType(body.type);
      if (!TYPES.has(type)) {
        throw new HttpError(400, "type must be PRACTICE, SOCIAL_EVENT or ANNOUNCEMENT");
      }
      existing.type = type;
    }
    res.json(publicDoc(await store.upsertEvent(existing)));
  })
);

router.delete(
  "/:eventId",
  asyncHandler(async (req, res) => {
    const deleted = await req.app.locals.store.deleteEvent(req.params.eventId);
    if (!deleted) throw new HttpError(404, "Event not found");
    res.status(204).send();
  })
);

module.exports = router;
