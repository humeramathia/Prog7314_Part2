const express = require("express");
const { asyncHandler } = require("../httpError");
const { publicDoc } = require("../store/shape");

async function ensureUser(store, firebaseUser) {
  const existing = await store.getUser(firebaseUser.uid);
  if (existing) return existing;
  const created = {
    id: firebaseUser.uid,
    userId: firebaseUser.uid,
    email: firebaseUser.email,
    displayName: firebaseUser.name,
    sportId: "",
    sportName: "",
    darkMode: false,
    createdAt: Date.now(),
    updatedAt: Date.now()
  };
  return store.upsertUser(created);
}

const router = express.Router();

router.get(
  "/",
  asyncHandler(async (req, res) => {
    const user = await ensureUser(req.app.locals.store, req.user);
    res.json(publicDoc(user));
  })
);

router.patch(
  "/",
  asyncHandler(async (req, res) => {
    const store = req.app.locals.store;
    const user = await ensureUser(store, req.user);
    const { displayName, sportId, sportName, darkMode } = req.body || {};

    if (displayName !== undefined) user.displayName = String(displayName).trim();
    if (darkMode !== undefined) user.darkMode = Boolean(darkMode);

    if (sportId !== undefined) {
      const sport = await store.getSport(String(sportId));
      if (!sport) {
        res.status(400).json({ error: "Unknown sportId" });
        return;
      }
      user.sportId = sport.sportId;
      user.sportName = sportName ? String(sportName) : sport.name;
    } else if (sportName !== undefined) {
      user.sportName = String(sportName);
    }

    user.updatedAt = Date.now();
    res.json(publicDoc(await store.upsertUser(user)));
  })
);

module.exports = router;
