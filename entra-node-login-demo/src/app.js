// The Express app, with no listen() of its own, so a test can drive it without opening a port.
const express = require('express')
const cookieParser = require('cookie-parser')
const home = require('./routes/home')
const auth = require('./routes/auth')

function buildApp () {
  const app = express()
  app.use(cookieParser())
  // req.path, never req.originalUrl: the callback's query string carries the authorization code.
  app.use((req, res, next) => {
    console.log('-> %s %s', req.method, req.path)
    next()
  })
  app.use(home)
  app.use(auth)
  return app
}

module.exports = { buildApp }
