// Where the client secret comes from: an exported value, else Key Vault through DefaultAzureCredential.
// Key Vault is a fake SecretClient handed in, so nothing here needs Azure or a login.
const { describe, test } = require('node:test')
const assert = require('node:assert/strict')
const { SecretClient } = require('@azure/keyvault-secrets')
const { loadClientSecret, secretClientFor, vaultUrlFor } = require('../src/entra/secret')
const { assertConfigured } = require('../src/entra/config')

/** A Key Vault holding the given secrets, recording which vault and secret were asked for. */
function fakeVault (secrets) {
  const asked = []
  const factory = vaultUrl => ({
    getSecret: async name => {
      asked.push({ vaultUrl, name })
      if (!(name in secrets)) throw new Error(`SecretNotFound: ${name}`)
      return { name, value: secrets[name] }
    }
  })
  return { factory, asked }
}

const VAULT_SETTINGS = { ENTRA_KEY_VAULT: 'kvdemo', ENTRA_KEY_VAULT_SECRET: 'entra-app-demo-secret' }

describe('client secret', () => {
  test('an_exported_secret_should_win_without_calling_key_vault', async () => {
    const env = { ...VAULT_SETTINGS, ENTRA_SIGNIN_CLIENT_SECRET: 'exported~value' }
    const vault = fakeVault({ 'entra-app-demo-secret': 'vault~value' })

    const source = await loadClientSecret({ env, secretClientFor: vault.factory })

    assert.equal(env.ENTRA_SIGNIN_CLIENT_SECRET, 'exported~value')
    assert.equal(source, 'environment')
    assert.deepEqual(vault.asked, [])
  })

  test('with_no_exported_secret_it_should_be_read_from_the_key_vault_named_in_the_settings', async () => {
    const env = { ...VAULT_SETTINGS }
    const vault = fakeVault({ 'entra-app-demo-secret': 'vault~value' })

    const source = await loadClientSecret({ env, secretClientFor: vault.factory })

    assert.equal(env.ENTRA_SIGNIN_CLIENT_SECRET, 'vault~value')
    assert.deepEqual(vault.asked, [{ vaultUrl: vaultUrlFor('kvdemo'), name: 'entra-app-demo-secret' }])
    assert.equal(source, 'Key Vault kvdemo/entra-app-demo-secret')
  })

  test('the_vault_address_should_be_the_named_vault_over_https', () => {
    const url = new URL(vaultUrlFor('kvdemo'))

    assert.equal(url.protocol, 'https:')
    assert.equal(url.hostname.split('.')[0], 'kvdemo')
    assert.ok(url.hostname.endsWith('.azure.net'))
  })

  test('with_no_secret_and_no_vault_it_should_be_left_unset_for_assertConfigured_to_report', async () => {
    const env = {}
    const vault = fakeVault({})

    const source = await loadClientSecret({ env, secretClientFor: vault.factory })

    assert.equal(env.ENTRA_SIGNIN_CLIENT_SECRET, undefined)
    assert.equal(source, undefined)
    assert.deepEqual(vault.asked, [])
  })

  test('a_key_vault_failure_should_name_the_vault_and_secret_and_say_how_to_fix_it', async () => {
    const env = { ...VAULT_SETTINGS }
    const vault = fakeVault({})

    await assert.rejects(loadClientSecret({ env, secretClientFor: vault.factory }), err => {
      assert.match(err.message, /kvdemo\/entra-app-demo-secret/)
      assert.match(err.message, /az login/)
      assert.match(err.message, /SecretNotFound/)
      return true
    })
    assert.equal(env.ENTRA_SIGNIN_CLIENT_SECRET, undefined)
  })

  test('an_empty_secret_in_key_vault_should_be_refused', async () => {
    const env = { ...VAULT_SETTINGS }
    const vault = fakeVault({ 'entra-app-demo-secret': '' })

    await assert.rejects(loadClientSecret({ env, secretClientFor: vault.factory }), /empty/)
    assert.equal(env.ENTRA_SIGNIN_CLIENT_SECRET, undefined)
  })

  test('the_real_client_should_be_a_key_vault_secret_client_for_that_vault', () => {
    const client = secretClientFor(vaultUrlFor('kvdemo'))

    assert.ok(client instanceof SecretClient)
    assert.equal(client.vaultUrl, vaultUrlFor('kvdemo'))
  })

  test('the_missing_secret_message_should_offer_key_vault_as_well_as_an_export', () => {
    const secret = process.env.ENTRA_SIGNIN_CLIENT_SECRET
    delete process.env.ENTRA_SIGNIN_CLIENT_SECRET
    try {
      assert.throws(() => assertConfigured(), /ENTRA_KEY_VAULT_SECRET/)
    } finally {
      if (secret !== undefined) process.env.ENTRA_SIGNIN_CLIENT_SECRET = secret
    }
  })
})
