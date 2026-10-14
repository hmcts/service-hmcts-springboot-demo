// Where the client secret comes from, in order:
//
//   1. ENTRA_SIGNIN_CLIENT_SECRET, if exported - always wins
//   2. Key Vault ENTRA_KEY_VAULT / ENTRA_KEY_VAULT_SECRET, read with DefaultAzureCredential
//
// DefaultAzureCredential is what makes one code path work everywhere: in AKS it is the pod's workload
// identity (a managed identity, so no secret anywhere), on a laptop it is the `az login` session. The
// value goes into this process's environment only - never into a file or a log.
const { DefaultAzureCredential } = require('@azure/identity')
const { SecretClient } = require('@azure/keyvault-secrets')

const vaultUrlFor = vaultName => `https://${vaultName}.vault.azure.net`

function secretClientFor (vaultUrl) {
  return new SecretClient(vaultUrl, new DefaultAzureCredential())
}

/** Returns where the secret came from, for the startup log, or undefined if nowhere was set up. */
async function loadClientSecret ({ env = process.env, secretClientFor: clientFor = secretClientFor } = {}) {
  if (env.ENTRA_SIGNIN_CLIENT_SECRET) return 'environment'

  const vault = env.ENTRA_KEY_VAULT
  const name = env.ENTRA_KEY_VAULT_SECRET
  if (!vault || !name) return undefined

  let secret
  try {
    secret = await clientFor(vaultUrlFor(vault)).getSecret(name)
  } catch (err) {
    throw new Error([
      `Cannot start: could not read the client secret from Key Vault ${vault}/${name}.`,
      '',
      `  ${err.message.split('\n')[0]}`,
      '',
      "Locally, run 'az login' with an account that can read that secret. In Azure, check the",
      "workload identity is one of the secret's readers."
    ].join('\n'))
  }
  if (!secret.value) throw new Error(`Cannot start: Key Vault ${vault}/${name} is empty.`)

  env.ENTRA_SIGNIN_CLIENT_SECRET = secret.value
  return `Key Vault ${vault}/${name}`
}

module.exports = { loadClientSecret, secretClientFor, vaultUrlFor }
