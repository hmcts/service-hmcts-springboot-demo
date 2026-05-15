export type Values = {
  heading: string
  subtitle: string
  githubLink: string
}

export const valuesDefault: Readonly<Values> = Object.freeze({
  heading: "Welcome to the API Portal",
  subtitle: "Explore, test, and integrate with our APIs.",
  githubLink: "https://github.com/hmcts",
})
