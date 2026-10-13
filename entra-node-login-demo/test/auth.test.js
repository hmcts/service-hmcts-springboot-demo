// Drives the app the way a browser does: start the round trip, carry the cookies, come back with the
// code. Entra is a stub HTTP server that signs real RS256 tokens and publishes its keys, so the real
// fetches and the real signature check in entra/ are exercised rather than mocked away.
const { before, after, beforeEach, describe, test } = require('node:test')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const http = require('node:http')
const { assertConfigured } = require('../src/entra/config')

const ISSUER = 'https://issuer.example/demo-tenant/v2.0'
const CLAIMS = {
  oid: '11111111-2222-3333-4444-555555555555',
  name: 'Ada Lovelace',
  // An array, not a string: External ID returns emails this way in some configurations.
  emails: ['ada.lovelace@hmcts.net']
}

const entraKey = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 })
const rogueKey = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 })

let entra
let request
let app

/** What the stub token endpoint answers with next. */
let tokenFor
/** The last body POSTed to the stub token endpoint. */
let lastTokenRequest
let keyFetches

function sign (header, claims, privateKey) {
  const segment = value => Buffer.from(JSON.stringify(value)).toString('base64url')
  const signingInput = `${segment(header)}.${segment(claims)}`
  const signature = header.alg === 'none'
    ? ''
    : crypto.sign('RSA-SHA256', Buffer.from(signingInput), privateKey).toString('base64url')
  return `${signingInput}.${signature}`
}

/** A token Entra would really issue for this sign-in. Each rejection test spoils one thing about it. */
function validToken (nonce, { claims = {}, header = {}, key = entraKey.privateKey } = {}) {
  const now = Math.floor(Date.now() / 1000)
  return sign(
    { alg: 'RS256', typ: 'JWT', kid: 'entra-key-1', ...header },
    { iss: ISSUER, aud: 'demo-client', tid: 'demo-tenant', iat: now, nbf: now, exp: now + 3600, nonce, ...CLAIMS, ...claims },
    key)
}

/** The two Entra endpoints this app calls server to server. */
function startStubEntra () {
  return new Promise(resolve => {
    const server = http.createServer((req, res) => {
      if (req.url.startsWith('/token')) {
        let body = ''
        req.on('data', chunk => { body += chunk })
        req.on('end', () => {
          lastTokenRequest = Object.fromEntries(new URLSearchParams(body))
          res.writeHead(200, { 'Content-Type': 'application/json' })
          res.end(JSON.stringify({ id_token: tokenFor(), expires_in: 3600 }))
        })
        return
      }
      if (req.url.startsWith('/keys')) {
        keyFetches++
        const jwk = { ...entraKey.publicKey.export({ format: 'jwk' }), kid: 'entra-key-1', use: 'sig', alg: 'RS256' }
        res.writeHead(200, { 'Content-Type': 'application/json' })
        return res.end(JSON.stringify({ keys: [jwk] }))
      }
      res.writeHead(404).end()
    })
    server.listen(0, () => resolve(server))
  })
}

before(async () => {
  entra = await startStubEntra()
  const stub = `http://localhost:${entra.address().port}`

  process.env.ENTRA_SIGNIN_CLIENT_ID = 'demo-client'
  process.env.ENTRA_SIGNIN_CLIENT_SECRET = 'demo-secret'
  process.env.ENTRA_TENANT_ID = 'demo-tenant'
  process.env.ENTRA_REDIRECT_URI = 'http://localhost:3000/auth/callback'
  process.env.ENTRA_AUTHORIZE_ENDPOINT = 'https://login.example/authorize'
  process.env.ENTRA_TOKEN_ENDPOINT = `${stub}/token`
  process.env.ENTRA_JWKS_URI = `${stub}/keys`
  process.env.ENTRA_ISSUER = ISSUER

  request = require('supertest')
  app = require('../src/app').buildApp()
})

after(() => entra.close())

