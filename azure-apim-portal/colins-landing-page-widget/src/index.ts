import { buildOnMessageHandlers, sendMessageToParent } from "@azure/api-management-custom-widgets-tools"

const widgetName = "colins-landing-page-widget"

// Notify the portal that the widget has loaded
sendMessageToParent("loaded", { widgetName })

// Handle messages from the portal (e.g. environment info, user context)
buildOnMessageHandlers({
  onValues: (values) => {
    console.log("Widget values received:", values)
  },
})
