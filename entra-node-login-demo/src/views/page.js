// The two bits of HTML this demo needs.

function escapeHtml (value) {
  return String(value ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}

function page (heading, body) {
  return `<!doctype html><html lang="en"><head><meta charset="utf-8">
<title>${escapeHtml(heading)} - entra-node-login-demo</title><style>
body{font-family:system-ui,sans-serif;margin:4rem auto;max-width:34rem;padding:0 1rem;line-height:1.5}
h1{font-size:1.5rem} .lead{color:#505a5f} .detail{color:#505a5f;font-size:.9rem}
.button{display:inline-block;background:#00703c;color:#fff;padding:.6rem 1.2rem;border-radius:3px;text-decoration:none}
dl{display:grid;grid-template-columns:5rem 1fr;gap:.5rem 1rem;margin:2rem 0}
dt{font-weight:600} code{word-break:break-all}
</style></head><body><h1>${escapeHtml(heading)}</h1>${body}</body></html>`
}

module.exports = { escapeHtml, page }
