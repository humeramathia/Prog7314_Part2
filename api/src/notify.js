/**
 * Push notifications through Firebase Cloud Messaging (FCM).
 *
 * The API sends two kinds of message:
 *  - Topic messages: every phone subscribed to `sport_<sportId>` hears
 *    about a new or changed fixture for that sport.
 *  - Token messages: one phone only, used by the Settings "Send test
 *    notification" button so the demo can prove the full round trip.
 *
 * Sending never blocks or fails an API request. If Firebase Admin is not
 * configured (local dev, tests) the notifier reports `{ sent: false }`.
 *
 * Reference: Google (2025) *Send messages to topics* and *Build app server
 * send requests*, Firebase Cloud Messaging documentation.
 * Available at: https://firebase.google.com/docs/cloud-messaging/send-message
 */

/** Android notification channel the app creates for fixture alerts. */
const FIXTURE_CHANNEL = "fixtures";

const TYPE_LABELS = {
  PRACTICE: "Practice",
  SOCIAL_EVENT: "Event",
  ANNOUNCEMENT: "Announcement"
};

/**
 * FCM topic for a sport. Must match `PushTopics.forSport` in the Android app.
 * FCM topic names may only use `[a-zA-Z0-9-_.~%]`.
 */
function topicForSport(sportId) {
  const safe = String(sportId || "")
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_.~-]/g, "_");
  return safe ? `sport_${safe}` : "";
}

function formatWhen(startsAt) {
  try {
    return new Date(startsAt).toLocaleString("en-ZA", {
      timeZone: "Africa/Johannesburg",
      weekday: "short",
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit"
    });
  } catch {
    return "";
  }
}

/** Builds the title/body/data for a fixture message. Pure, so it is unit tested. */
function fixtureMessage(event, sportName, action = "created") {
  const label = TYPE_LABELS[event.type] || "Fixture";
  const verb = action === "updated" ? "updated" : "added";
  const where = event.location ? ` at ${event.location}` : "";
  return {
    title: `${sportName || "SportSphere"}: ${label} ${verb}`,
    body: `${event.title} — ${formatWhen(event.startsAt)}${where}`.trim(),
    data: {
      kind: "fixture",
      action,
      eventId: String(event.id || ""),
      sportId: String(event.sportId || "")
    }
  };
}

function androidOptions() {
  return { priority: "high", notification: { channelId: FIXTURE_CHANNEL } };
}

/**
 * Real notifier backed by firebase-admin. `getMessaging` returns
 * `admin.messaging()` or null when Firebase is not configured.
 */
function createFcmNotifier(getMessaging) {
  async function send(target, message) {
    const messaging = getMessaging();
    if (!messaging) {
      console.log("[notify] FCM not configured; skipped", Object.keys(target)[0]);
      return { sent: false, reason: "fcm-not-configured" };
    }
    const id = await messaging.send({
      ...target,
      notification: { title: message.title, body: message.body },
      data: message.data || {},
      android: androidOptions()
    });
    console.log(`[notify] sent ${id} to ${target.topic ? `topic ${target.topic}` : "device token"}`);
    return { sent: true, id };
  }

  return {
    kind: "fcm",
    toTopic: (topic, message) => send({ topic }, message),
    toToken: (token, message) => send({ token }, message)
  };
}

/** Records messages instead of sending them. Used by the Jest suite. */
function createMemoryNotifier() {
  const sent = [];
  const record = (target) => async (address, message) => {
    sent.push({ [target]: address, ...message });
    return { sent: true, id: `memory-${sent.length}` };
  };
  return {
    kind: "memory",
    sent,
    toTopic: record("topic"),
    toToken: record("token")
  };
}

/**
 * Fire-and-forget fixture alert. Errors are logged, never thrown, so a
 * push problem can never stop a fixture from being saved.
 */
function notifyFixture(notifier, event, sportName, action) {
  const topic = topicForSport(event.sportId);
  if (!notifier || !topic) return Promise.resolve({ sent: false });
  return notifier
    .toTopic(topic, fixtureMessage(event, sportName, action))
    .catch((error) => {
      console.error("[notify] fixture push failed:", error.message);
      return { sent: false, reason: error.message };
    });
}

module.exports = {
  FIXTURE_CHANNEL,
  topicForSport,
  fixtureMessage,
  createFcmNotifier,
  createMemoryNotifier,
  notifyFixture
};
