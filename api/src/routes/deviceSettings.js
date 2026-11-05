/**
 * Per-user device settings stored on the profile document:
 *  - fcmToken: the phone's Firebase Cloud Messaging registration token
 *  - notificationsEnabled: the Settings → Notifications switch
 *  - language: the Settings → Language choice (en, af or xh)
 *
 * Kept in its own module so `routes/me.js` only needs one call.
 */

/** Languages the Android app ships translations for. */
const LANGUAGES = new Set(["en", "af", "xh"]);

/** FCM tokens are ~160 characters today; allow generous headroom. */
const MAX_TOKEN_LENGTH = 4096;

/**
 * Validates and applies the optional device fields from a PATCH body.
 * Returns an error message, or null when everything was valid.
 * Sending `fcmToken: ""` removes the token (used when the user logs out
 * or switches notifications off).
 */
function applyDeviceSettings(user, body) {
  const { fcmToken, notificationsEnabled, language } = body;

  if (fcmToken !== undefined && fcmToken !== null) {
    if (typeof fcmToken !== "string") return "fcmToken must be a string";
    const token = fcmToken.trim();
    if (token.length > MAX_TOKEN_LENGTH) return "fcmToken is too long";
    if (token) {
      user.fcmToken = token;
      user.fcmTokenUpdatedAt = Date.now();
    } else {
      delete user.fcmToken;
      delete user.fcmTokenUpdatedAt;
    }
  }

  if (notificationsEnabled !== undefined && notificationsEnabled !== null) {
    user.notificationsEnabled = Boolean(notificationsEnabled);
  }

  if (language !== undefined && language !== null) {
    const code = String(language).trim().toLowerCase();
    if (!LANGUAGES.has(code)) return "language must be one of en, af, xh";
    user.language = code;
  }

  return null;
}

/**
 * Profile as returned to the app. The raw push token is never echoed
 * back; the app only needs to know whether one is registered.
 */
function publicUser(user) {
  if (!user) return user;
  const { fcmToken, fcmTokenUpdatedAt, ...rest } = user;
  return {
    ...rest,
    language: rest.language || "en",
    notificationsEnabled: rest.notificationsEnabled !== false,
    hasDeviceToken: Boolean(fcmToken)
  };
}

/** Message sent by POST /api/me/test-notification, in the user's language. */
function testMessage(user) {
  const name = user.displayName || "athlete";
  const copy = {
    en: { title: "SportSphere test", body: `Hi ${name}, push notifications are working.` },
    af: { title: "SportSphere-toets", body: `Hallo ${name}, stootkennisgewings werk.` },
    xh: { title: "Uvavanyo lweSportSphere", body: `Molo ${name}, izaziso ziyasebenza.` }
  };
  const text = copy[user.language] || copy.en;
  return { ...text, data: { kind: "test" } };
}

module.exports = { LANGUAGES, applyDeviceSettings, publicUser, testMessage };
