const SPORT_METRICS = {
  football: [
    { key: "goals", label: "Goals" },
    { key: "assists", label: "Assists" }
  ],
  cricket: [
    { key: "runs", label: "Runs" },
    { key: "wickets", label: "Wickets" }
  ],
  tennis: [
    { key: "aces", label: "Aces" },
    { key: "winners", label: "Winners" }
  ],
  basketball: [
    { key: "points", label: "Points" },
    { key: "rebounds", label: "Rebounds" }
  ],
  swimming: [
    { key: "distance", label: "Distance (m)" },
    { key: "time", label: "Time (s)" }
  ],
  athletics: [
    { key: "distance", label: "Distance (m)" },
    { key: "time", label: "Time (s)" }
  ]
};

function metricsFor(sportId) {
  return SPORT_METRICS[sportId] || [];
}

function primaryMetricKey(sportId) {
  return metricsFor(sportId)[0]?.key || null;
}

function withSportMetrics(sport) {
  if (!sport) return sport;
  return { ...sport, metrics: metricsFor(sport.sportId || sport.id) };
}

function parseMetrics(sportId, body) {
  const schema = metricsFor(sportId);
  if (!schema.length) {
    return { error: "Unknown sportId" };
  }
  const source = body.metrics && typeof body.metrics === "object" ? body.metrics : body;
  const metrics = {};
  for (const field of schema) {
    const value = Number(source[field.key]);
    if (!Number.isFinite(value)) {
      return { error: `metrics.${field.key} is required` };
    }
    metrics[field.key] = value;
  }
  return { metrics, primaryMetric: schema[0].key, primaryValue: metrics[schema[0].key] };
}

function metricValue(session, key) {
  if (session?.metrics && Number.isFinite(Number(session.metrics[key]))) {
    return Number(session.metrics[key]);
  }
  if (key === "score" && Number.isFinite(Number(session?.score))) {
    return Number(session.score);
  }
  return null;
}

module.exports = {
  SPORT_METRICS,
  metricsFor,
  primaryMetricKey,
  withSportMetrics,
  parseMetrics,
  metricValue
};
