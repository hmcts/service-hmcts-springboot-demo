// Drives the app the way a browser does: start the round trip, carry the cookie, come back with the
// code. Entra is a stub HTTP server, so the real fetches in entra/client.js - token, discovery and
// keys - are exercised rather than mocked away. Its ID tokens are really signed, with a key pair made
// for this run, so the app's signature check is exercised too.
const { before, after, beforeEach, describe, test } = require('node:test')
const assert = require('node:assert/strict')
const http = require('node:http')
const { SignJWT, exportJWK, generateKeyPair } = require('jose')
const { assertConfigured } = require('../src/entra/config')

const CLAIMS = {
  oid: '11111111-2222-3333-4444-555555555555',
  name: 'Ada Lovelace',
  // An array, not a string: External ID returns emails this way in some configurations.
  emails: ['ada.lovelace@hmcts.net']
}

const CLIENT_ID = 'demo-client'
const KID = 'stub-key-1'

let entra
let issuer
let entraKey
let request
let app

// What the stub token endpoint answers with. Each test that wants something other than a good token
// replaces it; beforeEach puts the good one back.
let issueIdToken

/** A token exactly as Entra would issue it for this sign-in. Each override breaks one thing. */
function goodIdToken ({ nonce }, overrides = {}) {
  const {
    key = entraKey.privateKey,
    alg = 'RS256',
    iss = issuer,
    aud = CLIENT_ID,
    exp = '1h',
    claims = CLAIMS
  } = overrides
  return new SignJWT({ ...claims, nonce })
    .setProtectedHeader({ alg, kid: KID })
    .setIssuer(iss)
    .setAudience(aud)
    .setIssuedAt()
    .setExpirationTime(exp)
    .sign(key)
}

/** The three Entra endpoints this app calls server to server. */
function startStubEntra () {
  return new Promise(resolve => {
    const server = http.createServer(async (req, res) => {
      const json = body => {
        res.writeHead(200, { 'Content-Type': 'application/json' })
        res.end(JSON.stringify(body))
      }
      if (req.url === '/tenant/v2.0/.well-known/openid-configuration') {
        return json({ issuer, jwks_uri: `${issuer.replace('/v2.0', '')}/discovery/v2.0/keys` })
      }
      if (req.url === '/tenant/discovery/v2.0/keys') {
        const jwk = await exportJWK(entraKey.publicKey)
        return json({ keys: [{ ...jwk, kid: KID, use: 'sig', alg: 'RS256' }] })
      }
      if (req.url === '/tenant/oauth2/v2.0/token') {
        return json({ id_token: await issueIdToken(), expires_in: 3600 })
      }
      res.writeHead(404).end()
    })
    server.listen(0, () => resolve(server))
  })
}

before(async () => {
  entraKey = await generateKeyPair('RS256')
  entra = await startStubEntra()
  issuer = `http://localhost:${entra.address().port}/tenant/v2.0`

  // Set before the app is required: src/entra/config.js reads the environment once, on load.
  process.env.ENTRA_CLIENT_ID = CLIENT_ID
  process.env.ENTRA_CLIENT_SECRET = 'demo-secret'
  process.env.ENTRA_REDIRECT_URI = 'http://localhost:3000/auth/callback'
  process.env.ENTRA_AUTHORIZE_ENDPOINT = 'https://login.example/authorize'
  process.env.ENTRA_TOKEN_ENDPOINT = `http://localhost:${entra.address().port}/tenant/oauth2/v2.0/token`

  request = require('supertest')
  app = require('../src/app').buildApp()
})

after(() => {
  // fetch keeps its connections alive, and close() alone would wait for them.
  entra.closeAllConnections()
  entra.close()
})

