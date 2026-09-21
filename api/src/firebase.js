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

module.exports = { firebaseReady, verifyIdToken };
