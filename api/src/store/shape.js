function publicDoc(doc) {
  if (!doc) return doc;
  const { _rid, _self, _etag, _attachments, _ts, ...rest } = doc;
  return rest;
}

function publicList(docs) {
  return docs.map(publicDoc);
}

function parseMillis(value) {
  if (value == null || value === "") return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

function isoDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function inYearMonth(recordedAt, year, month) {
  const date = new Date(recordedAt);
  return date.getFullYear() === year && date.getMonth() === month - 1;
}

function monthByDate(sessions, year, month, metricKey, readValue) {
  return sessions
    .filter((session) => inYearMonth(session.recordedAt, year, month))
    .sort((a, b) => a.recordedAt - b.recordedAt)
    .map((session) => {
      const date = new Date(session.recordedAt);
      return {
        date: isoDate(date),
        day: date.getDate(),
        recordedAt: session.recordedAt,
        sessionId: session.id,
        value: readValue(session, metricKey)
      };
    })
    .filter((point) => Number.isFinite(point.value));
}

module.exports = { publicDoc, publicList, parseMillis, isoDate, inYearMonth, monthByDate };
