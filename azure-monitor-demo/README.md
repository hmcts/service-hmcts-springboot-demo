# Azure Monitor Alert Demo

Walkthrough for setting up an **Action Group** (email + SMS) and an **Alert Rule** that fires when a specific error pattern appears in Log Analytics.

---

## Prerequisites

- An Azure subscription with an existing **Log Analytics Workspace** that receives application logs.
- The application must be shipping logs at `ERROR` level to that workspace (e.g. via the Azure Monitor agent or a Spring Boot `logback-spring.xml` appender targeting Application Insights / Log Analytics).
- Sufficient RBAC role: **Monitoring Contributor** (or higher) on the resource group.

---

## Step 1 — Create an Action Group

An Action Group defines *who gets notified* and *how*.

### Azure Portal

1. Go to **Azure Monitor → Alerts → Action groups → + Create**.
2. Fill in the **Basics** tab:

   | Field | Value |
   |---|---|
   | Subscription | *(your subscription)* |
   | Resource group | *(your resource group)* |
   | Action group name | `ag-colin-email-sms` |
   | Display name | `Colin Alerts` *(max 12 chars shown in SMS)* |

3. Switch to the **Notifications** tab and add two notifications:

   | Notification type | Name | Detail |
   |---|---|---|
   | Email/SMS/Push/Voice | `Email Colin` | ✅ Email: `colingreenwood@scrumconnect.com` |
   | Email/SMS/Push/Voice | `SMS Colin` | ✅ SMS: `+44 <your mobile number>` |

4. Click **Review + create → Create**.

### Azure CLI equivalent

```bash
az monitor action-group create \
  --name ag-colin-email-sms \
  --resource-group <your-rg> \
  --short-name "ColinAlerts" \
  --action email EmailColin colingreenwood@scrumconnect.com \
  --action sms   SmsColin   44 <your-mobile-number-without-leading-0>
```

---

## Step 2 — Create the Alert Rule

The alert watches Log Analytics for `ERROR` level log entries containing the subscription-conflict message.

### 2a — Open the Alert Rule wizard

1. Go to **Azure Monitor → Alerts → + Create → Alert rule**.
2. Under **Scope**, select your **Log Analytics Workspace**.

### 2b — Condition (Log search query)

1. Click **+ Add condition**.
2. Signal type: **Custom log search**.
3. Paste the KQL query:

```kql
AppTraces
| where SeverityLevel == 3                          // 3 = Error in Application Insights
| where Message contains "subscription already exist with 37370ba3-d1c7-4e42-b95d-37eddf25ed29"
| summarize AggregatedValue = count() by bin(TimeGenerated, 5m)
```

> **Note:** If your workspace uses the classic `traces` table instead of `AppTraces`, replace `AppTraces` with `traces` and `SeverityLevel` with `severityLevel`.

4. Set the alert logic:

   | Setting | Value |
   |---|---|
   | Operator | Greater than |
   | Threshold value | `0` |
   | Aggregation granularity | 5 minutes |
   | Frequency of evaluation | Every 5 minutes |

### 2c — Actions tab

1. Click **+ Select action groups**.
2. Choose `ag-colin-email-sms` created in Step 1.

### 2d — Details tab

| Field | Value |
|---|---|
| Alert rule name | `alert-subscription-conflict` |
| Description | Fires when a duplicate-subscription error for 37370ba3-d1c7-4e42-b95d-37eddf25ed29 appears in logs |
| Severity | **1 – Error** |
| Enable upon creation | ✅ |

5. Click **Review + create → Create**.

### Azure CLI equivalent

```bash
# 1. Get the action group resource ID
AG_ID=$(az monitor action-group show \
  --name ag-colin-email-sms \
  --resource-group <your-rg> \
  --query id -o tsv)

# 2. Get the Log Analytics workspace resource ID
WS_ID=$(az monitor log-analytics workspace show \
  --workspace-name <your-workspace-name> \
  --resource-group <your-rg> \
  --query id -o tsv)

# 3. Create the scheduled-query alert rule
az monitor scheduled-query create \
  --name alert-subscription-conflict \
  --resource-group <your-rg> \
  --scopes "$WS_ID" \
  --condition-query "AppTraces | where SeverityLevel == 3 | where Message contains 'subscription already exist with 37370ba3-d1c7-4e42-b95d-37eddf25ed29' | summarize AggregatedValue = count() by bin(TimeGenerated, 5m)" \
  --condition-operator GreaterThan \
  --condition-threshold 0 \
  --condition-time-aggregation Count \
  --evaluation-frequency 5m \
  --window-size 5m \
  --severity 1 \
  --description "Fires when duplicate-subscription error 37370ba3 appears in logs" \
  --action-groups "$AG_ID"
```

---

## Step 3 — Verify

