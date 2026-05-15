import {getValues, Secrets} from "@azure/api-management-custom-widgets-tools"
import {valuesDefault} from "./values"

class App {
  constructor(public readonly secrets: Secrets) {
    const values = getValues(valuesDefault)

    Object.entries(values).forEach(([key, value]) => {
      const element = document.getElementById(`values.${key}`)
      if (element) {
        if (key === "githubLink" && element instanceof HTMLAnchorElement) {
          element.href = value
        } else {
          element.innerText = value
        }
      }
    })
  }
}

export default App
