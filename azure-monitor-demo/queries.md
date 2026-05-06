# KQL Queries

KQL queries for monitoring `hearing-results-document-subscription` in Log Analytics workspace `la-mdv-dev-int-ws`.

---

## Dashboard queries

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

---

## Ad-hoc investigation

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

## KQL tips

### Display local time instead of UTC

The Azure Portal labels any `datetime` column as `[UTC]` regardless of what you name it. Convert to a **string** with `format_datetime()` to remove the label.

```kql
| extend LocalTime = datetime_utc_to_local(TimeGenerated, 'Europe/London')  // BST/GMT aware
| order by LocalTime desc                                                    // sort on datetime before converting
| project
    ['LocalTime'] = format_datetime(LocalTime, 'yyyy-MM-dd HH:mm:ss'),     // string = no [UTC] label
    ...
```

**Key points:**
- `datetime_utc_to_local(..., 'Europe/London')` handles BST/GMT switching automatically — no hardcoded `+1h`.
- `format_datetime()` converts to string, removing the `[UTC]` portal label.
- Always `order by` on the `datetime` typed column *before* `project` converts it to a string.
- Always filter on `TimeGenerated` not the derived `LocalTime` — `TimeGenerated` is indexed.

---

### Extract JSON fields from LogMessage

Log messages are often a JSON blob. Filter on cheap columns first to minimise rows that hit the expensive `parse_json()`.

```kql
ContainerLogV2
| where TimeGenerated > ago(1h)           // 1. indexed time filter first
| where PodNamespace == 'ns-dev-amp-01'   // 2. cheap column filters
| where LogMessage contains "37370ba3"    // 3. raw string scan before parsing
| extend LogJson = parse_json(LogMessage) // 4. parse only surviving rows
| extend Message = tostring(LogJson.message)
| extend Level   = tostring(LogJson.level)
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

### Chart types

| Type | Best for |
|---|---|
| `timechart` | Multiple series over time |
| `areachart` | Volume over time, filled |
| `stackedareachart` | Multiple series stacked — proportions over time |
| `columnchart` | Vertical bars comparing categories |
| `barchart` | Horizontal bars comparing categories |
| `piechart` | Proportions of a whole |
| `scatterchart` | Correlation between two numeric values |
| `ladderchart` | Event sequences and durations |
| `table` | Default grid view |

```kql
// Optional title/axis labels
| render columnchart with (title="Errors by service", xtitle="Service", ytitle="Count")
```