1. **Test the Action Group** immediately after creation:
   - Portal: open the action group → **Test** → select *Email/SMS Colin* → **Test action group**.
   - You should receive a test email at `colingreenwood@scrumconnect.com` and a test SMS within ~1 minute.

2. **Trigger the alert rule** by injecting a matching log line from your app:
   ```java
   log.error("subscription already exist with 37370ba3-d1c7-4e42-b95d-37eddf25ed29");
   ```
   Log Analytics ingestion lag is typically 2–5 minutes. The alert should fire on the next 5-minute evaluation cycle after ingestion.

3. Check **Azure Monitor → Alerts → Alert history** to confirm the rule fired and notifications were dispatched.

---

## Step 4 — Add context to alert notifications

The default email action only tells you the alert fired — it contains no log rows.
There are two levels of improvement:

### Option A — Quick win: add a deep-link in the alert description (2 minutes)

Azure Monitor injects a handful of dynamic placeholders into the alert description at fire time.
Edit the alert rule **Details** tab and set the description to:

```
Duplicate subscription error detected for 37370ba3-d1c7-4e42-b95d-37eddf25ed29.

Matched rows:   {{ResultCount}}
Window start:   {{SearchIntervalStartTimeUtc}}
Window end:     {{SearchIntervalEndTimeUtc}}
Search results: {{LinkToFilteredSearchResultsUI}}
```

`{{LinkToFilteredSearchResultsUI}}` becomes a direct URL into Log Analytics scoped to the exact time window that fired the alert — one click to see the rows.

> Available placeholders for log-search alerts: `{{ResultCount}}`, `{{LinkToSearchResults}}`, `{{LinkToFilteredSearchResultsUI}}`, `{{SearchIntervalStartTimeUtc}}`, `{{SearchIntervalEndTimeUtc}}`, `{{AlertThresholdOperator}}`, `{{AlertThresholdValue}}`, `{{SearchQuery}}`.

---

### Option B — Rich email with actual log rows via Logic App

A Logic App sits between the alert and your inbox. When the alert fires it POSTs the alert payload to the Logic App HTTP trigger; the Logic App re-queries Log Analytics, formats the matching rows into an HTML table, and sends a proper email.

#### 4a — Create the Logic App

1. **Azure Portal → Create a resource → Logic App → Consumption plan**.
2. Name it `la-alert-subscription-conflict`, same resource group.

#### 4b — Build the workflow (portal designer)

Add the following steps in order:

**Trigger — When a HTTP request is received**
- Method: `POST`
- Leave the schema blank for now; it will auto-populate from the first real alert payload.

---

**Action 1 — Run query against Log Analytics**
- Connector: *Azure Monitor Logs*
- Action: **Run query and list results**
- Workspace: *(select your workspace)*
- Query:
  ```kql
  AppTraces
  | where SeverityLevel == 3
  | where Message contains "subscription already exist with 37370ba3-d1c7-4e42-b95d-37eddf25ed29"
  | where TimeGenerated > ago(10m)
  | project TimeGenerated, Message, AppRoleName, SeverityLevel, Properties
  | order by TimeGenerated desc
  | take 20
  ```

---

**Action 2 — Send an email (Office 365 / Outlook connector)**
- Connector: *Office 365 Outlook* → **Send an email (V2)**
- To: `colingreenwood@scrumconnect.com`
- Subject: `🚨 Alert fired: subscription conflict 37370ba3`
- Body (switch to **Code view** to paste HTML):

```html
<h2>Subscription conflict alert fired</h2>
<p><b>Rule:</b> alert-subscription-conflict<br/>
<b>Time:</b> @{utcNow()}</p>

<h3>Matching log rows (last 10 min)</h3>
<table border="1" cellpadding="4" cellspacing="0">
  <tr>
    <th>Time</th>
    <th>Service</th>
    <th>Message</th>
  </tr>
  @{body('Run_query_and_list_results')?['value']}
</table>
```

> For a proper per-row table use a **For each** loop over `body('Run_query_and_list_results')?['value']` and build a `<tr>` per iteration.

---

#### 4c — Wire the Logic App into the Action Group

1. Copy the **HTTP POST URL** from the Logic App trigger (shown after first save).
2. Open `ag-colin-email-sms` → **Edit**.
3. Go to the **Actions** tab → **+ Add action**:

   | Field | Value |
   |---|---|
   | Action type | Logic App |
   | Name | `RichEmailColin` |
   | Logic App | `la-alert-subscription-conflict` |

4. Save the action group.

The SMS notification from Step 1 still fires (good for immediate awareness); the Logic App email arrives a few seconds later with the actual log rows.

#### 4d — Logic App ARM template (optional, for IaC)

