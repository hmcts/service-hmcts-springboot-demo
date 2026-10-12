// Everything that talks to Entra, kept away from the routing.
const { config } = require('./config')

/**
 * The address to send the browser to. response_type=code is the whole security design: the browser
 * carries a single-use authorization code, never a token.
 *
 * `prompt` is for local demos only - it makes Microsoft show its form even when this browser already
 * has a session, which is otherwise why sign-in "just works" with no way to try another account.
 */
function authorizeUrl (state, prompt) {
  const url = new URL(config.authorizeEndpoint)
  url.searchParams.set('client_id', config.clientId)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('redirect_uri', config.redirectUri)
  url.searchParams.set('response_mode', 'query')
  url.searchParams.set('scope', config.scope)
  url.searchParams.set('state', state)
  if (['login', 'select_account', 'create'].includes(prompt)) {
    url.searchParams.set('prompt', prompt)
  }
  return url.toString()
}

/** The back channel: this app to Microsoft, over TLS, with the client secret. No browser involved. */
async function exchangeCodeForIdToken (code) {
  const response = await fetch(config.tokenEndpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: config.clientId,
      client_secret: config.clientSecret,
      code,
      redirect_uri: config.redirectUri,
      grant_type: 'authorization_code',
      scope: config.scope
    })
  })
  const body = await response.json()
  if (!response.ok) throw new Error(JSON.stringify(body))
  return body.id_token
}

/**
 * Reads the claims out of the ID token WITHOUT checking its signature.
 *
 * Safe only here: this token came straight off a TLS connection to Entra's token endpoint,
 * authenticated with our own client secret, so nobody was in a position to substitute one. A token
 * arriving from anywhere else - a browser, an API caller - must be verified against the tenant's
 * JWKS first, because a decoded-but-unverified JWT is forgeable by anyone who can type base64.
 */
function claimsFrom (idToken) {
  const claims = JSON.parse(Buffer.from(idToken.split('.')[1], 'base64url').toString('utf8'))
  return {
    oid: claims.oid,
    name: claims.name,
    // External ID returns an array in some configurations.
    email: claims.email || (claims.emails && claims.emails[0])
  }
}

module.exports = { authorizeUrl, exchangeCodeForIdToken, claimsFrom }
