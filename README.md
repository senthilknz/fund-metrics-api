# Active Series Fund Metrics API

A Spring Boot REST API that serves Active Series managed fund metric data to a micro frontend. Fund data is stored as versioned JSON config files embedded in the application — no database required.

---

## Table of Contents

- [Local Development](#local-development)
- [How Versioning Works](#how-versioning-works)
- [Adding a New Quarterly Config — End-to-End Workflow](#adding-a-new-quarterly-config--end-to-end-workflow)
- [Verification Checklist](#verification-checklist)
- [Rollback Options](#rollback-options)
- [API Design — Config vs Chooser Response](#api-design--config-vs-chooser-response)
- [API Endpoints](#api-endpoints)
- [Swagger UI](#swagger-ui)
- [Request / Response Logging](#request--response-logging)
- [Running Tests](#running-tests)
- [CORS](#cors)

---

## Local Development

**Requirements:** Java 21+. Maven wrapper is included — no separate Maven installation needed.

```bash
# Run locally
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build JAR
./mvnw package
java -jar target/fund-metrics-api-0.0.1-SNAPSHOT.jar
```

API starts on `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## How Versioning Works

Config files live in:

```
src/main/resources/fund-configs/
  funds-config-v1.json    ← effectiveFrom: 2025-01-01
  funds-config-v2.json    ← effectiveFrom: 2025-04-01
  funds-config-v3.json    ← effectiveFrom: 2025-07-01  (example next quarter)
```

**The rule:** the active config is always the latest file whose `effectiveFrom` date is not after today.

- All config files are packaged into the JAR at build time.
- On startup, all files are loaded and sorted by `effectiveFrom`.
- Every midnight, the scheduler re-evaluates which version is active based on the current date.
- This means you can **deploy a new config file weeks before its activation date** — the old version keeps serving until midnight on the `effectiveFrom` date, then the new one activates automatically with no further action required.

---

## Adding a New Quarterly Config — End-to-End Workflow

This is the full process followed by the squad when the fund manager requests a data update.

> **Diagram:** see [`docs/quarterly-config-workflow.puml`](docs/quarterly-config-workflow.puml) for the full end-to-end sequence diagram (PlantUML). Render it in IntelliJ with the PlantUML plugin, or paste into [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/).

The diagram covers all participants — Fund Manager, Developer, Tester, Git, CloudBees, Spinnaker, SYST, UAT, ServiceNow, Prod, and the midnight scheduler — and shows exactly who does what at each phase.

### Step 1 — Fund manager request

The fund manager provides the new quarterly return figures and the date from which they should go live (e.g. 1 July 2025).

---

### Step 2 — Developer creates a branch

```bash
git checkout master
git pull
git checkout -b feature/fund-metrics-q3-2025
```

---

### Step 3 — Developer adds the new config file

Create a new file in `src/main/resources/fund-configs/`:

```bash
cp src/main/resources/fund-configs/funds-config-v2.json \
   src/main/resources/fund-configs/funds-config-v3.json
```

Edit `funds-config-v3.json` and update **all** of the following fields:

| Field | What to change |
|---|---|
| `"version"` | Increment to next version, e.g. `"3.0.0"` |
| `"effectiveFrom"` | First day of the new quarter, e.g. `"2025-07-01"` |
| `"publishedAt"` | Today's ISO timestamp, e.g. `"2025-06-15T00:00:00Z"` |
| `"performanceAsOf"` | Last day of the previous quarter, e.g. `"2025-06-30"` |
| `"funds[*].returns"` | All return values provided by the fund manager |

> Do **not** modify or delete any existing config files. All versions must remain in the repo — they form the audit history and support the `/history` and `/preview` endpoints.

**Verify the new file locally before raising the PR:**

```bash
./mvnw spring-boot:run

# Confirm v3 is in history
curl http://localhost:8080/api/v1/funds/history | python3 -m json.tool

# Preview what will serve on the activation date
curl "http://localhost:8080/api/v1/funds/preview?date=2025-07-01" | python3 -m json.tool

# Confirm current active is still the previous version (before effectiveFrom)
curl http://localhost:8080/api/v1/funds | python3 -m json.tool
```

---

### Step 4 — Developer raises a Pull Request

```bash
git add src/main/resources/fund-configs/funds-config-v3.json
git commit -m "feat: add Q3 2025 fund metrics config (effective 2025-07-01)"
git push origin feature/fund-metrics-q3-2025
```

Raise a PR against `master`. The PR description should include:
- Which quarter this covers
- The `effectiveFrom` date
- Confirmation that the preview endpoint was tested locally
- Sign-off from the fund manager on the return figures

---

### Step 5 — PR approved and merged to master

Another developer reviews and approves. On merge to `master`:

- CloudBees automatically triggers a new build.
- The build runs `./mvnw package`, executes all tests, and produces a new JAR containing the new config file.
- CloudBees creates a new task in Spinnaker on successful build.

---

### Step 6 — Developer deploys to SYST via Spinnaker

Trigger the Spinnaker pipeline for the SYST environment.

Once deployed to SYST, verify:

```bash
# Replace with your SYST hostname
SYST_HOST=https://fund-metrics-api.syst.example.com

# All versions loaded correctly (should include v1, v2, v3)
curl $SYST_HOST/api/v1/funds/history

# Active version is still the previous one (effectiveFrom not yet reached)
curl $SYST_HOST/api/v1/funds

# Preview confirms v3 will serve correctly on the activation date
curl "$SYST_HOST/api/v1/funds/preview?date=2025-07-01"
```

---

### Step 7 — Developer deploys to UAT via Spinnaker

Trigger the Spinnaker pipeline for the UAT environment.

Testers verify the new fund data on UAT using the same checks as SYST. They can use the `/preview` endpoint to confirm the new figures without waiting for the activation date:

```bash
UAT_HOST=https://fund-metrics-api.uat.example.com
curl "$UAT_HOST/api/v1/funds/preview?date=2025-07-01" | python3 -m json.tool
```

---

### Step 8 — Developer raises a Change Request in ServiceNow

Raise a CR in ServiceNow referencing:
- The Spinnaker pipeline and build number
- The `effectiveFrom` date (when the data goes live to users, not when the deploy happens)
- The PR / commit that introduced the change
- UAT sign-off from the testers

> Note: the CR covers the deployment to Prod. The fund data itself does not go live until midnight on the `effectiveFrom` date, so there is no user-facing impact at deploy time.

---

### Step 9 — Developer deploys to Prod via Spinnaker

Once the CR is approved, trigger the Spinnaker pipeline for the Prod environment.

Post-deploy verification on Prod:

```bash
PROD_HOST=https://fund-metrics-api.example.com

# Confirm the new version is embedded and visible in history
curl $PROD_HOST/api/v1/funds/history

# Active version is still the previous one — effectiveFrom not yet reached
curl $PROD_HOST/api/v1/funds

# Preview confirms correct data will serve on the activation date
curl "$PROD_HOST/api/v1/funds/preview?date=2025-07-01"
```

**From this point, no further action is needed.** At midnight on 1 July 2025, the scheduler automatically activates v3. The micro frontend receives the new data on its next request via the ETag revalidation mechanism.

---

## Verification Checklist

Use this checklist when verifying a config update at each stage.

```
[ ] funds/history includes the new version (e.g. 3.0.0)
[ ] funds/history shows versions in correct effectiveFrom order
[ ] funds (active) still returns the previous version before effectiveFrom date
[ ] funds/preview?date=<effectiveFrom> returns the new version with correct figures
[ ] Return values in preview match the figures signed off by the fund manager
[ ] All existing tests pass: ./mvnw test
```

---

## Rollback Options

If bad data is deployed, there are three options depending on urgency.

### Option 1 — In-memory hold (immediate, no redeploy)

Freeze the active version in memory without redeploying. Buys time to fix the data.

```bash
# Roll back to v2 immediately
curl -X POST "https://fund-metrics-api.example.com/api/v1/funds/activate?version=2.0.0"
# Response: { "success": true, "message": "Activated version: 2.0.0" }
```

> Resets automatically on the next app restart or midnight scheduler tick. Raise a CR and deploy a proper fix before then.

### Option 2 — Fix-forward (preferred for data corrections)

Add a corrected config file with today's date as `effectiveFrom` and go through the full pipeline. Keeps the full audit trail intact.

```
funds-config-v3.json       ← bad data (do not delete — audit trail)
funds-config-v3.1.json     ← corrected data, effectiveFrom: today
```

### Option 3 — Remove and redeploy

Delete the bad config file, commit, and redeploy. The previous version auto-activates. Use this if the file itself is the problem (e.g. malformed JSON that prevents startup).

```bash
git rm src/main/resources/fund-configs/funds-config-v3.json
git commit -m "fix: remove malformed v3 config"
git push
# Raise emergency CR → deploy via Spinnaker
```

---

## API Design — Config vs Chooser Response

The config files and the `/chooser` response are intentionally shaped differently. This separation is best practice and follows the **BFF (Backend For Frontend)** pattern.

```
funds-config-v2.json                      ← raw data, complete, no UI concerns
        ↓
FundConfigService.toChooserResponse()     ← mapping layer
        ↓
GET /api/v1/funds/chooser                 ← view model shaped for the fund chooser UI
```

| Layer | Purpose |
|-------|---------|
| Config JSON | Source of truth — all return periods, full metric descriptions, version metadata |
| `/chooser` response | View model — only what the chooser page needs, labels and descriptions co-located with values |

**Why not align the config to the chooser response shape?**

- The config serves multiple endpoints (`/funds`, `/history`, `/preview`) — shaping it for one UI would break the others
- The config holds all 6 return periods; `/chooser` intentionally exposes only the 5-year figure
- Display strings ("Fee", "Return", "Time", "Risk") are presentation concerns — they belong in the mapping layer, not the data layer
- Future UIs (mobile app, adviser portal) may need different shapes — a new mapping method handles that without touching the config

---

## API Endpoints

### `GET /api/v1/funds`
Returns the currently active fund config.

Uses `ETag` and `Cache-Control: no-cache` so the micro frontend always gets the latest data without changing its URL. Returns `304 Not Modified` when data is unchanged (zero bandwidth), `200` with fresh data when a new version has activated.

```bash
curl http://localhost:8080/api/v1/funds
```

---

### `GET /api/v1/funds/chooser`
Returns fund metrics shaped specifically for the fund chooser page.

Each metric embeds its own `label` and `description` alongside the value — the micro frontend can render every card without cross-referencing a separate `metricDescriptions` map, knowing which return period to display, or hardcoding any display strings client-side. Only the 5-year return is included; all other periods are omitted.

Supports the same `ETag` / `304` caching semantics as `GET /api/v1/funds`.

```bash
curl http://localhost:8080/api/v1/funds/chooser
```

Example response:
```json
{
  "disclaimer": "Past performance is not a reliable indication of future performance.",
  "performanceAsOf": "2025-03-31",
  "funds": [
    {
      "id": "growth",
      "name": "Growth Fund",
      "fee": {
        "value": 0.85,
        "unit": "%",
        "label": "Fee",
        "description": "85c per $100 of your balance per year"
      },
      "estimatedReturn": {
        "value": 6.33,
        "unit": "%",
        "periodValue": 5,
        "periodUnit": "years",
        "label": "Return",
        "description": "Estimated average annual return over 5 years"
      },
      "minInvestmentTimeframe": {
        "value": 10,
        "unit": "years",
        "label": "Time",
        "description": "Recommended min. investment time"
      },
      "riskIndicator": {
        "value": 4,
        "scaleMin": 1,
        "scaleMax": 7,
        "label": "Risk",
        "description": "How much the fund goes up and down"
      }
    }
  ]
}
```

---

### `GET /api/v1/funds/history`
Returns all embedded config versions in ascending `effectiveFrom` order. Use this to confirm a new config file was picked up correctly after a deploy.

```bash
curl http://localhost:8080/api/v1/funds/history
```

---

### `GET /api/v1/funds/preview?date=YYYY-MM-DD`
Returns the config that would be active on the given date without affecting the live data. Primary QA tool — use this to verify new return figures before the activation date is reached.

```bash
# What serves on the activation date?
curl "http://localhost:8080/api/v1/funds/preview?date=2025-07-01"

# What was serving in the previous quarter?
curl "http://localhost:8080/api/v1/funds/preview?date=2025-03-15"
```

---

### `POST /api/v1/funds/activate?version=x.x.x`
Force-activates a specific version in memory. For emergency rollback only. Resets on next restart or midnight tick.

```bash
curl -X POST "http://localhost:8080/api/v1/funds/activate?version=2.0.0"
# { "success": true, "message": "Activated version: 2.0.0" }
```

---

## Swagger UI

| URL | Purpose |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs — try all endpoints in the browser |
| `http://localhost:8080/v3/api-docs` | Raw OpenAPI JSON spec |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI YAML spec |

---

## Request / Response Logging

All `/api/**` requests are logged at `INFO` level:

```
>>> GET /api/v1/funds | query="" | content-type=
<<< 200 | duration=12ms | content-type=application/json

>>> POST /api/v1/funds/activate | query="version=2.0.0" | content-type=
<<< 200 | duration=3ms | content-type=application/json | body={"success":true,...}
```

Response bodies over 2000 characters are truncated. Actuator, Swagger, and `/v3/api-docs` paths are excluded.

---

## Actuator

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
```

---

## Running Tests

```bash
./mvnw test
```

| Test class | What it covers |
|---|---|
| `FundConfigServiceTest` | Both config files load; v1 active before 2025-04-01; v2 active on/after 2025-04-01; `forceActivateVersion` switches correctly; unknown version returns false |
| `FundControllerTest` | All five endpoints (including `/chooser`); ETag returns correct version string; `304` when ETag matches; `200` when ETag differs (new version activated); error cases |

---

## CORS

`GET` and `POST` on `/api/**` are allowed from:

- `http://localhost:3000` — local micro frontend dev server
- `https://your-microfrontend.example.com` — production micro frontend
