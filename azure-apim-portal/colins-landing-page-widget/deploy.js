const {deployNodeJS} = require("@azure/api-management-custom-widgets-tools")
const {execSync} = require("child_process")

// Use existing Azure CLI login — no browser prompt needed
let tokenOverride
try {
  const tokenJson = JSON.parse(
    execSync("az account get-access-token --resource https://management.azure.com", {stdio: ["pipe", "pipe", "pipe"]}).toString()
  )
  tokenOverride = `Bearer ${tokenJson.accessToken}`
  console.log("Using Azure CLI credentials")
} catch (e) {
  console.warn("Could not get Azure CLI token — will fall back to interactive browser login")
  console.warn("Run 'az login' if the deploy fails")
}

const serviceInformation = {
  resourceId: "subscriptions/10c625e6-832a-4713-82e6-07a770299631/resourceGroups/colin-rg/providers/Microsoft.ApiManagement/service/colin-apim",
  managementApiEndpoint: "https://management.azure.com",
  ...(tokenOverride && {tokenOverride}),
}

const name = "cw-colins-landing-page-widget"
const fallbackConfigPath = "./static/config.msapim.json"

deployNodeJS(serviceInformation, name, fallbackConfigPath)
