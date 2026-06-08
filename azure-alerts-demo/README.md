# Azure Alerts Demo

This is a signpost — there is no application code here. Azure alerts are
configured in Azure Monitor (portal or Terraform) and sit outside the
application codebase.

---

## What this demonstrates

How to set up Azure Monitor alerts that watch a **Log Analytics Workspace** for
specific log patterns emitted by a running service, and notify the team via
email, SMS, or Microsoft Teams when a condition is met.

---

## The monitored service

Alerts in this demo are scoped to
**[service-cp-crime-hearing-results-document-subscription](https://github.com/hmcts/service-cp-crime-hearing-results-document-subscription)**
(`hearing-results-document-subscription`)

That service writes structured JSON logs to stdout which are ingested into Log
Analytics via the Azure Monitor Agent. The alert rules query the
`ContainerLogV2` table for specific log patterns (e.g. `ResponseStatusException`
at `error` level).

---

## Terraform

Alert rules and Action Groups are managed via Terraform in
[cp-amp-terraform-alerts](https://github.com/hmcts/cp-amp-terraform-alerts).
