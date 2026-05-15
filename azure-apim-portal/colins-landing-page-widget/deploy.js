const {deployNodeJS} = require("@azure/api-management-custom-widgets-tools")

const serviceInformation = {
  "resourceId": "subscriptions/10c625e6-832a-4713-82e6-07a770299631/resourceGroups/colin-rg/providers/Microsoft.ApiManagement/service/colin-apim",
  "managementApiEndpoint": "https://management.azure.com"
}
const name = "cw-colins-landing-page-widget"
const fallbackConfigPath = "./static/config.msapim.json"
const config = {
  "interactiveBrowserCredentialOptions": {
    "redirectUri": "http://localhost:1337"
  }
}

deployNodeJS(serviceInformation, name, fallbackConfigPath, config)
