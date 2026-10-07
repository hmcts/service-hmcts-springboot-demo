(function () {
  'use strict'

  var $ = function (id) { return document.getElementById(id) }
  var secretBox = $('secret-box')

  function csrf () {
    var match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/)
    return match ? decodeURIComponent(match[1]) : ''
  }

  function api (method, path, body) {
    var headers = { 'Content-Type': 'application/json' }
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = csrf()
    return fetch(path, {
      method: method,
      headers: headers,
      credentials: 'same-origin',
      body: body ? JSON.stringify(body) : undefined
    }).then(function (res) {
      if (res.status === 401) { show(false); throw new Error('Not signed in.') }
      if (!res.ok) {
        return res.json().catch(function () { return {} }).then(function (b) {
          throw new Error(b.message || b.error || ('Something went wrong (' + res.status + ').'))
        })
      }
      return res.status === 204 ? null : res.json()
    })
  }

  function showError (message) {
    var el = $('error')
    el.textContent = message || ''
    el.hidden = !message
  }

  function show (signedIn) {
    $('signed-in').hidden = !signedIn
    $('signed-out').hidden = signedIn
  }

  function text (tag, value, className) {
    var el = document.createElement(tag)
    el.textContent = value
    if (className) el.className = className
    return el
  }

  function button (label, className, onClick) {
    var el = text('button', label, className)
    el.type = 'button'
    el.addEventListener('click', onClick)
    return el
  }

  function renderApp (app, apis) {
    var box = document.createElement('div')
    box.className = 'app'
    box.appendChild(text('h3', app.name))
    var ids = document.createElement('p')
    ids.appendChild(document.createTextNode('Client ID '))
    ids.appendChild(text('code', app.clientId))
    box.appendChild(ids)

    var table = document.createElement('table')
    var head = table.createTHead().insertRow()
    ;['API', 'Subscription key', ''].forEach(function (h) { head.appendChild(text('th', h)) })
    var body = table.createTBody()
    app.subscriptions.forEach(function (s) {
      var row = body.insertRow()
      row.insertCell().textContent = s.apiId
      row.insertCell().appendChild(text('code', s.subscriptionKey))
      row.insertCell().appendChild(button('Remove', 'link', function () {
        api('DELETE', '/api/applications/' + app.id + '/apis/' + encodeURIComponent(s.apiId)).then(load).catch(fail)
      }))
    })
    if (!app.subscriptions.length) {
      var empty = body.insertRow().insertCell()
      empty.colSpan = 3
      empty.className = 'muted'
      empty.textContent = 'No APIs connected yet.'
    }
    box.appendChild(table)

    var actions = document.createElement('div')
    actions.className = 'actions'
    var select = document.createElement('select')
    select.setAttribute('aria-label', 'API to connect to ' + app.name)
    apis.forEach(function (a) {
      var option = document.createElement('option')
      option.value = a
      option.textContent = a
      select.appendChild(option)
    })
    actions.appendChild(select)
    actions.appendChild(button('Connect API', '', function () {
      api('POST', '/api/applications/' + app.id + '/apis', { apiId: select.value }).then(load).catch(fail)
    }))
    actions.appendChild(button('Delete application', 'danger', function () {
      api('DELETE', '/api/applications/' + app.id).then(load).catch(fail)
    }))
    box.appendChild(actions)
    return box
  }

  function load () {
    showError('')
    return Promise.all([api('GET', '/api/applications'), api('GET', '/api/apis')]).then(function (results) {
      var apps = results[0]
      var list = $('apps')
      list.textContent = ''
      apps.forEach(function (app) { list.appendChild(renderApp(app, results[1])) })
      $('none').hidden = apps.length > 0
    })
  }

  function fail (error) { showError(error.message) }

  $('create-form').addEventListener('submit', function (event) {
    event.preventDefault()
    api('POST', '/api/applications', { name: $('app-name').value }).then(function (created) {
      $('new-client-id').textContent = created.application.clientId
      $('new-client-secret').textContent = created.clientSecret
      secretBox.hidden = false
      $('app-name').value = ''
      return load()
    }).catch(fail)
  })

  $('sign-out').addEventListener('click', function () {
    var form = document.createElement('form')
    form.method = 'post'
    form.action = '/logout'
    var input = document.createElement('input')
    input.type = 'hidden'
    input.name = '_csrf'
    input.value = csrf()
    form.appendChild(input)
    document.body.appendChild(form)
    form.submit()
  })

  api('GET', '/api/me').then(function (me) {
    $('who-name').textContent = me.name
    $('who-via').textContent = '(' + me.via + ')'
    show(true)
    return load()
  }).catch(function () { show(false) })
})()
