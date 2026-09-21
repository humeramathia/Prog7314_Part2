function verifyEmailPage(fallbackApiKey) {
  const key = String(fallbackApiKey || "").replace(/</g, "");
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>SportSphere email verification</title>
  <style>
    body { font-family: Segoe UI, sans-serif; background: #F8FAFC; color: #0F172A; margin: 0; padding: 32px 20px; }
    main { max-width: 28rem; margin: 0 auto; background: #fff; border: 1px solid #E2E8F0; border-radius: 16px; padding: 24px; }
    h1 { color: #0F766E; font-size: 1.4rem; }
    p { color: #64748B; line-height: 1.5; }
    .ok { color: #0F766E; font-weight: 600; }
    .err { color: #DC2626; }
  </style>
</head>
<body>
  <main>
    <h1>SportSphere</h1>
    <p id="status">Confirming your email…</p>
  </main>
  <script>
    const fallbackKey = ${JSON.stringify(key)};
    const params = new URLSearchParams(window.location.search);
    const hashParams = new URLSearchParams(window.location.hash.replace(/^#/, ""));
    const mode = params.get("mode") || hashParams.get("mode");
    const oobCode = params.get("oobCode") || params.get("oobcode") || hashParams.get("oobCode");
    const apiKey = params.get("apiKey") || hashParams.get("apiKey") || fallbackKey;
    const status = document.getElementById("status");

    function show(text, cls) {
      status.className = cls || "";
      status.textContent = text;
    }

    if (mode && mode !== "verifyEmail") {
      show("This link is not an email verification link.", "err");
    } else if (!oobCode) {
      show("Gmail opened a stripped link. Long-press the button in the email, copy the link, and paste it into Chrome. Then return to SportSphere and tap I have confirmed.", "err");
    } else if (!apiKey) {
      show("The verification link is missing the Firebase API key. Open the app, tap Resend, or use Continue with Google.", "err");
    } else {
      fetch("https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + encodeURIComponent(apiKey), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ oobCode: oobCode })
      }).then(function (res) { return res.json().then(function (body) { return { ok: res.ok, body: body }; }); })
        .then(function (result) {
          if (result.ok) {
            show("Email verified. Return to SportSphere and tap I have confirmed.", "ok");
          } else {
            show((result.body && result.body.error && result.body.error.message) || "Could not verify this link. Request a new email in the app.", "err");
          }
        }).catch(function () {
          show("Could not reach Firebase. Check your connection and try the link in Chrome.", "err");
        });
    }
  </script>
</body>
</html>`;
}

module.exports = { verifyEmailPage };
