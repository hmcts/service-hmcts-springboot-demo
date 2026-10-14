// Everything that talks to Entra, kept away from the routing.
const crypto = require('node:crypto')
const { config } = require('./config')

/** A random URL-safe value: 32 bytes is 43 characters, the RFC 7636 minimum for a PKCE verifier. */
function randomToken () {
  return crypto.randomBytes(32).toString('base64url')
}

/** PKCE S256: the challenge goes to /authorize, the verifier stays with us until the code is redeemed. */
function pkceChallenge (verifier) {
  return crypto.createHash('sha256').update(verifier).digest('base64url')
}

/**
 * The address to send the browser to. response_type=code is the whole security design: the browser
 * carries a single-use authorization code, never a token.
 *
 * `prompt` is for local demos only - it makes Microsoft show its form even when this browser already
 * has a session, which is otherwise why sign-in "just works" with no way to try another account.
 */
function authorizeUrl ({ state, nonce, codeVerifier }, prompt) {
  const url = new URL(config.authorizeEndpoint)
  url.searchParams.set('client_id', config.clientId)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('redirect_uri', config.redirectUri)
  url.searchParams.set('response_mode', 'query')
  url.searchParams.set('scope', config.scope)
  url.searchParams.set('state', state)
  url.searchParams.set('nonce', nonce)
  url.searchParams.set('code_challenge', pkceChallenge(codeVerifier))
  url.searchParams.set('code_challenge_method', 'S256')
  if (['login', 'select_account', 'create'].includes(prompt)) {
    url.searchParams.set('prompt', prompt)
  }
  return url.toString()
}

/**
 * The back channel: this app to Microsoft, over TLS, with the client secret and the PKCE verifier.
 * A stolen code is useless without both. No browser involved.
 */
async function exchangeCodeForIdToken (code, codeVerifier) {
  const response = await fetch(config.tokenEndpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: config.clientId,
      client_secret: config.clientSecret,
      code,
      code_verifier: codeVerifier,
      redirect_uri: config.redirectUri,
      grant_type: 'authorization_code',
      scope: config.scope
    })
  })
  const body = await response.json()
  if (!response.ok) throw new Error(JSON.stringify(body))
  return body.id_token
}

/** The three claims this demo shows, from a token validateIdToken has already accepted. */
function userFrom (claims) {
  return {
    oid: claims.oid,
    name: claims.name,
    // External ID returns an array in some configurations.
    email: claims.email || (claims.emails && claims.emails[0])
  }
}

module.exports = { randomToken, authorizeUrl, exchangeCodeForIdToken, userFrom }
