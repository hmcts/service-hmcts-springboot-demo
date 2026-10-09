const { buildApp } = require('./src/app')
const { config, assertConfigured } = require('./src/entra/config')

try {
  assertConfigured()
} catch (err) {
  console.error(err.message)
  process.exit(1)
}

buildApp().listen(config.port, () =>
  console.log(`entra-node-login-demo listening on http://localhost:${config.port}`))
