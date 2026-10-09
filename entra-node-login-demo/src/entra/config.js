// Where Entra is and what to talk to it with.
//
// Everything here is defaulted to the HMCTS sandbox tenant so the demo runs with one export. None of
// it is a credential: the client ID and the tenant's endpoints are sent to the browser on every
// sign-in, which is why they can sit in a public repo. The secret cannot, and has no default.
const SANDBOX = {
  clientId: 'aedd508a-d053-4d15-ae9c-6a61a54370f6',
  authorizeEndpoint: 'https://hmctsextsbox.ciamlogin.com/d44f885c-4fac-47bf-afde-d7d861ec4d7b/oauth2/v2.0/authorize',
  tokenEndpoint: 'https://hmctsextsbox.ciamlogin.com/d44f885c-4fac-47bf-afde-d7d861ec4d7b/oauth2/v2.0/token',
  // Matches the redirect URI registered on that app registration. On another port Entra answers
  // AADSTS50011 (redirect URI mismatch) before the user ever sees a login form.
  port: 3100
}

// Read on each use rather than captured at import, so it cannot matter whether this module was
// loaded before or after the environment was set up.
const config = {
  get clientId () { return process.env.ENTRA_CLIENT_ID || SANDBOX.clientId },
  get clientSecret () { return process.env.ENTRA_CLIENT_SECRET },
  get authorizeEndpoint () { return process.env.ENTRA_AUTHORIZE_ENDPOINT || SANDBOX.authorizeEndpoint },
  get tokenEndpoint () { return process.env.ENTRA_TOKEN_ENDPOINT || SANDBOX.tokenEndpoint },
  get port () { return process.env.PORT || SANDBOX.port },
  get redirectUri () { return process.env.ENTRA_REDIRECT_URI || `http://localhost:${config.port}/auth/callback` },
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
    'Cannot start: ENTRA_CLIENT_SECRET is not set.',
    '',
    '  export ENTRA_CLIENT_SECRET=...   # from the Entra app registration',
    '  npm start',
    '',
    'Everything else defaults to the HMCTS sandbox tenant. Never put the secret in a file here.'
  ].join('\n'))
}

module.exports = { config, assertConfigured }
