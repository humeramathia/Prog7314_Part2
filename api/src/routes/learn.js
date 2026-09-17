const express = require("express");
const { asyncHandler, HttpError } = require("../httpError");
const { publicDoc, publicList } = require("../store/shape");
const { CATEGORIES } = require("../data/catalog");

const router = express.Router();

router.get(
  "/",
  asyncHandler(async (req, res) => {
    const sportId = String(req.query.sportId || "").trim();
    if (!sportId) throw new HttpError(400, "sportId is required");
    const category = req.query.category ? String(req.query.category).toUpperCase() : undefined;
    if (category && !CATEGORIES.includes(category)) {
      throw new HttpError(400, "category must be RULES, TECHNIQUES, TRAINING or SAFETY");
    }
    const guides = await req.app.locals.store.listLearning(sportId, category);
    res.json(publicList(guides));
  })
);

router.get(
  "/:guideId",
  asyncHandler(async (req, res) => {
    const guide = await req.app.locals.store.getLearning(req.params.guideId);
    if (!guide) throw new HttpError(404, "Guide not found");
    res.json(publicDoc(guide));
  })
);

module.exports = router;
