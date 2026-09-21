const { HttpError } = require("../src/httpError");

test("HttpError keeps the HTTP status for Express", () => {
  const err = new HttpError(400, "sportId is required");
  expect(err.status).toBe(400);
  expect(err.message).toBe("sportId is required");
});
