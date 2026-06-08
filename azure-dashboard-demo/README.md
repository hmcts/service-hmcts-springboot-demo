# Azure Dashboard Demo

This is a signpost — there is no application code here. Azure dashboards are
configured in the Azure Portal (or via imported JSON) and sit outside the
application codebase.

---

## What this demonstrates

How to build an Azure Portal dashboard of KQL-driven chart and table tiles that
give a live view of a running service — error trends, inbound notification
volumes, and recent log entries — all sourced from **Log Analytics**.

---

## The monitored service

Dashboard tiles in this demo are scoped to
**[service-cp-crime-hearing-results-document-subscription](https://github.com/hmcts/service-cp-crime-hearing-results-document-subscription)**
(`hearing-results-document-subscription`).

The hrds writes structured JSON logs to stdout which are ingested into Log
Analytics via the Azure Monitor Agent. The dashboard queries the
`ContainerLogV2` table using KQL to produce charts and tables.


---

## Terraform

Dashboards are managed via Terraform in
[cp-amp-terraform-az-dashboard](https://github.com/hmcts/cp-amp-terraform-az-dashboard).
