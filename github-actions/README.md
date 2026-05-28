# github-actions

Demo module showing the full set of GitHub Actions workflows used across HMCTS services
(based on `service-hmcts-crime-springboot-template` and `service-cp-crime-hearing-results-document-subscription`).

The intent is to provide a reference implementation of each workflow so developers can understand
what runs, when it runs, and how to replicate it in a new service.

---

## Workflows to implement

### 1. `ci-draft.yml` — Build and Publish (Non-Release)
Triggered on **PR to main** and **push to main**.  
Calls the reusable `ci-build-publish.yml` workflow.  
On push: publishes artefact, builds Docker image, triggers ADO deploy pipeline.  
On PR: build and test only (no publish/deploy).

### 2. `ci-released.yml` — Build and Publish (Release)
Triggered when a **GitHub Release is published** or via `workflow_dispatch`.  
Validates that the API spec version is fixed (not a SNAPSHOT), then calls `ci-build-publish.yml`
with `is_release: true` — produces a versioned artefact, Docker image, and ADO deploy.

### 3. `ci-build-publish.yml` — Reusable CI pipeline
Called by `ci-draft` and `ci-released` via `workflow_call`.  
Jobs:
- **Artefact-Version** — uses `hmcts/artefact-version-action` to generate a draft or release version
- **Build** — `./gradlew build`, uploads JAR as a workflow artefact
- **Provider-Deploy** — `./gradlew publish` to Azure DevOps artifact feed and GitHub Packages
- **Build-Docker** — builds and pushes image to `ghcr.io` using `docker/build-push-action`
- **Deploy** — triggers ADO pipeline via `hmcts/trigger-ado-pipeline`

### 4. `code-analysis.yml` — PMD Static Analysis
Triggered on **PR to main**.  
Runs PMD against `src/main/java` using a project-level ruleset (`.github/pmd-ruleset.xml`).  
Fails the build if any violations are found.

### 5. `codeql.yml` — CodeQL + DAST
Triggered on **PR to main** and on a **weekly schedule**.  
Jobs:
- **analyze** — GitHub CodeQL security scan (`security-extended` queries), generates and uploads SBOM (CycloneDX)
- **DAST** — builds the app, starts it via `docker-compose`, runs OWASP ZAP baseline scan, uploads HTML report

### 6. `secrets-scanner.yml` — Secrets / Credential Leak Detection
Triggered on **PR to main**, **weekly schedule**, and `workflow_dispatch`.  
Uses `hmcts/secrets-scanner` (Gitleaks) to scan the full git history for committed secrets.

### 7. `auto-merge-dependabot.yml` — Dependabot Auto-merge
Triggered on any **pull request** raised by `dependabot[bot]`.  
Auto-approves and enables auto-merge so Dependabot dependency bumps merge without manual intervention.

---

## Secrets required

| Secret | Used by |
|---|---|
| `AZURE_DEVOPS_ARTIFACT_USERNAME` | ci-build-publish — publish to ADO feed |
| `AZURE_DEVOPS_ARTIFACT_TOKEN` | ci-build-publish — publish to ADO feed |
| `HMCTS_CP_ADO_PAT` | ci-build-publish — trigger ADO deploy pipeline |
| `GITLEAKS_LICENSE` | secrets-scanner |
| `HMCTS_CP_GITLEAKS_REGEX_INTERNAL_URL` | secrets-scanner — custom internal URL regex |

---

## Reference implementations

- [`service-hmcts-crime-springboot-template`](https://github.com/hmcts/service-hmcts-crime-springboot-template) — canonical source
- [`service-cp-crime-hearing-results-document-subscription`](https://github.com/hmcts/service-cp-crime-hearing-results-document-subscription) — live service using the same set
