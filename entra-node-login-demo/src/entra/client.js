// Everything that talks to Entra, kept away from the routing.
const { createRemoteJWKSet, jwtVerify } = require('jose')
const { config } = require('./config')

/**
 * The address to send the browser to. response_type=code is the whole security design: the browser
 * carries a single-use authorization code, never a token.
 *
 * `prompt` is for local demos only - it makes Microsoft show its form even when this browser already
 * has a session, which is otherwise why sign-in "just works" with no way to try another account.
 */
function authorizeUrl (state, nonce, prompt) {
  const url = new URL(config.authorizeEndpoint)
  url.searchParams.set('client_id', config.clientId)
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('redirect_uri', config.redirectUri)
  url.searchParams.set('response_mode', 'query')
  url.searchParams.set('scope', config.scope)
  url.searchParams.set('state', state)
  url.searchParams.set('nonce', nonce)
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
 * Verifies the ID token and reads the claims out of it. Nothing in the token is believed until all of
 * this has passed:
 *
 *   signature  made by one of the tenant's published keys (looked up by the token's `kid`)
 *   alg        RS256 only - never what the token's own header asks for, which is how `alg: none` and
 *              HS256 key-confusion forgeries get through
 *   iss        this tenant, as its discovery document names it
 *   aud        this app's client ID - a token issued to some other app is not for us
 *   exp, nbf   in date, with a minute of clock skew allowed
 *   nonce      the one this browser was given at /auth/entra, so a token from another sign-in cannot
 *              be replayed into this one
 *
 * It arrived over TLS from the token endpoint, so the signature is arguably belt and braces here
 * (OIDC Core 3.1.3.7 allows TLS in its place). It is checked anyway: it costs nothing once the keys
 * are cached, and it means this function is safe wherever the token came from.
 */
async function verifiedClaims (idToken, expectedNonce) {
  const { issuer, keys } = await tenant()
  const { payload } = await jwtVerify(idToken, keys, {
    algorithms: ['RS256'],
    issuer,
    audience: config.clientId,
    clockTolerance: 60
  })
  if (!expectedNonce || payload.nonce !== expectedNonce) throw new Error('nonce does not match this sign-in')

  return {
    oid: payload.oid,
    name: payload.name,
    // External ID returns an array in some configurations.
    email: payload.email || (payload.emails && payload.emails[0])
  }
}

/**
 * The tenant's issuer and signing keys, fetched once from its discovery document. The key set is
 * jose's remote JWKS, which caches the keys and refetches when a token names a `kid` it has not seen -
 * which is what happens when Entra rotates them. A failed fetch is not cached, so the next sign-in
 * tries again.
 */
let tenantPromise
function tenant () {
  tenantPromise ??= fetchTenant().catch(err => {
    tenantPromise = undefined
    throw err
  })
  return tenantPromise
}

async function fetchTenant () {
  const response = await fetch(config.discoveryEndpoint)
  if (!response.ok) throw new Error(`discovery failed: ${response.status} from ${config.discoveryEndpoint}`)
  const { issuer, jwks_uri: jwksUri } = await response.json()
  return { issuer, keys: createRemoteJWKSet(new URL(jwksUri)) }
}

module.exports = { authorizeUrl, exchangeCodeForIdToken, verifiedClaims }
