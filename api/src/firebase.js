const fs = require("fs");
const { config } = require("./config");

let admin = null;
let initialized = false;

function loadAdmin() {
  if (!admin) {
    admin = require("firebase-admin");
  }
  return admin;
}

function initFirebase() {
  if (initialized) return admin && admin.apps.length > 0;
  initialized = true;
  if (config.skipAuth) return false;
  if (!fs.existsSync(config.firebase.credentialsPath)) return false;
  const firebaseAdmin = loadAdmin();
  firebaseAdmin.initializeApp({
    credential: firebaseAdmin.credential.cert(require(config.firebase.credentialsPath)),
    projectId: config.firebase.projectId || undefined
  });
  return true;
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