beforeEach(() => {
  lastTokenRequest = undefined
  keyFetches = 0
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
    assert.equal(sentTo.searchParams.get('state'), cookieFrom(response, 'entra_state'))
  })

  test('starting_sign_in_should_send_an_s256_pkce_challenge_for_the_verifier_kept_in_a_cookie', async () => {
    const response = await request(app).get('/auth/entra').expect(302)

    const sentTo = new URL(response.headers.location)
    const verifier = cookieFrom(response, 'entra_pkce')
    assert.equal(sentTo.searchParams.get('code_challenge_method'), 'S256')
    assert.equal(sentTo.searchParams.get('code_challenge'), s256(verifier))
    // RFC 7636: 43 to 128 characters.
    assert.ok(verifier.length >= 43 && verifier.length <= 128)
  })

  test('starting_sign_in_should_send_the_nonce_kept_in_a_cookie', async () => {
    const response = await request(app).get('/auth/entra').expect(302)

    const nonce = new URL(response.headers.location).searchParams.get('nonce')
    assert.ok(nonce)
    assert.equal(nonce, cookieFrom(response, 'entra_nonce'))
  })

  test('the_flow_cookies_should_be_http_only_and_short_lived', async () => {
    const response = await request(app).get('/auth/entra').expect(302)

    for (const name of ['entra_state', 'entra_nonce', 'entra_pkce']) {
      const cookie = response.headers['set-cookie'].find(c => c.startsWith(`${name}=`))
      assert.match(cookie, /HttpOnly/, name)
      assert.match(cookie, /Max-Age=600/, name)
    }
  })

  test('redeeming_the_code_should_send_the_pkce_verifier_that_matches_the_challenge', async () => {
    const flow = await startSignIn()
    tokenFor = () => validToken(flow.nonce)

    await finishSignIn(flow).expect(200)

    assert.equal(lastTokenRequest.code, 'the-auth-code')
    assert.equal(s256(lastTokenRequest.code_verifier), flow.challenge)
  })

  test('returning_from_entra_should_show_the_users_oid_name_and_email', async () => {
    const flow = await startSignIn()
    tokenFor = () => validToken(flow.nonce)

    const response = await finishSignIn(flow).expect(200)

    assert.match(response.text, /11111111-2222-3333-4444-555555555555/)
    assert.match(response.text, /Ada Lovelace/)
    assert.match(response.text, /ada\.lovelace@hmcts\.net/)
  })

  test('the_signing_keys_should_be_fetched_once_and_reused', async () => {
    for (let i = 0; i < 2; i++) {
      const flow = await startSignIn()
      tokenFor = () => validToken(flow.nonce)
      await finishSignIn(flow).expect(200)
    }

    assert.ok(keyFetches <= 1, `fetched the keys ${keyFetches} times`)
  })

  test('a_callback_whose_state_does_not_match_the_cookie_should_be_rejected', async () => {
    const flow = await startSignIn()

    const response = await finishSignIn({ ...flow, state: 'forged-by-someone-else' }).expect(400)

    assert.match(response.text, /State mismatch/)
  })

  test('a_callback_with_no_cookie_at_all_should_be_rejected', async () => {
    await request(app).get('/auth/callback?code=the-auth-code&state=anything').expect(400)
  })

  test('a_callback_with_no_code_should_be_rejected_before_calling_entra', async () => {
    const flow = await startSignIn()

    const response = await request(app)
      .get(`/auth/callback?state=${flow.state}`)
      .set('Cookie', flow.cookies)
      .expect(400)

    assert.match(response.text, /No authorization code/)
    assert.equal(lastTokenRequest, undefined)
  })

  test('a_callback_carrying_an_entra_error_should_say_so', async () => {
    const response = await request(app)
      .get('/auth/callback?error=access_denied&error_description=The+user+cancelled')
      .expect(400)

    assert.match(response.text, /access_denied/)
    assert.match(response.text, /The user cancelled/)
  })
})

