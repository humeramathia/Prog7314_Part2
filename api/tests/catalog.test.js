const { CATEGORIES, SPORTS } = require("../src/data/catalog");

test("catalog exposes six sports and four learn categories", () => {
  expect(SPORTS).toHaveLength(6);
  expect(CATEGORIES).toEqual(["RULES", "TECHNIQUES", "TRAINING", "SAFETY"]);
});
