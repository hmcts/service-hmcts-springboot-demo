const express = require('express')
const { page } = require('../views/page')

const router = express.Router()

router.get('/', (req, res) => {
  res.send(page('Sign in', `
    <p class="lead">This demo signs you in with Microsoft Entra and shows what Entra says about you.</p>
    <a class="button" href="/auth/entra?prompt=login">Sign in with Microsoft Entra</a>`))
})

module.exports = router
