const express = require("express");
const { asyncHandler, HttpError } = require("../httpError");
const { publicDoc, publicList } = require("../store/shape");

const router = express.Router();

router.get(
  "/",
  asyncHandler(async (req, res) => {
    const sports = await req.app.locals.store.listSports();
    res.json(publicList(sports));
  })
);

router.get(
  "/:sportId",
  asyncHandler(async (req, res) => {
    const sport = await req.app.locals.store.getSport(req.params.sportId);
    if (!sport) throw new HttpError(404, "Sport not found");
    res.json(publicDoc(sport));
  })
);

module.exports = router;
