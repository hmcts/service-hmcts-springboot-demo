import {askForSecrets} from "@azure/api-management-custom-widgets-tools"

askForSecrets("app").catch(e => console.error("Failed to connect to Developer Portal.", e))
