const { withSportMetrics } = require("./metrics");

const SPORTS = [
  { id: "football", sportId: "football", name: "Football" },
  { id: "cricket", sportId: "cricket", name: "Cricket" },
  { id: "tennis", sportId: "tennis", name: "Tennis" },
  { id: "basketball", sportId: "basketball", name: "Basketball" },
  { id: "swimming", sportId: "swimming", name: "Swimming" },
  { id: "athletics", sportId: "athletics", name: "Athletics" }
].map(withSportMetrics);

const CATEGORIES = ["RULES", "TECHNIQUES", "TRAINING", "SAFETY"];

function beginnerBody(sport, category) {
  switch (category) {
    case "RULES":
      return `${sport} beginners should learn the scoring system, playing area, and basic fouls first. Know when play starts and stops, and what counts as a legal action.`;
    case "TECHNIQUES":
      return `Start with stance, grip, and a repeatable movement. Practice slowly, then add speed once the pattern feels consistent.`;
    case "TRAINING":
      return `Train twice a week: one skill session and one fitness session. Keep a short log of what you practised and how it felt.`;
    case "SAFETY":
      return `Warm up, wear the right kit, and stop if you feel sharp pain. Hydrate, check the surface, and tell a coach about any injury.`;
    default:
      return "";
  }
}

function seedDocuments(now = Date.now()) {
  const day = 86_400_000;
  const events = [];
  const learning = [];

  SPORTS.forEach((sport, index) => {
    events.push({
      id: `${sport.id}-practice`,
      sportId: sport.id,
      title: `${sport.name} squad practice`,
      type: "PRACTICE",
      startsAt: now + day * (2 + index),
      location: "Campus courts",
      notes: "Bring kit and water. Warm-up starts 15 minutes early.",
      createdBy: "seed"
    });
    events.push({
      id: `${sport.id}-event`,
      sportId: sport.id,
      title: `${sport.name} inter-campus fixture`,
      type: "EVENT",
      startsAt: now + day * (9 + index),
      location: "Main stadium",
      notes: "Arrive 45 minutes before start. Team photo at the gate.",
      createdBy: "seed"
    });
    CATEGORIES.forEach((category) => {
      learning.push({
        id: `${sport.id}-${category.toLowerCase()}`,
        sportId: sport.id,
        category,
        title: `${sport.name} ${category.toLowerCase()}`,
        body: beginnerBody(sport.name, category)
      });
    });
  });

  return { sports: SPORTS, events, learning };
}

module.exports = { SPORTS, CATEGORIES, seedDocuments };
