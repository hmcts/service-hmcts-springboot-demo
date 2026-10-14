// The Entra sign-in round trip.
//
//   GET /auth/entra      redirect the browser to Entra
//   GET /auth/callback   Entra sends the browser back with a code; swap it for the ID token and check it
const express = require('express')
const { config } = require('../entra/config')
const { randomToken, authorizeUrl, exchangeCodeForIdToken, userFrom } = require('../entra/client')
const { validateIdToken, IdTokenRejected } = require('../entra/id-token')
const { escapeHtml, page } = require('../views/page')

// One per value the callback must check. All three are single-use and die with the round trip.
const STATE_COOKIE = 'entra_state'
const NONCE_COOKIE = 'entra_nonce'
const PKCE_COOKIE = 'entra_pkce'
const FLOW_COOKIES = [STATE_COOKIE, NONCE_COOKIE, PKCE_COOKIE]

const router = express.Router()

// Step 1. Send the browser to Entra.
router.get('/auth/entra', (req, res) => {
  const flow = {
    // The CSRF defence for the round trip. Anyone can navigate a browser to /auth/callback, so the
    // cookie is what proves this browser is the one that started this flow.
    state: randomToken(),
    // Comes back inside the ID token, tying the token to this sign-in.
    nonce: randomToken(),
    // PKCE: only its hash goes to Entra now; the code can be redeemed only by whoever holds this.
    codeVerifier: randomToken()
  }
  // httpOnly keeps them from page scripts; ten minutes is plenty to type a password.
  const options = { httpOnly: true, sameSite: 'lax', secure: config.usesHttps, maxAge: 10 * 60 * 1000 }
  res.cookie(STATE_COOKIE, flow.state, options)
  res.cookie(NONCE_COOKIE, flow.nonce, options)
  res.cookie(PKCE_COOKIE, flow.codeVerifier, options)

  console.log('   redirecting to Entra  state=%s  prompt=%s', short(flow.state), req.query.prompt || 'none')
  res.redirect(authorizeUrl(flow, req.query.prompt))
})

// Step 2. Entra sends the browser back with a code. Step 3. Swap it for the ID token, server to server.
// Step 4. Check the token before believing anything in it.
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
  if (!code) {
    return res.status(400).send(page('No authorization code',
      '<p>Entra came back without a code. Start again from the home page.</p>'))
  }
  const nonce = req.cookies[NONCE_COOKIE]
  const codeVerifier = req.cookies[PKCE_COOKIE]
  FLOW_COOKIES.forEach(name => res.clearCookie(name))

  let idToken
  try {
    idToken = await exchangeCodeForIdToken(code, codeVerifier)
    console.log('   token exchange ok')
  } catch (err) {
    console.error('   token exchange failed: %s', err.message)
    return res.status(502).send(page('Token exchange failed', `<p class="detail">${escapeHtml(err.message)}</p>`))
  }

  let user
  try {
    user = userFrom(await validateIdToken(idToken, nonce))
    console.log('   id token valid')
  } catch (err) {
    if (!(err instanceof IdTokenRejected)) {
      console.error('   could not check the id token: %s', err.message)
      return res.status(502).send(page('Could not check the ID token', `<p class="detail">${escapeHtml(err.message)}</p>`))
    }
    console.error('   id token rejected: %s', err.message)
    return res.status(401).send(page('ID token rejected', `<p class="detail">${escapeHtml(err.message)}</p>`))
  }

  // The claims, and only the claims. The ID token itself is never logged.
  console.log('   claims  oid=%s  name=%s  email=%s',
    user.oid, user.name || '(not returned)', user.email || '(not returned)')

  res.send(page('Signed in', `
    <dl>
      <dt>oid</dt><dd><code>${escapeHtml(user.oid)}</code></dd>
      <dt>name</dt><dd>${escapeHtml(user.name || '(not returned)')}</dd>
      <dt>email</dt><dd>${escapeHtml(user.email || '(not returned)')}</dd>
    </dl>
    <a class="button" href="/">Start again</a>`))
})

/** Enough of a random value to follow it through the log, not enough to replay it. */
function short (value) {
  return `${value.slice(0, 8)}...`
}

module.exports = router
