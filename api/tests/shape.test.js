const { monthByDate, inYearMonth } = require("../src/store/shape");
const { metricValue } = require("../src/data/metrics");

test("monthByDate returns sessions in a year/month plotted by date", () => {
  const sessions = [
    { id: "a", recordedAt: new Date(2026, 8, 3).getTime(), metrics: { points: 18, rebounds: 7 } },
    { id: "b", recordedAt: new Date(2026, 8, 17).getTime(), metrics: { points: 24, rebounds: 11 } },
    { id: "c", recordedAt: new Date(2026, 7, 20).getTime(), metrics: { points: 30, rebounds: 9 } }
  ];
  const points = monthByDate(sessions, 2026, 9, "points", metricValue);
  expect(points).toHaveLength(2);
  expect(points[0]).toMatchObject({ date: "2026-09-03", day: 3, value: 18 });
  expect(points[1]).toMatchObject({ date: "2026-09-17", day: 17, value: 24 });
  expect(inYearMonth(sessions[2].recordedAt, 2026, 9)).toBe(false);
});
