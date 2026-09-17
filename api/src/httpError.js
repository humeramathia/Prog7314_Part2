class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function asyncHandler(fn) {
  return (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);
}

function errorHandler(err, req, res, next) {
  const status = err.status || 500;
  const publicMessage = status === 500 ? "Server error" : err.message;
  if (status >= 500) {
    console.error(err);
  }
  res.status(status).json({ error: publicMessage });
}

module.exports = { HttpError, asyncHandler, errorHandler };
