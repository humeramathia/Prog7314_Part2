const { blobUrl, blobBaseUrl, isBlobUrl } = require("../src/data/blobMedia");

test("blobUrl points at the public learn container", () => {
  const url = blobUrl("football-techniques.jpg");
  expect(url).toBe(`${blobBaseUrl()}/football-techniques.jpg`);
  expect(isBlobUrl(url)).toBe(true);
  expect(url).toMatch(/^https:\/\/st10276384media\.blob\.core\.windows\.net\/learn\//);
});

test("isBlobUrl rejects Wikipedia hotlinks", () => {
  expect(
    isBlobUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/x.jpg")
  ).toBe(false);
  expect(isBlobUrl(null)).toBe(false);
});
