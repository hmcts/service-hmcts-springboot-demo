# Azure Monitor Alerts

Step-by-step guide for creating an Action Group (email + SMS) and an Alert Rule that fires when a specific error pattern appears in Log Analytics.

> ⚠️ **Prototyping only** — the portal (clickops) steps below are intended to help you understand and prototype alert configuration. Production alerts are deployed via Terraform in [cp-amp-terraform-alerts](https://github.com/hmcts/cp-amp-terraform-alerts). Do not manually create or modify alerts in production environments.

---

## Part 1 — Create an Action Group

An Action Group defines *who gets notified* and *how* when an alert fires.

### Step 1 — Navigate to Azure Monitor

From the Azure Portal home page click the **Monitor** icon under Azure services.

![Navigate to Azure Monitor from the portal home page](alerts-images/alerts-monitor1.png)

---

### Step 2 — Open Create → Action group

In **Monitor | Alerts** click **+ Create → Action group**.

![Create → Action group menu](alerts-images/alerts-create-action-group3.png)

---

### Step 3 — Fill in the Basics tab

Enter the subscription, resource group, action group name and display name.

> The display name is limited to 12 characters — it appears as the sender name on SMS messages.

![Create action group — Basics tab](alerts-images/alerts-create-action-group4.png)

---

### Step 4 — Add email and SMS notifications

Switch to the **Notifications** tab. Set Notification type to **Email/SMS message/Push/Voice** and fill in the email address and mobile number on the flyout panel. Click **OK**.

![Create action group — Notifications tab with email and SMS details](alerts-images/alerts-create-action-group5.png)

---

### Step 5 — Skip the Actions tab

The **Actions** tab is for webhooks, Logic Apps etc. Leave it empty for a basic email/SMS setup and click **Next: Tags >** then **Review + create**.

![Create action group — Actions tab (leave empty)](alerts-images/alerts-create-action-group6.png)

---

### Step 6 — Review and create

Confirm the summary shows the correct notification type (Email/SMS) and click **Create**.

![Create action group — Review + create summary](alerts-images/alerts-create-action-group7.png)

---

### Step 7 — Action group created

The action group overview confirms it is active with Email and SMS notifications configured.

![ColinsAG action group overview showing email and SMS notifications](alerts-images/alerts-action-group8.png)

---

## Part 2 — Create an Alert Rule

The alert rule watches Log Analytics and triggers the Action Group when the KQL condition is met.

### Step 1 — Open Create → Alert rule

In **Monitor | Alerts** click **+ Create → Alert rule**.

![Create → Alert rule menu](alerts-images/create-alert-rule1.png)

---

### Step 2 — Set the Scope

On the **Scope** tab click **+ Select scope** and choose your **Log Analytics Workspace** as the resource.

![Create alert rule — Scope tab](alerts-images/create-alert-scope2.png)

---

### Step 3 — Define the Condition (KQL query)

On the **Condition** tab set Signal name to **Custom log search** and paste your KQL query. The Log Analytics editor opens in a side panel so you can test the query before saving.

```kql
ContainerLogV2
| where PodNamespace == 'ns-dev-amp-01'
| where ContainerName != 'istio-proxy'
| where PodName contains 'hearing-results'
| where LogLevel == 'error'
| where LogMessage contains 'ResponseStatusException'
```

Set **Measure** to `Table rows`, **Operator** to `Greater than or equal to`, **Threshold** to `1`.

![Create alert rule — Condition tab with custom log search KQL](alerts-images/create-alert-rule-kql3.png)

---

### Step 4 — Attach the Action Group

On the **Actions** tab select **Use action groups**, click **+ Select action groups** and choose the group created in Part 1. You can also customise the **Email subject** here.

![Create alert rule — Actions tab with action group selected](alerts-images/create-alert-ag4.png)

---

### Step 5 — Fill in the Details tab

On the **Details** tab set the subscription, resource group, severity, alert rule name and region.

![Create alert rule — Details tab](alerts-images/create-alert-rule-details5.png)

---

### Step 6 — Review and create

The **Review + create** tab shows a summary of scope, condition, actions and estimated monthly cost. Click **Create**.

![Create alert rule — Review + create summary](alerts-images/create-alert-review6.png)

---

### Step 7 — Alert rule created

The alert rule overview shows the scope (workspace), the KQL condition, and the linked action group. Use **Edit**, **Disable**, **Duplicate** or **Delete** from the toolbar to manage it.

![ColinsTestRule alert rule overview](alerts-images/CreateRuleDone7.png)

---

## Part 3 — Alert fires

When the KQL condition is matched you will receive both an email and an SMS.

### Email notification

An email arrives from `azure-noreply@microsoft.com` with the alert name, why it fired, and the metric values that crossed the threshold.

![Alert fired email notification](alerts-images/RuleFiredEmail8.png)

---

### SMS notification

An SMS arrives within seconds showing the severity, alert rule name, and workspace — useful for immediate awareness even without internet access.

![Alert fired SMS notification](alerts-images/RuleFiredSms9.png)
