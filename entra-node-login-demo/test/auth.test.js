// Drives the app the way a browser does: start the round trip, carry the cookie, come back with the
// code. Entra is a stub HTTP server, so the real fetch in entra/client.js is exercised rather than
// mocked away.
const { before, after, describe, test } = require('node:test')
const assert = require('node:assert/strict')
const http = require('node:http')
const { assertConfigured } = require('../src/entra/config')

const CLAIMS = {
  oid: '11111111-2222-3333-4444-555555555555',
  name: 'Ada Lovelace',
  // An array, not a string: External ID returns emails this way in some configurations.
  emails: ['ada.lovelace@hmcts.net']
}

let entra
let request
let app

function idTokenFor (claims) {
  const segment = value => Buffer.from(JSON.stringify(value)).toString('base64url')
  return `${segment({ alg: 'RS256' })}.${segment(claims)}.not-a-real-signature`
}

/** The one Entra endpoint this app calls server to server. */
function startStubEntra () {
  return new Promise(resolve => {
    const server = http.createServer((req, res) => {
      if (req.url.startsWith('/token')) {
        res.writeHead(200, { 'Content-Type': 'application/json' })
        return res.end(JSON.stringify({ id_token: idTokenFor(CLAIMS), expires_in: 3600 }))
      }
      res.writeHead(404).end()
    })
    server.listen(0, () => resolve(server))
  })
}

before(async () => {
  entra = await startStubEntra()
  const tokenEndpoint = `http://localhost:${entra.address().port}/token`

  // Set before the app is required: src/entra/config.js reads the environment once, on load.
  process.env.ENTRA_CLIENT_ID = 'demo-client'
  process.env.ENTRA_CLIENT_SECRET = 'demo-secret'
  process.env.ENTRA_REDIRECT_URI = 'http://localhost:3000/auth/callback'
  process.env.ENTRA_AUTHORIZE_ENDPOINT = 'https://login.example/authorize'
  process.env.ENTRA_TOKEN_ENDPOINT = tokenEndpoint

  request = require('supertest')
  app = require('../src/app').buildApp()
})

after(() => entra.close())

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
  })

  test('returning_from_entra_should_show_the_users_oid_name_and_email', async () => {
    const started = await request(app).get('/auth/entra').expect(302)
    const state = stateCookieFrom(started)

    const response = await request(app)
      .get(`/auth/callback?code=the-auth-code&state=${state}`)
      .set('Cookie', `entra_state=${state}`)
      .expect(200)

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

function stateCookieFrom (response) {
  const cookie = response.headers['set-cookie'].find(c => c.startsWith('entra_state='))
  return cookie.split(';')[0].split('=')[1]
}