```json
{
  "definition": {
    "$schema": "https://schema.management.azure.com/providers/Microsoft.Logic/schemas/2016-06-01/workflowdefinition.json#",
    "triggers": {
      "manual": { "type": "Request", "kind": "Http", "inputs": { "method": "POST" } }
    },
    "actions": {
      "Run_LA_Query": {
        "type": "ApiConnection",
        "inputs": {
          "host": { "connection": { "name": "@parameters('$connections')['azuremonitorlogs']['connectionId']" } },
          "method": "post",
          "path": "/queryData",
          "body": {
            "query": "AppTraces | where SeverityLevel == 3 | where Message contains '37370ba3-d1c7-4e42-b95d-37eddf25ed29' | where TimeGenerated > ago(10m) | project TimeGenerated, Message, AppRoleName | order by TimeGenerated desc | take 20",
            "timerange": "Last 10 minutes"
          }
        }
      },
      "Send_Email": {
        "type": "ApiConnection",
        "runAfter": { "Run_LA_Query": ["Succeeded"] },
        "inputs": {
          "host": { "connection": { "name": "@parameters('$connections')['office365']['connectionId']" } },
          "method": "post",
          "path": "/v2/Mail",
          "body": {
            "To": "colingreenwood@scrumconnect.com",
            "Subject": "🚨 Alert: subscription conflict 37370ba3",
            "Body": "<p>Matching rows:</p>@{body('Run_LA_Query')?['value']}",
            "Importance": "High"
          }
        }
      }
    }
  }
}
```

---

## Useful KQL — ad-hoc investigation

```kql
// All error-level messages in the last hour
AppTraces
| where SeverityLevel == 3
| where TimeGenerated > ago(1h)
| project TimeGenerated, Message, AppRoleName, Properties
| order by TimeGenerated desc

// Count of the specific conflict error over time
AppTraces
| where SeverityLevel == 3
| where Message contains "37370ba3-d1c7-4e42-b95d-37eddf25ed29"
| summarize count() by bin(TimeGenerated, 1h)
| render timechart
```

---

## KQL tips — dashboards and local time

### Pinning a query to a dashboard

1. Run your query in Log Analytics.
2. Click **Save → Pin to dashboard**.
3. In the pin dialog switch to the **Existing** tab and explicitly pick your target dashboard from the dropdown — if you accept the default it goes to whichever dashboard was last active, which is often *"My Dashboard"*.
4. To find a misplaced tile: **Portal → Dashboard → *(dashboard name dropdown top-left)* → Browse all dashboards**.

---

### Displaying local time instead of UTC

The Azure Portal labels any `datetime` column as `[UTC]` regardless of what you name it. The fix is to convert to a **string** with `format_datetime()` after converting the timezone — a string column carries no UTC label.

```kql
ContainerLogV2
| where PodNamespace  == 'ns-dev-amp-01'
| where TimeGenerated > ago(1h)
| where ContainerName != 'istio-proxy'
| where PodName contains 'hearing-results-document-subscription'
| extend LogJson  = parse_json(LogMessage)
| extend Message  = tostring(LogJson.message)
| extend Level    = tostring(LogJson.level)
| extend LocalTime = datetime_utc_to_local(TimeGenerated, 'Europe/London')  // BST/GMT aware
| order by LocalTime desc                                                    // sort on datetime before converting
| take 500
| project
    ['LocalTime'] = format_datetime(LocalTime, 'yyyy-MM-dd HH:mm:ss'),     // string = no [UTC] label
    Level,
    Message
```

**Key points:**
- `datetime_utc_to_local(..., 'Europe/London')` handles BST/GMT switching automatically — no hardcoded `+1h`.
- `format_datetime()` converts to string, which removes the `[UTC]` portal label.
- Always `order by` on the `datetime` typed column *before* `project` converts it to a string.
- Keep `where TimeGenerated > ago(...)` filters on the original UTC column — it is indexed; derived columns are not.

---

### Extracting JSON fields from LogMessage

Log messages are often a JSON blob. Use `parse_json()` to crack it open, then dot-notation for individual fields. Filter on cheap columns first to minimise the number of rows that hit the expensive parse.

```kql
ContainerLogV2
| where TimeGenerated > ago(1h)           // 1. indexed time filter first
| where PodNamespace == 'ns-dev-amp-01'   // 2. cheap column filters
| where LogMessage contains "37370ba3"    // 3. raw string scan before parsing
| extend LogJson = parse_json(LogMessage) // 4. parse only surviving rows
| extend Message = tostring(LogJson.message)
| extend Level   = tostring(LogJson.level)
| extend LocalTime = datetime_utc_to_local(TimeGenerated, 'Europe/London')
| order by LocalTime desc
| take 100
| project
    ['LocalTime'] = format_datetime(LocalTime, 'yyyy-MM-dd HH:mm:ss'),
    Level,
    Message
```

**Type conversions:**

