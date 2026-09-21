const express = require("express");
const { asyncHandler, HttpError } = require("../httpError");
const { sendVerificationEmail } = require("../auth/sendVerification");

const router = express.Router();

router.post(
  "/send-verification",
  asyncHandler(async (req, res) => {
    const header = req.headers.authorization || "";
    if (!header.startsWith("Bearer ")) {
      throw new HttpError(401, "Missing Bearer token");
    }
    res.json(await sendVerificationEmail(header.slice(7)));
  })
);

module.exports = router;
