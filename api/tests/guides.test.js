const { seedDocuments } = require("../src/data/catalog");

test("learn guides are unique per sport and not a shared template", () => {
  const { learning } = seedDocuments();
  const titles = learning.map((guide) => guide.title);
  expect(new Set(titles).size).toBe(titles.length);

  const bodies = learning.map((guide) => guide.body);
  expect(new Set(bodies).size).toBe(bodies.length);

  const footballRules = learning.find((guide) => guide.id === "football-rules");
  const cricketRules = learning.find((guide) => guide.id === "cricket-rules");
  expect(footballRules.body).toMatch(/offside/i);
  expect(cricketRules.body).toMatch(/LBW/i);
  expect(footballRules.body).not.toBe(cricketRules.body);
});

test("safety guides use sport-specific video links instead of Big Buck Bunny", () => {
  const { learning } = seedDocuments();
  const payload = JSON.stringify(learning);
  expect(payload).not.toMatch(/BigBuckBunny/i);
  expect(payload).not.toMatch(/picsum\.photos/i);

  const safety = learning.filter((guide) => guide.category === "SAFETY");
  expect(safety).toHaveLength(6);
  expect(safety.every((guide) => /youtube\.com/.test(guide.mediaUrl || ""))).toBe(true);
  expect(new Set(safety.map((guide) => guide.mediaUrl)).size).toBe(safety.length);
});

test("seed events are sport-specific and more than three generic rows", () => {
  const { events } = seedDocuments();
  const football = events.filter((event) => event.sportId === "football");
  expect(football.length).toBeGreaterThanOrEqual(4);
  expect(football.map((event) => event.title).join(" ")).toMatch(/derby|set-piece|referee/i);
  expect(football.every((event) => event.createdBy === "seed")).toBe(true);

  const cricket = events.filter((event) => event.sportId === "cricket");
  expect(cricket.some((event) => /T20|nets|pitch/i.test(event.title))).toBe(true);
});