| JSON value | KQL cast |
|---|---|
| string | `tostring(LogJson.field)` |
| integer | `toint(LogJson.field)` |
| decimal | `todouble(LogJson.field)` |
| boolean | `tobool(LogJson.field)` |
| timestamp | `todatetime(LogJson.field)` |

**Explore unknown JSON structure:**
```kql
ContainerLogV2
| where TimeGenerated > ago(15m)
| take 5
| extend LogJson = parse_json(LogMessage)
| project LogJson
```
Expand the result tree in the portal to discover all available field names.

---

### Zero-fill gaps in time charts

`summarize` only returns buckets where data exists, leaving gaps in charts. Use `make-series` with `default=0` to fill them:

```kql
ContainerLogV2
| where PodNamespace == 'ns-dev-amp-01'
| where LogMessage contains "ERROR"
| make-series ErrorCount = count() default=0
    on TimeGenerated
    from ago(24h) to now()
    step 5m
| render timechart
```

---

## Dashboard — hearing-results-document-subscription

The file [`my-dashboard.json`](./my-dashboard.json) is an exportable Azure Portal dashboard definition. It contains 5 tiles laid out across 3 rows and is scoped to workspace `la-mdv-dev-int-ws` in resource group `rg-mdv-dev-int-01`.

### Layout

```
┌─────────────────────────────┬─────────────────────────────┐
│ Received Notifications      │ Received Notifications      │
│ Last 7 days by hour         │ Last 12 weeks by day        │
│ (line chart)                │ (line chart)                │
├─────────────────────────────┼─────────────────────────────┤
│ Errors By Hour              │ Errors                      │
│ Last 7 days (line chart)    │ Last 7 days (table, top 50) │
├─────────────────────────────┴─────────────────────────────┤
│ Logs — last 12 hours, local time (table, top 500)         │
└───────────────────────────────────────────────────────────┘
```

### Importing the dashboard

1. **Azure Portal → Dashboard → + New dashboard → Import**.
2. Select `my-dashboard.json`.
3. The dashboard will appear under your private dashboards — rename it if needed.

> The JSON is tied to subscription `e6b5053b-4c38-4475-a835-a025aeb3d8c7` and workspace `la-mdv-dev-int-ws`. If you need to point it at a different workspace, find and replace the `resourceIds` value throughout the JSON before importing.

### Exporting an updated dashboard

If you edit tiles in the portal and want to save the changes back to source:

1. **Dashboard → Edit → ... (top right) → Download**.
2. Replace `my-dashboard.json` in this folder with the downloaded file.

---

## Dashboard queries — hearing-results-document-subscription

### 1. Error counts (timechart — last 7 days, hourly buckets)

```kql
ContainerLogV2
| where PodNamespace  == 'ns-dev-amp-01'
| where TimeGenerated > ago(7d)
| where ContainerName != 'istio-proxy'
| where PodName contains 'hearing-results'
| where LogLevel == 'error'
| where LogMessage contains 'ResponseStatusException'
| project TimeGenerated, LogLevel, LogMessage
| summarize ErrorCount = count() by bin(TimeGenerated, 1h)
| render timechart
```

### 2. Inbound notifications (timechart — last 84 days, daily buckets)

```kql
ContainerLogV2
| where PodNamespace  == 'ns-dev-amp-01'
| where TimeGenerated > ago(84d)
| where ContainerName != 'istio-proxy'
| where PodName contains 'hearing-results'
| where LogMessage contains 'Received notification request'
| project TimeGenerated, LogLevel, LogMessage
| make-series ReceivedNotification = count() default=0
    on TimeGenerated
    from ago(84d) to now()
    step 1d
| render timechart
```

### 3. Errors detail (table — last 7 days, most recent 50)

```kql
ContainerLogV2
| where PodNamespace  == 'ns-dev-amp-01'
| where TimeGenerated > ago(7d)
| where ContainerName != 'istio-proxy'
| where PodName contains 'hearing-results'
| where LogLevel == 'error'
| extend LogJson = parse_json(LogMessage)
| extend Message = tostring(LogJson.message)
| extend Level   = tostring(LogJson.level)
| order by TimeGenerated desc
| take 50
| project TimeGenerated, Message
```

### 4. Logs (table — last 12 hours, local time, most recent 500)

```kql
ContainerLogV2
| where PodNamespace  == 'ns-dev-amp-01'
| where TimeGenerated > ago(12h)
| where ContainerName != 'istio-proxy'
| where PodName contains 'hearing-results-document-subscription'
| extend LogJson   = parse_json(LogMessage)
| extend Message   = tostring(LogJson.message)
| extend Level     = tostring(LogJson.level)
| extend LocalTime = datetime_utc_to_local(TimeGenerated, 'Europe/London')
| order by LocalTime desc
| take 500
| project
    ['LocalTime'] = format_datetime(LocalTime, 'yyyy-MM-dd HH:mm:ss'),
    Level,
    Message
```
