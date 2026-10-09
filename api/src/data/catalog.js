const { withSportMetrics } = require("./metrics");
const { seedEvents } = require("./events");
const { seedGuides } = require("./guides");

const SPORTS = [
  { id: "football", sportId: "football", name: "Football" },
  { id: "cricket", sportId: "cricket", name: "Cricket" },
  { id: "tennis", sportId: "tennis", name: "Tennis" },
  { id: "basketball", sportId: "basketball", name: "Basketball" },
  { id: "swimming", sportId: "swimming", name: "Swimming" },
  { id: "athletics", sportId: "athletics", name: "Athletics" }
].map(withSportMetrics);

const CATEGORIES = ["RULES", "TECHNIQUES", "TRAINING", "SAFETY"];

function seedDocuments(now = Date.now()) {
  return {
    sports: SPORTS,
    events: seedEvents(now),
    learning: seedGuides()
  };
}

module.exports = { SPORTS, CATEGORIES, seedDocuments };
