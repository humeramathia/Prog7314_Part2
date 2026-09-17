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

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

function monthlyAverages(sessions, year) {
  const grouped = Array.from({ length: 12 }, () => []);
  sessions.forEach((session) => {
    const date = new Date(session.recordedAt);
    if (date.getFullYear() !== year) return;
    grouped[date.getMonth()].push(session.score);
  });
  return MONTHS.map((month, index) => {
    const values = grouped[index];
    const average = values.length ? values.reduce((sum, n) => sum + n, 0) / values.length : 0;
    return { month, average: Math.round(average * 10) / 10 };
  });
}

module.exports = { publicDoc, publicList, parseMillis, monthlyAverages };
