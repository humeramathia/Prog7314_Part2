const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "..", ".env") });

function isPlaceholder(value) {
  return !value || value.includes("paste-") || value.includes("your-");
}

const config = {
  port: Number(process.env.PORT) || 3000,
  nodeEnv: process.env.NODE_ENV || "development",
  skipAuth: process.env.SKIP_AUTH === "true",
  devUser: {
    uid: process.env.DEV_USER_ID || "dev-user",
    email: process.env.DEV_USER_EMAIL || "dev@sportsphere.app",
    name: process.env.DEV_USER_NAME || "Dev User"
  },
  cosmos: {
    endpoint: process.env.COSMOS_ENDPOINT,
    key: process.env.COSMOS_KEY,
    database: process.env.COSMOS_DATABASE || "sportsphere",
    enabled: !isPlaceholder(process.env.COSMOS_KEY)
  },
  firebase: {
    credentialsPath: process.env.GOOGLE_APPLICATION_CREDENTIALS
      ? path.resolve(__dirname, "..", process.env.GOOGLE_APPLICATION_CREDENTIALS)
      : path.resolve(__dirname, "..", "firebase-service-account.json"),
    projectId: process.env.FIREBASE_PROJECT_ID
  }
};

module.exports = { config, isPlaceholder };
