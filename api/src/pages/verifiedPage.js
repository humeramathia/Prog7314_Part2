function verifiedPage() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>SportSphere email verified</title>
  <style>
    body { font-family: Segoe UI, sans-serif; background: #F8FAFC; color: #0F172A; margin: 0; padding: 32px 20px; }
    main { max-width: 28rem; margin: 0 auto; background: #fff; border: 1px solid #E2E8F0; border-radius: 16px; padding: 24px; }
    h1 { color: #0F766E; font-size: 1.4rem; }
    p { color: #64748B; line-height: 1.5; }
    .ok { color: #0F766E; font-weight: 600; }
  </style>
</head>
<body>
  <main>
    <h1>SportSphere</h1>
    <p class="ok">Email verified.</p>
    <p>Return to SportSphere and tap I have confirmed.</p>
  </main>
</body>
</html>`;
}

module.exports = { verifiedPage };
