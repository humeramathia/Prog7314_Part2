const { config } = require("../config");
const { firebaseReady, verifyIdToken } = require("../firebase");
const { HttpError } = require("../httpError");

async function requireAuth(req, res, next) {
  try {
    if (config.skipAuth) {
      req.user = {
        uid: config.devUser.uid,
        email: config.devUser.email,
        name: config.devUser.name
      };
      return next();
    }

    const header = req.headers.authorization || "";
    if (!header.startsWith("Bearer ")) {
      throw new HttpError(401, "Missing Bearer token");
    }
    if (!firebaseReady()) {
      throw new HttpError(503, "Auth is not configured. Add firebase-service-account.json or set SKIP_AUTH=true for local demo.");
    }

    const decoded = await verifyIdToken(header.slice(7));
    req.user = {
      uid: decoded.uid,
      email: decoded.email || "",
      name: decoded.name || decoded.email || "athlete"
    };
    next();
  } catch (err) {
    if (err instanceof HttpError) return next(err);
    next(new HttpError(401, "Invalid or expired token"));
  }
}

module.exports = { requireAuth };
