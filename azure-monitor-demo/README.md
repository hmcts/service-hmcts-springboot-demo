# Azure Monitor Demo

Guides for monitoring `hearing-results-document-subscription` using Azure Log Analytics.

> ⚠️ **Prototyping only** — the portal (clickops) guides here are for learning and prototyping. Production alerts and monitoring infrastructure are deployed via Terraform in [cp-amp-terraform-alerts](https://github.com/hmcts/cp-amp-terraform-alerts).

---

## Architecture overview

```mermaid
flowchart TD

    subgraph K8S["☸️ Kubernetes"]
        POD["Pod\nhearing-results-document-subscription\nns-dev-amp-01"]
        STDOUT["stdout / stderr\nJSON structured logs"]
        POD --> STDOUT
    end

    subgraph INGEST["Azure Monitor — Ingestion"]
        AGENT["Azure Monitor Agent\nDaemonSet on each node"]
        STDOUT --> AGENT
    end

    subgraph LA["Log Analytics Workspace  —  la-mdv-dev-int-ws"]
        TABLE["ContainerLogV2\nPodName · PodNamespace · LogLevel · LogMessage"]
        AGENT --> TABLE
        TABLE --> KQL["KQL queries\nfilter · summarize · parse_json · render"]
    end

    KQL --> ADHOC["🔍 Ad-hoc investigation\nLog Analytics portal"]

    KQL --> DASH["📊 Azure Dashboard\npinned chart & table tiles"]

    KQL --> RULE["⚡ Alert Rule\nKQL condition + threshold\ne.g. ErrorCount ≥ 1"]

    RULE --> AG["Action Group"]

    AG --> EMAIL["📧 Email"]
    AG --> SMS["📱 SMS"]
    AG --> CHANEMAIL["📧 Teams channel\nvia channel email address"]
    AG --> WEBHOOK["🔗 Webhook"]

    WEBHOOK --> LA2["Azure Logic App\nHTTP trigger"]
    LA2 --> CHAT["💬 Teams group chat\ne.g. AMP Tech"]
```

---

## Contents

| Guide | Description |
|---|---|
| [queries.md](./queries.md) | KQL queries for the dashboard, ad-hoc investigation, and KQL tips (local time, JSON parsing, chart types, zero-fill) |
| [dashboard.md](./dashboard.md) | How to create dashboard tiles from KQL queries, import/export the dashboard JSON |
| [alerts.md](./alerts.md) | How to set up an Action Group (email + SMS) and an Alert Rule, with rich notification options via Logic App |
| [teams-webhook.md](./teams-webhook.md) | Sending alerts to MS Teams via Workflows webhook — including known Save greyed out issue on HMCTS accounts |
| [logic-app.md](./logic-app.md) | Creating an Azure Logic App to post Teams messages on alert — recommended alternative when Power Automate licences are unavailable |
