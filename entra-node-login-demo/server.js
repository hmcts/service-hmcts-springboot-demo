// Sign in with Microsoft Entra, then show who signed in.
//
// Nothing is stored and there is no session: the claims are rendered once and forgotten.
const fs = require('node:fs')
const http = require('node:http')
const https = require('node:https')
const { buildApp } = require('./src/app')
const { config, assertConfigured } = require('./src/entra/config')
const { loadClientSecret } = require('./src/entra/secret')

const CERT_DIR = 'certs'

// Settings from a gitignored .env, if there is one. Anything already exported wins over it.
try {
  process.loadEnvFile('.env')
} catch (err) {
  if (err.code !== 'ENOENT') throw err
}

async function start () {
  let secretSource
  try {
    secretSource = await loadClientSecret()
    assertConfigured()
  } catch (err) {
    console.error(err.message)
    process.exit(1)
  }

  // Entra redirects only to a URI registered on the app registration. Some are registered as https -
  // web-api-marketplace's local one is - and a plain-http server would never receive the callback.
  // Length only, never the value: a secret that arrives truncated or quoted is indistinguishable from
  // a wrong one in Entra's reply, which says "Invalid client secret provided" either way.
  console.log('entra-node-login-demo  client %s  secret %d chars from %s  redirect %s',
    config.clientId, config.clientSecret.length, secretSource, config.redirectUri)

  const app = buildApp()
  if (config.usesHttps) {
    let credentials
    try {
      credentials = { key: fs.readFileSync(`${CERT_DIR}/localhost-key.pem`),
        cert: fs.readFileSync(`${CERT_DIR}/localhost.pem`) }
    } catch {
      console.error([
        `Cannot start: ENTRA_REDIRECT_URI is https, so this needs a certificate in ${CERT_DIR}/.`,
        '',
        '  npm run cert',
        '',
        'It is self-signed, so the browser will warn once and let you continue.'
      ].join('\n'))
      process.exit(1)
    }
    https.createServer(credentials, app).listen(config.port, () =>
      console.log(`entra-node-login-demo listening on https://localhost:${config.port}`))
  } else {
    http.createServer(app).listen(config.port, () =>
      console.log(`entra-node-login-demo listening on http://localhost:${config.port}`))
  }
}

start()
