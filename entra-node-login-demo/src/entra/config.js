// Where Entra is and what to talk to it with.
//
// Everything here is defaulted to the HMCTS sandbox tenant so the demo runs with one export. None of
// it is a credential: the client ID and the tenant's endpoints are sent to the browser on every
// sign-in, which is why they can sit in a public repo. The secret cannot, and has no default.
const SANDBOX = {
  authorityHost: 'https://hmctsextsbox.ciamlogin.com',
  tenantId: 'd44f885c-4fac-47bf-afde-d7d861ec4d7b',
  clientId: 'aedd508a-d053-4d15-ae9c-6a61a54370f6',
  // Matches the redirect URI registered on that app registration. On another port Entra answers
  // AADSTS50011 (redirect URI mismatch) before the user ever sees a login form.
  port: 3100
}

// Entra publishes both at <authority>/<tenant>/v2.0/.well-known/openid-configuration; they are
// derived rather than configured so a different tenant needs two settings, not four.
const endpoint = name =>
  `${process.env.ENTRA_AUTHORITY_HOST || SANDBOX.authorityHost}/` +
  `${process.env.ENTRA_TENANT_ID || SANDBOX.tenantId}/oauth2/v2.0/${name}`

// Read on each use rather than captured at import, so it cannot matter whether this module was
// loaded before or after the environment was set up.
const config = {
  get clientId () { return process.env.ENTRA_SIGNIN_CLIENT_ID || SANDBOX.clientId },
  get clientSecret () { return process.env.ENTRA_SIGNIN_CLIENT_SECRET },
  get authorizeEndpoint () { return process.env.ENTRA_AUTHORIZE_ENDPOINT || endpoint('authorize') },
  get tokenEndpoint () { return process.env.ENTRA_TOKEN_ENDPOINT || endpoint('token') },
  get port () { return process.env.PORT || SANDBOX.port },
  get redirectUri () { return process.env.ENTRA_REDIRECT_URI || `http://localhost:${config.port}/auth/callback` },
  // Entra only redirects to a URI registered on the app, so an https one means serving https here.
  get usesHttps () { return config.redirectUri.startsWith('https:') },
  scope: 'openid profile email'
}

/**
 * The secret is the only thing with no default, so it is the only thing that can be missing. Checked
 * at startup, so it is a refusal to start that says what to do - rather than a 400 from Entra during
 * the token exchange, after the user has already typed their password.
 */
function assertConfigured () {
  if (config.clientSecret) return

  throw new Error([
    'Cannot start: ENTRA_SIGNIN_CLIENT_SECRET is not set.',
    '',
    '  export ENTRA_SIGNIN_CLIENT_SECRET=\'<paste it - quoted, the value contains ~>\'',
    '  npm start',
    '',
    'Everything else defaults to the HMCTS sandbox tenant. Never put the secret in a file here.',
    '',
    'SIGNIN, not ONBOARDING: this is the application that signs people in. The one that registers',
    'applications through Graph is a different registration - see entra-app-registration-demo.'
  ].join('\n'))
}

module.exports = { config, assertConfigured }
