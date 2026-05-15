# Colins Landing Page Widget

A custom widget for the `colin-apim` Developer Portal, providing a styled landing page with quick links to the API catalogue, subscription management, and GitHub.

## Local development

```bash
npm install
npm start
```

This starts a local dev server. Open the URL it prints — the widget renders in a sandboxed iframe that mimics the portal environment.

## Edit the widget

The main file to edit is [`public/index.html`](public/index.html) — this is the widget's HTML, CSS, and content. The TypeScript entry point in [`src/index.ts`](src/index.ts) handles communication with the portal (user context, environment values).

## Deploy to the Developer Portal

```bash
npm run deploy
```

This pushes the widget directly to `colin-apim`. Once deployed, go to the portal editor in the Azure Portal and add the widget to a page, then publish.

## Configuration

The APIM connection details are in `package.json` under `"apimWidgets"`:

```json
{
  "subscriptionId": "10c625e6-832a-4713-82e6-07a770299631",
  "resourceGroupName": "colin-rg",
  "serviceName": "colin-apim"
}
```

Update these if deploying to a different APIM instance.
