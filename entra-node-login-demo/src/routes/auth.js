// The Entra sign-in round trip.
//
//   GET /auth/entra      redirect the browser to Entra
//   GET /auth/callback   Entra sends the browser back with a code; swap it for the ID token
const crypto = require('node:crypto')
const express = require('express')
const { authorizeUrl, exchangeCodeForIdToken, claimsFrom } = require('../entra/client')
const { escapeHtml, page } = require('../views/page')

const STATE_COOKIE = 'entra_state'

const router = express.Router()

// Step 1. Send the browser to Entra.
router.get('/auth/entra', (req, res) => {
  // The CSRF defence for the round trip. Anyone can navigate a browser to /auth/callback, so the
  // cookie is what proves this browser is the one that started this flow.
  const state = crypto.randomBytes(16).toString('hex')
  res.cookie(STATE_COOKIE, state, { httpOnly: true, sameSite: 'lax' })

  console.log('   redirecting to Entra  state=%s  prompt=%s', short(state), req.query.prompt || 'none')
  res.redirect(authorizeUrl(state, req.query.prompt))
})

// Step 2. Entra sends the browser back with a code. Step 3. Swap it for the ID token, server to server.
router.get('/auth/callback', async (req, res) => {
  const { code, state, error, error_description: errorDescription } = req.query

  console.log('   back from Entra  code=%s  state=%s  error=%s',
    code ? 'present' : 'MISSING', state ? short(state) : 'MISSING', error || 'none')

  if (error) {
    console.error('   Entra refused: %s - %s', error, errorDescription || '(no description)')
    return res.status(400).send(page('Entra returned an error', `<p>${escapeHtml(error)}</p>
      <p class="detail">${escapeHtml(errorDescription || '')}</p>`))
  }
  if (!state || state !== req.cookies[STATE_COOKIE]) {
    console.error('   state mismatch  query=%s  cookie=%s',
      state ? short(state) : 'MISSING',
      req.cookies[STATE_COOKIE] ? short(req.cookies[STATE_COOKIE]) : 'MISSING')
    return res.status(400).send(page('State mismatch',
      '<p>Possible CSRF, or an expired or replayed link. Start again from the home page.</p>'))
  }
  res.clearCookie(STATE_COOKIE)

  let claims
  try {
    claims = claimsFrom(await exchangeCodeForIdToken(code))
    console.log('   token exchange ok')
  } catch (err) {
    console.error('   token exchange failed: %s', err.message)
    return res.status(502).send(page('Token exchange failed', `<p class="detail">${escapeHtml(err.message)}</p>`))
  }

  // The claims, and only the claims. The ID token itself is never logged.
  console.log('   claims  oid=%s  name=%s  email=%s',
    claims.oid, claims.name || '(not returned)', claims.email || '(not returned)')

  res.send(page('Signed in', `
    <dl>
      <dt>oid</dt><dd><code>${escapeHtml(claims.oid)}</code></dd>
      <dt>name</dt><dd>${escapeHtml(claims.name || '(not returned)')}</dd>
      <dt>email</dt><dd>${escapeHtml(claims.email || '(not returned)')}</dd>
    </dl>
    <a class="button" href="/">Start again</a>`))
})

/** Enough of a random value to follow it through the log, not enough to replay it. */
function short (value) {
  return `${value.slice(0, 8)}...`
}

module.exports = router