describe('id token validation', () => {
  const now = () => Math.floor(Date.now() / 1000)

  // Each one spoils exactly one thing about an otherwise valid token.
  const spoiled = {
    a_nonce_from_another_sign_in: [() => validToken('someone-elses-nonce'), /nonce/],
    no_nonce_at_all: [() => validToken(undefined), /nonce/],
    another_applications_audience: [nonce => validToken(nonce, { claims: { aud: 'other-client' } }), /aud/],
    another_issuer: [nonce => validToken(nonce, { claims: { iss: 'https://evil.example/v2.0' } }), /iss/],
    another_tenant: [nonce => validToken(nonce, { claims: { tid: 'other-tenant' } }), /tid/],
    an_expired_token: [nonce => validToken(nonce, { claims: { exp: now() - 600 } }), /expired/],
    a_token_not_yet_valid: [nonce => validToken(nonce, { claims: { nbf: now() + 600 } }), /not yet valid/],
    a_signature_from_another_key: [nonce => validToken(nonce, { key: rogueKey.privateKey }), /signature/],
    an_unknown_key_id: [nonce => validToken(nonce, { header: { kid: 'not-a-published-key' } }), /kid/],
    an_unsigned_alg_none_token: [nonce => validToken(nonce, { header: { alg: 'none' } }), /alg/],
    a_tampered_payload: [nonce => tamper(validToken(nonce), { oid: 'attacker' }), /signature/]
  }

  for (const [name, [token, reason]] of Object.entries(spoiled)) {
    test(`${name}_should_be_rejected`, async () => {
      const flow = await startSignIn()
      tokenFor = () => token(flow.nonce)

      const response = await finishSignIn(flow).expect(401)

      assert.match(response.text, /ID token rejected/)
      assert.match(response.text, reason)
      assert.doesNotMatch(response.text, /Ada Lovelace/)
    })
  }
})

describe('configuration', () => {
  test('a_missing_setting_should_name_itself_rather_than_fail_later', () => {
    const secret = process.env.ENTRA_SIGNIN_CLIENT_SECRET
    delete process.env.ENTRA_SIGNIN_CLIENT_SECRET
    try {
      assert.throws(() => assertConfigured(), /ENTRA_SIGNIN_CLIENT_SECRET/)
    } finally {
      process.env.ENTRA_SIGNIN_CLIENT_SECRET = secret
    }
  })

  test('every_setting_present_should_start_without_complaint', () => {
    assert.doesNotThrow(() => assertConfigured())
  })
})

/** Step 1 as a browser does it: everything the callback will need, read off the redirect. */
async function startSignIn () {
  const response = await request(app).get('/auth/entra').expect(302)
  const sentTo = new URL(response.headers.location).searchParams
  return {
    state: sentTo.get('state'),
    nonce: sentTo.get('nonce'),
    challenge: sentTo.get('code_challenge'),
    cookies: response.headers['set-cookie'].map(c => c.split(';')[0])
  }
}

/** Step 2 as a browser does it: back from Entra with a code, carrying the cookies from step 1. */
function finishSignIn (flow) {
  return request(app)
    .get(`/auth/callback?code=the-auth-code&state=${flow.state}`)
    .set('Cookie', flow.cookies)
}

function cookieFrom (response, name) {
  const cookie = response.headers['set-cookie'].find(c => c.startsWith(`${name}=`))
  return cookie.split(';')[0].split('=')[1]
}

function s256 (value) {
  return crypto.createHash('sha256').update(value).digest('base64url')
}

/** Swaps claims in the payload but keeps the original signature. */
function tamper (token, changes) {
  const [header, payload, signature] = token.split('.')
  const claims = { ...JSON.parse(Buffer.from(payload, 'base64url')), ...changes }
  return `${header}.${Buffer.from(JSON.stringify(claims)).toString('base64url')}.${signature}`
}