describe('entra sign-in', () => {
  test('visiting_the_home_page_should_offer_a_sign_in_link', async () => {
    const response = await request(app).get('/').expect(200)

    assert.match(response.text, /href="\/auth\/entra/)
  })

  test('starting_sign_in_should_redirect_to_entra_with_a_state_cookie', async () => {
    const response = await request(app).get('/auth/entra').expect(302)

    const sentTo = new URL(response.headers.location)
    assert.equal(sentTo.origin + sentTo.pathname, 'https://login.example/authorize')
    // The browser must carry a single-use code, never a token.
    assert.equal(sentTo.searchParams.get('response_type'), 'code')
    assert.equal(sentTo.searchParams.get('client_id'), 'demo-client')
    assert.equal(sentTo.searchParams.get('scope'), 'openid profile email')
    assert.equal(sentTo.searchParams.get('state'), stateCookieFrom(response))
    assert.equal(sentTo.searchParams.get('nonce'), cookieFrom(response, 'entra_nonce'))
  })

  test('returning_from_entra_should_show_the_users_oid_name_and_email', async () => {
    const response = await signIn(200)

    assert.match(response.text, /11111111-2222-3333-4444-555555555555/)
    assert.match(response.text, /Ada Lovelace/)
    assert.match(response.text, /ada\.lovelace@hmcts\.net/)
  })

  test('a_callback_whose_state_does_not_match_the_cookie_should_be_rejected', async () => {
    const started = await request(app).get('/auth/entra').expect(302)

    const response = await request(app)
      .get('/auth/callback?code=the-auth-code&state=forged-by-someone-else')
      .set('Cookie', `entra_state=${stateCookieFrom(started)}`)
      .expect(400)

    assert.match(response.text, /State mismatch/)
  })

  test('a_callback_with_no_cookie_at_all_should_be_rejected', async () => {
    await request(app).get('/auth/callback?code=the-auth-code&state=anything').expect(400)
  })

  test('a_callback_carrying_an_entra_error_should_say_so', async () => {
    const response = await request(app)
      .get('/auth/callback?error=access_denied&error_description=The+user+cancelled')
      .expect(400)

    assert.match(response.text, /access_denied/)
    assert.match(response.text, /The user cancelled/)
  })
})

describe('id token verification', () => {
  // Each of these is a token the stub Entra hands back over the back channel that must not be
  // believed. The signed-in page must never render, and none of the claims may leak into the page.
  async function assertRejected (issue) {
    issueIdToken = issue
    const response = await signIn(401)

    assert.match(response.text, /ID token rejected/)
    assert.doesNotMatch(response.text, /Ada Lovelace/)
  }

  test('a_token_signed_by_some_other_key_should_be_rejected', async () => {
    const forger = await generateKeyPair('RS256')
    await assertRejected(() => goodIdToken(pendingSignIn, { key: forger.privateKey }))
  })

  test('an_unsigned_token_should_be_rejected', async () => {
    const segment = value => Buffer.from(JSON.stringify(value)).toString('base64url')
    await assertRejected(() =>
      `${segment({ alg: 'none' })}.${segment({ ...CLAIMS, iss: issuer, aud: CLIENT_ID, nonce: pendingSignIn.nonce })}.`)
  })

  test('a_token_signed_with_a_shared_secret_instead_of_the_tenant_key_should_be_rejected', async () => {
    // The HS256 key-confusion attack: sign with a secret and hope the verifier treats it as a key.
    await assertRejected(() => goodIdToken(pendingSignIn, { alg: 'HS256', key: new TextEncoder().encode('x'.repeat(32)) }))
  })

  test('a_token_issued_for_another_app_should_be_rejected', async () => {
    await assertRejected(() => goodIdToken(pendingSignIn, { aud: 'some-other-client' }))
  })

  test('a_token_from_another_tenant_should_be_rejected', async () => {
    await assertRejected(() => goodIdToken(pendingSignIn, { iss: 'https://login.example/other-tenant/v2.0' }))
  })

  test('an_expired_token_should_be_rejected', async () => {
    await assertRejected(() => goodIdToken(pendingSignIn, { exp: Math.floor(Date.now() / 1000) - 3600 }))
  })

  test('a_token_from_a_different_sign_in_should_be_rejected', async () => {
    // A genuine token, but its nonce belongs to some other round trip: a replay.
    await assertRejected(() => goodIdToken({ nonce: 'from-an-earlier-sign-in' }))
  })
})

describe('configuration', () => {
  test('a_missing_setting_should_name_itself_rather_than_fail_later', () => {
    const secret = process.env.ENTRA_CLIENT_SECRET
    delete process.env.ENTRA_CLIENT_SECRET
    try {
      assert.throws(() => assertConfigured(), /ENTRA_CLIENT_SECRET/)
    } finally {
      process.env.ENTRA_CLIENT_SECRET = secret
    }
  })

  test('every_setting_present_should_start_without_complaint', () => {
    assert.doesNotThrow(() => assertConfigured())
  })
})

// The sign-in in progress: what /auth/entra put in the browser's cookies. The stub token endpoint
// reads the nonce from here, as Entra would have read it from the authorize request.
let pendingSignIn

beforeEach(() => {
  pendingSignIn = undefined
  issueIdToken = () => goodIdToken(pendingSignIn)
})

/** The whole round trip: start at /auth/entra, then come back to the callback with its cookies. */
async function signIn (expectedStatus) {
  const started = await request(app).get('/auth/entra').expect(302)
  pendingSignIn = { state: stateCookieFrom(started), nonce: cookieFrom(started, 'entra_nonce') }

  return request(app)
    .get(`/auth/callback?code=the-auth-code&state=${pendingSignIn.state}`)
    .set('Cookie', [`entra_state=${pendingSignIn.state}`, `entra_nonce=${pendingSignIn.nonce}`])
    .expect(expectedStatus)
}

function stateCookieFrom (response) {
  return cookieFrom(response, 'entra_state')
}

function cookieFrom (response, name) {
  const cookie = response.headers['set-cookie'].find(c => c.startsWith(`${name}=`))
  return cookie.split(';')[0].split('=')[1]
}
