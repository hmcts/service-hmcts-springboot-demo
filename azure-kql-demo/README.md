# azure-kql-demo

KQL queries and a helper script for querying Azure Log Analytics via the Azure CLI.

Brought across from `service-cp-crime-hearing-results-document-subscription/support` — the pattern
is reusable across any HMCTS service that writes structured logs to a Log Analytics workspace.

---

## Folder structure

| Folder | Use |
|---|---|
| [`ams-kql/`](ams-kql/) | Azure Media Services (AMS) related queries — progression-service and sjp-service logs |

### `ams-kql/` query index

| Query | Description |
|---|---|
| `progression-service-logs.kql` | Progression service logs via `KubePodInventory` + `ContainerLog` join (legacy prod pattern) |
| `progression-service-logs-v2.kql` | Progression service logs via `ContainerLogV2` (dev/int pattern) |
| `sjp-service-logs.kql` | SJP service logs via `KubePodInventory` + `ContainerLog` join, filtered by correlation ID and errors |
| `sjp-service-logs-v2.kql` | SJP service logs via `ContainerLogV2`, filtered by correlation ID and errors |

---

## Running queries via Azure CLI

**Prerequisites:** `az` CLI and `jq` installed.

```bash
brew install jq                              # if not already installed
az extension add --name log-analytics        # one-time
az login
```

**Set your workspace ID** (Azure portal → your Log Analytics workspace → Overview → Workspace ID):

```bash
export WORKSPACE_ID=<your-workspace-guid>
```

Tip — add shell functions to `~/.zshrc` for quick environment switching:

```bash
kqlsit() { export WORKSPACE_ID=<sit-workspace-guid>; }
kqlprd() { export WORKSPACE_ID=<prd-workspace-guid>; }
```

**Run a query:**

```bash
./run-query.sh ams-kql/progression-service-logs-v2.kql
./run-query.sh ams-kql/progression-service-logs-v2.kql PT6H
./run-query.sh ams-kql/sjp-service-logs-v2.kql P1D --json
```

**List available queries:**

```bash
./run-query.sh
```

**Timespan** uses ISO 8601 duration format (default: `PT1H`):

| Value | Meaning |
|---|---|
| `PT30M` | last 30 minutes |
| `PT1H` | last 1 hour |
| `PT6H` | last 6 hours |
| `P1D` | last 1 day |
| `P7D` | last 7 days |

> Common mistake: `PT7D` is invalid — days are not a time component. Use `P7D`.

---

## Query patterns

### `ContainerLogV2` (dev/int)
Used where `ContainerLogV2` is available — simpler, filter directly on `PodName`:

```kql
ContainerLogV2
| where PodName startswith "progression-service-"
| order by TimeGenerated asc
```

### `KubePodInventory` + `ContainerLog` join (prod legacy)
Used in production where `ContainerLogV2` hasn't rolled out yet.
Joins pod inventory to get pod names, then queries `ContainerLog`:

```kql
let KubePodLogs = (clustername: string, podnameprefix: string) {
    let ContainerIdList = KubePodInventory
        | where ClusterName =~ clustername
        | where Name startswith strcat(podnameprefix, "-")
        | distinct ContainerID, PodLabel, Namespace, PodIp, Name;
    ContainerLog
        | where ContainerID in (ContainerIdList)
        | lookup kind=leftouter (ContainerIdList) on ContainerID
};
KubePodLogs('K8-PRD-CS01-CL02', 'progression-service')
| order by TimeGenerated asc
```

---

## Reference

- Full query library (HRDS service): [`service-cp-crime-hearing-results-document-subscription/support`](https://github.com/hmcts/service-cp-crime-hearing-results-document-subscription/tree/main/support)
- Log Analytics workspace: Azure portal → `LA-MPD-PRD-INT-WS`
