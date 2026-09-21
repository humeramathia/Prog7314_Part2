const { HttpError } = require("../httpError");

const CONTINUE_URL = "https://sportsphere-st10276384.onrender.com/verify-email";
const ANDROID_PACKAGE = "com.example.prog7314_part2";

function webApiKey() {
  return process.env.FIREBASE_WEB_API_KEY || "AIzaSyAGvb3cS_XxE0LCsSpVXLtVkL7c5KoC5MU";
}

async function sendVerificationEmail(idToken) {
  if (!idToken) {
    throw new HttpError(401, "Missing Bearer token");
  }

  const res = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=${encodeURIComponent(webApiKey())}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        requestType: "VERIFY_EMAIL",
        idToken,
        continueUrl: CONTINUE_URL,
        canHandleCodeInApp: false,
        androidPackageName: ANDROID_PACKAGE
      })
    }
  );

  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message = (body.error && body.error.message) || "Could not send the email";
    const status = String(message).includes("TOO_MANY") ? 429 : 400;
    throw new HttpError(status, message);
  }

  return { ok: true, email: body.email || "" };
}

module.exports = { sendVerificationEmail, CONTINUE_URL };
