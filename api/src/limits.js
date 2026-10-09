const LIMITS = {
  displayName: 60,
  title: 80,
  location: 120,
  notes: 500,
  description: 1000
};

function clipped(value) {
  return String(value ?? "").trim();
}

function assertLength(value, max, field) {
  const text = clipped(value);
  if (text.length > max) {
    return `${field} must be ${max} characters or fewer`;
  }
  return null;
}

module.exports = { LIMITS, clipped, assertLength };
