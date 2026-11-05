/**
 * Public Azure Blob Storage base for Learn images.
 *
 * Account: st10276384media
 * Container: learn (anonymous blob read)
 * Microsoft, [s.a.]. *Introduction to Azure Blob Storage*.
 */
function blobBaseUrl() {
  const fromEnv = process.env.AZURE_BLOB_BASE_URL;
  const fallback = "https://st10276384media.blob.core.windows.net/learn";
  return String(fromEnv || fallback).replace(/\/$/, "");
}

function blobUrl(fileName) {
  return `${blobBaseUrl()}/${fileName}`;
}

function isBlobUrl(url) {
  return typeof url === "string" && url.includes(".blob.core.windows.net/");
}

module.exports = { blobBaseUrl, blobUrl, isBlobUrl };
