const express = require("express");
const { randomUUID } = require("crypto");
const { asyncHandler, HttpError } = require("../httpError");
const { publicDoc, publicList, parseMillis } = require("../store/shape");
const { LIMITS, clipped, assertLength } = require("../limits");

const TYPES = new Set(["PRACTICE", "SOCIAL_EVENT", "ANNOUNCEMENT"]);

function normalizeType(value) {
  const type = String(value || "").toUpperCase();
  if (type === "EVENT") return "SOCIAL_EVENT";
  return type;
}

function canManageEvent(event, uid) {
  if (!event || !uid) return false;
  if (!event.createdBy || event.createdBy === "seed") return false;
  return event.createdBy === uid;
}

function applyTextFields(target, body) {
  if (body.title !== undefined) {
    const title = clipped(body.title);
    const error = assertLength(title, LIMITS.title, "title");
    if (error) return error;
    if (!title) return "title is required";
    target.title = title;
  }
  if (body.location !== undefined) {
    const location = clipped(body.location);
    const error = assertLength(location, LIMITS.location, "location");
    if (error) return error;
    target.location = location;
  }
  if (body.description !== undefined) {
    const description = clipped(body.description);
    const error = assertLength(description, LIMITS.description, "description");
    if (error) return error;
    target.description = description;
  }
  if (body.notes !== undefined) {
    const notes = clipped(body.notes);
    const error = assertLength(notes, LIMITS.notes, "notes");
    if (error) return error;
    target.notes = notes;
  }
  return null;
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
    const title = clipped(body.title);
    const type = normalizeType(body.type);
    const startsAt = parseMillis(body.startsAt);
    if (!sportId || !title || !TYPES.has(type) || startsAt == null) {
      throw new HttpError(400, "sportId, title, type (PRACTICE|SOCIAL_EVENT|ANNOUNCEMENT) and startsAt are required");
    }
    const lengthError =
      assertLength(title, LIMITS.title, "title") ||
      assertLength(body.location, LIMITS.location, "location") ||
      assertLength(body.description, LIMITS.description, "description") ||
      assertLength(body.notes, LIMITS.notes, "notes");
    if (lengthError) throw new HttpError(400, lengthError);

    const sport = await req.app.locals.store.getSport(sportId);
    if (!sport) throw new HttpError(400, "Unknown sportId");

    const event = {
      id: randomUUID(),
      sportId,
      title,
      type,
      startsAt,
      endsAt: parseMillis(body.endsAt),
      location: clipped(body.location),
      description: clipped(body.description),
      notes: clipped(body.notes),
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
    if (!canManageEvent(existing, req.user.uid)) {
      throw new HttpError(403, "Only the organiser can edit this event");
    }
    const body = req.body || {};
    const textError = applyTextFields(existing, body);
    if (textError) throw new HttpError(400, textError);
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
    const store = req.app.locals.store;
    const existing = await store.getEvent(req.params.eventId);
    if (!existing) throw new HttpError(404, "Event not found");
    if (!canManageEvent(existing, req.user.uid)) {
      throw new HttpError(403, "Only the organiser can delete this event");
    }
    const deleted = await store.deleteEvent(req.params.eventId);
    if (!deleted) throw new HttpError(404, "Event not found");
    res.status(204).send();
  })
);

module.exports = router;
