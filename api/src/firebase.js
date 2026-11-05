const fs = require("fs");
const os = require("os");
const path = require("path");
const { config } = require("./config");

let admin = null;
let initialized = false;

function loadAdmin() {
  if (!admin) {
    admin = require("firebase-admin");
  }
  return admin;
}

function credentialsFromEnv() {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    try {
      return JSON.parse(Buffer.from(raw, "base64").toString("utf8"));
    } catch {
      return null;
    }
  }
}

function credentialsPath() {
  const fromEnv = credentialsFromEnv();
  if (fromEnv) {
    const filePath = path.join(os.tmpdir(), "firebase-service-account.json");
    fs.writeFileSync(filePath, JSON.stringify(fromEnv));
    return filePath;
  }
  if (fs.existsSync(config.firebase.credentialsPath)) {
    return config.firebase.credentialsPath;
  }
  return null;
}

function initFirebase() {
  if (initialized) return admin && admin.apps.length > 0;
  initialized = true;
  try {
    if (config.skipAuth) return false;
    const filePath = credentialsPath();
    if (!filePath) return false;
    const firebaseAdmin = loadAdmin();
    firebaseAdmin.initializeApp({
      credential: firebaseAdmin.credential.cert(require(filePath)),
      projectId: config.firebase.projectId || undefined
    });
    return true;
  } catch (error) {
    console.error("Firebase Admin init failed:", error.message);
    return false;
  }
}

function firebaseReady() {
  return initFirebase();
}

async function verifyIdToken(token) {
  if (!firebaseReady()) {
    throw new Error("Firebase Admin is not configured");
  }
  return loadAdmin().auth().verifyIdToken(token);
}

/**
 * Firebase Cloud Messaging client, or null when Firebase Admin is not
 * configured (local dev without a service account, Jest).
 */
function messaging() {
  if (!firebaseReady()) return null;
  return loadAdmin().messaging();
}

/**
 * Builds a verify-email URL for the signed-in user without sending mail.
 * Hotmail / Outlook often drop Firebase's noreply messages; the app can
 * open this link in Chrome instead.
 */
async function generateEmailVerificationLink(email) {
  if (!firebaseReady()) {
    throw new Error("Firebase Admin is not configured");
  }
  return loadAdmin().auth().generateEmailVerificationLink(email, {
    url: "https://sportsphere-st10276384.firebaseapp.com",
    handleCodeInApp: false
  });
}

module.exports = {
  firebaseReady,
  verifyIdToken,
  messaging,
  generateEmailVerificationLink
};
