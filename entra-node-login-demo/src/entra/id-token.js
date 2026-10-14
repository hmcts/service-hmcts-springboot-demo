// Checks an ID token before a single claim in it is believed: signature first, then the claims that
// say it was issued by our tenant, for this app, for this sign-in, and is still in date.
const crypto = require('node:crypto')
const { config } = require('./config')

// Entra's clock and ours will not agree to the second.
const CLOCK_SKEW_SECONDS = 300

class IdTokenRejected extends Error {}

// kid -> KeyObject. Entra rotates its keys, so an unknown kid triggers one refetch before giving up.
let keys = new Map()

async function fetchKeys () {
  const response = await fetch(config.jwksUri)
  if (!response.ok) throw new Error(`fetching signing keys from ${config.jwksUri}: HTTP ${response.status}`)
  const { keys: jwks } = await response.json()
  keys = new Map(jwks
    .filter(jwk => jwk.kty === 'RSA' && (!jwk.use || jwk.use === 'sig'))
    .map(jwk => [jwk.kid, crypto.createPublicKey({ key: jwk, format: 'jwk' })]))
}

async function keyFor (kid) {
  if (!keys.has(kid)) await fetchKeys()
  const key = keys.get(kid)
  if (!key) throw new IdTokenRejected(`signed with kid ${kid}, which is not one of the tenant's published keys`)
  return key
}

function decode (segment) {
  return JSON.parse(Buffer.from(segment, 'base64url').toString('utf8'))
}

/** Returns the token's claims, or throws IdTokenRejected saying which check failed. */
async function validateIdToken (idToken, expectedNonce) {
  const parts = String(idToken).split('.')
  if (parts.length !== 3) throw new IdTokenRejected('not a JWT')
  const [headerSegment, payloadSegment, signature] = parts

  let header, claims
  try {
    header = decode(headerSegment)
    claims = decode(payloadSegment)
  } catch {
    throw new IdTokenRejected('not a JWT')
  }

  // Pinned, never taken from the token: alg=none or an HMAC alg keyed with the public key would
  // otherwise let anyone sign their own.
  if (header.alg !== 'RS256') throw new IdTokenRejected(`alg ${header.alg} is not RS256`)

  const valid = crypto.verify('RSA-SHA256', Buffer.from(`${headerSegment}.${payloadSegment}`),
    await keyFor(header.kid), Buffer.from(signature, 'base64url'))
  if (!valid) throw new IdTokenRejected('signature does not verify')

  const now = Math.floor(Date.now() / 1000)
  if (claims.iss !== config.issuer) throw new IdTokenRejected(`iss ${claims.iss} is not ${config.issuer}`)
  if (claims.aud !== config.clientId) throw new IdTokenRejected(`aud ${claims.aud} is not this app`)
  if (claims.tid !== config.tenantId) throw new IdTokenRejected(`tid ${claims.tid} is not this tenant`)
  if (typeof claims.exp !== 'number' || claims.exp + CLOCK_SKEW_SECONDS < now) {
    throw new IdTokenRejected('expired')
  }
  if (typeof claims.nbf === 'number' && claims.nbf - CLOCK_SKEW_SECONDS > now) {
    throw new IdTokenRejected('not yet valid')
  }
  // Ties the token to the sign-in this browser started, so a token from another one cannot be replayed in.
  if (!expectedNonce || claims.nonce !== expectedNonce) {
    throw new IdTokenRejected('nonce does not match this sign-in')
  }

  return claims
}

module.exports = { validateIdToken, IdTokenRejected }
