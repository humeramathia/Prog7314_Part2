const express = require("express");
const { asyncHandler, HttpError } = require("../httpError");
const { sendVerificationEmail } = require("../auth/sendVerification");
const { verifyIdToken, generateEmailVerificationLink } = require("../firebase");

const router = express.Router();

function bearerToken(req) {
  const header = req.headers.authorization || "";
  if (!header.startsWith("Bearer ")) {
    throw new HttpError(401, "Missing Bearer token");
  }
  return header.slice(7);
}

router.post(
  "/send-verification",
  asyncHandler(async (req, res) => {
    res.json(await sendVerificationEmail(bearerToken(req)));
  })
);

router.post(
  "/verification-link",
  asyncHandler(async (req, res) => {
    const decoded = await verifyIdToken(bearerToken(req));
    const email = decoded && decoded.email;
    if (!email) {
      throw new HttpError(400, "Account has no email");
    }
    try {
      const link = await generateEmailVerificationLink(email);
      res.json({ ok: true, link });
    } catch (error) {
      const message = (error && error.message) || "Could not build a verification link";
      throw new HttpError(503, message);
    }
  })
);

module.exports = router;
