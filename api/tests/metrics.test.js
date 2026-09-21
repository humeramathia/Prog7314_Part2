const { parseMetrics, primaryMetricKey, metricsFor } = require("../src/data/metrics");

test("basketball requires points and rebounds", () => {
  const parsed = parseMetrics("basketball", { metrics: { points: 12, rebounds: 4 } });
  expect(parsed.error).toBeUndefined();
  expect(parsed.primaryMetric).toBe("points");
  expect(parsed.metrics).toEqual({ points: 12, rebounds: 4 });
});

test("rejects a generic score field", () => {
  const parsed = parseMetrics("basketball", { score: 12 });
  expect(parsed.error).toMatch(/metrics.points/);
});

test("primary metric follows the sport schema", () => {
  expect(primaryMetricKey("swimming")).toBe("distance");
  expect(metricsFor("football").map((m) => m.key)).toEqual(["goals", "assists"]);
});
