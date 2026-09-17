function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function createMemoryStore() {
  const users = new Map();
  const sports = new Map();
  const events = new Map();
  const performance = new Map();
  const learning = new Map();

  return {
    kind: "memory",

    async getUser(userId) {
      return users.has(userId) ? clone(users.get(userId)) : null;
    },
    async upsertUser(user) {
      users.set(user.userId, clone(user));
      return clone(user);
    },

    async listSports() {
      return [...sports.values()].map(clone);
    },
    async getSport(sportId) {
      return sports.has(sportId) ? clone(sports.get(sportId)) : null;
    },
    async upsertSport(sport) {
      sports.set(sport.sportId, clone(sport));
      return clone(sport);
    },

    async listEvents(sportId, from, to) {
      return [...events.values()]
        .filter((item) => item.sportId === sportId)
        .filter((item) => (from == null || item.startsAt >= from) && (to == null || item.startsAt < to))
        .sort((a, b) => a.startsAt - b.startsAt)
        .map(clone);
    },
    async getEvent(eventId) {
      return events.has(eventId) ? clone(events.get(eventId)) : null;
    },
    async upsertEvent(event) {
      events.set(event.id, clone(event));
      return clone(event);
    },
    async deleteEvent(eventId) {
      return events.delete(eventId);
    },

    async listPerformance(userId, sportId) {
      return [...performance.values()]
        .filter((item) => item.userId === userId && (!sportId || item.sportId === sportId))
        .sort((a, b) => b.recordedAt - a.recordedAt)
        .map(clone);
    },
    async getPerformance(sessionId) {
      return performance.has(sessionId) ? clone(performance.get(sessionId)) : null;
    },
    async upsertPerformance(session) {
      performance.set(session.id, clone(session));
      return clone(session);
    },
    async deletePerformance(sessionId) {
      return performance.delete(sessionId);
    },

    async listLearning(sportId, category) {
      return [...learning.values()]
        .filter((item) => item.sportId === sportId && (!category || item.category === category))
        .map(clone);
    },
    async getLearning(guideId) {
      return learning.has(guideId) ? clone(learning.get(guideId)) : null;
    },
    async upsertLearning(guide) {
      learning.set(guide.id, clone(guide));
      return clone(guide);
    }
  };
}

module.exports = { createMemoryStore };
