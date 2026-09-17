const { monthlyAverages } = require("../src/store/shape");

test("monthlyAverages groups scores by month", () => {
  const sessions = [
    { recordedAt: new Date(2026, 0, 10).getTime(), score: 50 },
    { recordedAt: new Date(2026, 0, 20).getTime(), score: 70 },
    { recordedAt: new Date(2026, 2, 1).getTime(), score: 90 }
  ];
  const points = monthlyAverages(sessions, 2026);
  expect(points[0]).toEqual({ month: "Jan", average: 60 });
  expect(points[2]).toEqual({ month: "Mar", average: 90 });
  expect(points[1].average).toBe(0);
});
