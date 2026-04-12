# Active Series Fund Metrics API

A Spring Boot REST API that serves Active Series managed fund metric data to a micro frontend. Fund data is stored as versioned JSON config files embedded in the application — no database required.

---

## Table of Contents

- [Local Development](#local-development)
- [How Versioning Works](#how-versioning-works)
- [Config File Structure](#config-file-structure)
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

Config files use **Calendar Versioning (CalVer)** — the filename and `version` field both carry the date the file was authored, in `YYYY.MM.DD` format.

```
src/main/resources/fund-configs/
  funds-config-2025.04.01.json    ← effectiveFrom: 2025-04-01T00:00:00  (current)
  funds-config-2025.07.01.json    ← effectiveFrom: 2025-07-01T09:00:00  (example — 9am activation)
```

**The rule:** the active config is always the latest file whose `effectiveFrom` (NZT date-time) is not after the current NZT time.

- All config files are packaged into the JAR at build time.
- On startup, all files are loaded and sorted by `effectiveFrom`.
- At startup a **precise one-time `TaskScheduler` task** is armed to fire at the exact `effectiveFrom` instant of the next future config. When it fires, it activates the new config and immediately arms the task for the one after that. Zero unnecessary firings between quarterly updates.
- A **daily midnight `@Scheduled` cron** re-evaluates and re-schedules as a safety net — it handles the edge case where the app restarts after a missed activation or a JVM clock correction cancels the one-time task.
- You can **deploy a new config file weeks before its activation date** — the previous version keeps serving until the specified NZT date-time, then the new one activates automatically with no deployment or manual step required.

> **Why CalVer?** Fund configs are tied to specific effective dates, not to feature releases. A version like `2025.07.01` is self-documenting — anyone reading it immediately knows when it goes live, without cross-referencing a changelog.

> **NZT convention:** `effectiveFrom` is always interpreted as a NZT (Pacific/Auckland) local date-time. There is no timezone suffix in the JSON value — the NZT context is applied by the scheduler. Use midnight (`T00:00:00`) when time-of-day precision is not needed.

### Diagrams

| Diagram | File | Purpose |
|---------|------|---------|
| Component | [`docs/component-diagram.puml`](docs/component-diagram.puml) | System structure — components and their relationships |
| Sequence — Quarterly Workflow | [`docs/quarterly-config-workflow.puml`](docs/quarterly-config-workflow.puml) | Who does what, step by step, from fund manager request to automatic activation |
| Sequence — Request Flow | [`docs/request-flow.puml`](docs/request-flow.puml) | How a micro frontend request travels through the API at runtime |

Render any `.puml` file in IntelliJ with the PlantUML plugin, or paste the contents into [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml/).

---

## Config File Structure

Each config file is a self-contained JSON document. Below is a minimal annotated example.

```jsonc
{
  // CalVer: YYYY.MM.DD — the date this file was authored.
  // Used as the ETag value on the /funds endpoint.
  "version": "2025.07.01",

  // NZT date-time from which this config becomes the active version.
  // A one-time TaskScheduler task fires at this exact instant.
  // Use T00:00:00 for midnight or any specific time (e.g. T09:00:00).
  "effectiveFrom": "2025-07-01T00:00:00",

  // ISO-8601 timestamp of when this file was published.
  "publishedAt": "2025-06-15T00:00:00Z",

  // Last day of the period these returns cover.
  "performanceAsOf": "2025-06-30",

  "dataSource": "Active Series",
  "disclaimer": "Past performance is not a reliable indication of future performance.",

  // Keyed paragraphs — the frontend can show/hide individual notes by key.
  "footerNotes": [
    { "key": "sustainability", "text": "..." },
    { "key": "return",         "text": "..." },
    { "key": "risk",           "text": "..." },
    { "key": "riaa-certification", "text": "..." }
  ],

  // Global display metadata for each metric type.
  // Labels, units, and scale bounds live here — shared across all funds.
  // Per-fund tooltip copy lives on each fund (see below).
  "metricDescriptions": {
    "fee": {
      "label": "Fee",
      "description": "{cents}c per $100 of your balance per year",  // {cents} is replaced at runtime
      "unit": "%"
    },
    "returns": {
      "label": "Return",
      "description": "Estimated average annual return over 5 years",
      "unit": "%",
      "periods": ["3months","6months","1year","3years","5years","10years"],
      "chooserPeriod": "5years"   // which period to show on the chooser card
    },
    "minInvestmentTimeframe": {
      "label": "Time",
      "description": "Recommended min. investment time",
      "unit": "years"
    },
    "riskIndicator": {
      "label": "Risk",
      "description": "How much the fund goes up and down",
      "scale": "1 (lowest) to 7 (highest)",
      "scaleMin": 1,
      "scaleMax": 7
    }
  },

  "funds": [
    {
      "id": "growth",
      "name": "Growth Fund",
      "description": "Higher long-term capital growth. ...",
      "fee": { "annualFundCharge": 0.85, "unit": "%" },

      // returns is an object — period values and tooltip copy travel together.
      // This means a newly-launched fund (no 5-year history) simply provides
      // different tooltip text without any structural change.
      "returns": {
        "values": {
          "3months": 1.92, "6months": 8.74, "1year": 13.11,
          "3years": 12.85, "5years": 6.33, "10years": 7.88
        },
        "tooltip": "Average annual return is over five years after deducting annual fund charges but before tax to 31 December 2025.",
        "tooltipLink": null    // optional — omit or set null if not needed
      },

      "minInvestmentTimeframe": { "value": 10, "unit": "years" },

      // riskIndicator also owns its own tooltip copy.
      // The PDS link differs between Active Series and KiwiSaver funds.
      "riskIndicator": {
        "value": 4,
        "label": "Medium",
        "tooltip": "How much the fund goes up and down in value ...",
        "tooltipLink": {
          "text": "Westpac Active Series Product Disclosure Statement",
          "url": "https://www.westpac.co.nz/kiwisaver/active-series/pds/"
        }
      }
    }
    // ... more funds
  ]
}
```

### What to update each quarter

| Field | What to set |
|---|---|
| `version` | Today's date in `YYYY.MM.DD` format, e.g. `"2025.07.01"` |
| `effectiveFrom` | NZT date-time to activate, e.g. `"2025-07-01T00:00:00"` (midnight) or `"2025-07-01T09:00:00"` (9am) |
| `publishedAt` | Today's ISO timestamp, e.g. `"2025-06-15T00:00:00Z"` |
| `performanceAsOf` | Last day of the period the returns cover, e.g. `"2025-06-30"` |
| `funds[*].returns.values` | New return figures signed off by the fund manager |
| `funds[*].returns.tooltip` | Update the date reference if the copy changes |
| `funds[*].riskIndicator.tooltip` | Update only if FMA risk indicator copy changes |
| `footerNotes` | Add, remove, or edit paragraphs as needed |

> **Do not modify or delete existing config files.** All versions must remain in the repo — they form the audit history and support the `/history` and `/preview` endpoints.

---

## Adding a New Quarterly Config — End-to-End Workflow

This is the full process followed by the squad when the fund manager requests a data update.

> **See [`docs/quarterly-config-workflow.puml`](docs/quarterly-config-workflow.puml)** for the complete sequence diagram covering all participants — Fund Manager, Developer, Tester, Git, CloudBees, Spinnaker, SYST, UAT, ServiceNow, Prod, and the midnight scheduler.

### Step 1 — Fund manager request

The fund manager provides the new quarterly return figures and the activation date (e.g. 1 July 2025).

### Step 2 — Create a branch

```bash
git checkout main
git pull
git checkout -b feature/fund-metrics-q3-2025
```

### Step 3 — Add the new config file

Copy the latest config file and give it a CalVer filename matching the activation date:

```bash
cp src/main/resources/fund-configs/funds-config-2025.04.01.json \
   src/main/resources/fund-configs/funds-config-2025.07.01.json
```

Edit `funds-config-2025.07.01.json` and update all fields listed in the table above.

**Verify locally before raising the PR:**

```bash
./mvnw spring-boot:run

# Confirm new version is in history
curl http://localhost:8080/api/v1/funds/history | python3 -m json.tool

# Preview what will serve on the activation date
curl "http://localhost:8080/api/v1/funds/preview?date=2025-07-01" | python3 -m json.tool

# Confirm current active is still the previous version (effectiveFrom not yet reached)
curl http://localhost:8080/api/v1/funds | python3 -m json.tool

# Check the chooser response reflects the new figures (when previewing)
curl "http://localhost:8080/api/v1/funds/chooser" | python3 -m json.tool
```

### Step 4 — Raise a Pull Request

```bash
git add src/main/resources/fund-configs/funds-config-2025.07.01.json
git commit -m "feat: add Q3 2025 fund metrics config (effective 2025-07-01)"
git push origin feature/fund-metrics-q3-2025
```

PR description should include:
- Quarter covered and `effectiveFrom` date
- Confirmation that `/preview` was tested locally
- Fund manager sign-off on the return figures

### Step 5 — CI Build (CloudBees)

On merge to `main`, CloudBees automatically:
1. Runs `./mvnw package` — embeds the new config file in the JAR
2. Runs all tests — confirms the new version loads and `effectiveFrom` resolves correctly
3. Publishes the build artefact and creates a Spinnaker task

### Step 6 — Deploy to SYST, verify

```bash
SYST=https://fund-metrics-api.syst.example.com

curl $SYST/api/v1/funds/history                           # new version present?
curl "$SYST/api/v1/funds/preview?date=2025-07-01"        # figures correct?
curl $SYST/api/v1/funds                                   # still serving previous version?
```

### Step 7 — Deploy to UAT, tester sign-off

```bash
UAT=https://fund-metrics-api.uat.example.com
curl "$UAT/api/v1/funds/preview?date=2025-07-01" | python3 -m json.tool
```

Tester confirms the return figures match the fund manager's sign-off.

### Step 8 — Raise a Change Request in ServiceNow

Reference the Spinnaker build number, the `effectiveFrom` date, and UAT sign-off.

> The CR covers the **deployment to Prod**. The fund data itself goes live at midnight NZT on `effectiveFrom` — no second CR or on-call action is required for the automatic activation.

### Step 9 — Deploy to Prod

```bash
PROD=https://fund-metrics-api.example.com

curl $PROD/api/v1/funds/history                           # new version present?
curl $PROD/api/v1/funds                                   # still serving previous version?
curl "$PROD/api/v1/funds/preview?date=2025-07-01"        # preview correct?
```

**No further action needed.** At midnight NZT on 2025-07-01, the scheduler fires `refreshActiveConfig()`, the new version becomes active, and the micro frontend receives fresh data on its next request via the ETag revalidation mechanism.

---

## Verification Checklist

Use at every stage (local, SYST, UAT, Prod).

```
[ ] funds/history includes the new version (e.g. 2025.07.01)
[ ] funds/history shows all versions in ascending effectiveFrom order
[ ] funds (active) still returns the previous version before effectiveFrom date
[ ] funds/preview?date=<effectiveFrom> returns the new version
[ ] Return values in preview match the figures signed off by the fund manager
[ ] funds/chooser shows the correct 5-year return, fee, timeframe, and risk
[ ] All existing tests pass: ./mvnw test
```

---

## Rollback Options

### Option 1 — In-memory hold (immediate, no redeploy)

Force-activates a specific version in memory without redeploying. Buys time to prepare a proper fix.

Available only on the **management port (8081)** — not exposed on the public API port.

```bash
curl -X POST http://<host>:8081/actuator/fund-activate \
  -H "Content-Type: application/json" \
  -d '{"version": "2025.04.01"}'
# { "success": true, "message": "Activated version: 2025.04.01" }
```

> Resets automatically on the next restart or midnight scheduler tick. Raise a CR and deploy a fix before then.

### Option 2 — Fix-forward (preferred for data corrections)

Add a corrected config file with today's date as `effectiveFrom` and go through the full pipeline. The audit trail stays intact.

```
funds-config-2025.07.01.json     ← bad data (do not delete — audit trail)
funds-config-2025.07.02.json     ← corrected data, effectiveFrom: day after
```

### Option 3 — Remove and redeploy

Delete the bad config file, commit, and redeploy. The previous version auto-activates. Use this only if the file is malformed and preventing startup.

```bash
git rm src/main/resources/fund-configs/funds-config-2025.07.01.json
git commit -m "fix: remove malformed 2025.07.01 config"
git push
# Raise emergency CR → deploy via Spinnaker
```

---

## API Design — Config vs Chooser Response

The config files and the `/chooser` response are intentionally shaped differently. This separation follows the **BFF (Backend For Frontend)** pattern.

```
funds-config-2025.04.01.json          ← raw data, complete, all return periods
          ↓
FundConfigService.toChooserResponse() ← mapping layer (BFF)
          ↓
GET /api/v1/funds/chooser             ← view model shaped for the fund chooser UI
```

| Layer | Purpose |
|-------|---------|
| Config JSON | Source of truth — all return periods, full metric metadata, per-fund tooltip copy, version info |
| `/chooser` response | View model — only the chooser page's needs: one return period, labels and values co-located, tooltips embedded |

**Why not shape the config to match the chooser response?**

- The config serves four endpoints (`/funds`, `/chooser`, `/history`, `/preview`) — shaping it for one UI would couple data to presentation
- The config holds all 6 return periods; `/chooser` intentionally exposes only the 5-year figure
- Future UIs (mobile app, adviser portal) can have their own mapping methods without touching the config

---

## API Endpoints

### `GET /api/v1/funds`

Returns the currently active fund config.

Uses `ETag` (`"YYYY.MM.DD"`) and `Cache-Control: no-cache` so the micro frontend always revalidates. Returns `304 Not Modified` when data is unchanged, `200` with fresh data when a new version has activated.

```bash
curl http://localhost:8080/api/v1/funds
```

---

### `GET /api/v1/funds/chooser`

Returns fund metrics shaped for the fund chooser page. Each metric embeds its own `label`, `description`, and `tooltip` alongside the value — no client-side joins required.

```bash
curl http://localhost:8080/api/v1/funds/chooser
```

Example response (abbreviated):

```json
{
  "disclaimer": "Past performance is not a reliable indication of future performance.",
  "performanceAsOf": "2025-03-31",
  "footerNotes": [
    { "key": "sustainability", "text": "..." },
    { "key": "return",         "text": "..." },
    { "key": "risk",           "text": "..." },
    { "key": "riaa-certification", "text": "..." }
  ],
  "funds": [
    {
      "id": "growth",
      "name": "Growth Fund",
      "fee": {
        "value": 0.85, "unit": "%",
        "label": "Fee",
        "description": "85c per $100 of your balance per year"
      },
      "estimatedReturn": {
        "value": 6.33, "unit": "%",
        "periodValue": 5, "periodUnit": "years",
        "label": "Return",
        "description": "Estimated average annual return over 5 years",
        "tooltip": "Average annual return is over five years after deducting annual fund charges but before tax to 31 December 2025.",
        "tooltipLink": null
      },
      "minInvestmentTimeframe": {
        "value": 10, "unit": "years",
        "label": "Time",
        "description": "Recommended min. investment time"
      },
      "riskIndicator": {
        "value": 4, "scaleMin": 1, "scaleMax": 7,
        "label": "Risk",
        "description": "How much the fund goes up and down",
        "tooltip": "How much the fund goes up and down in value ...",
        "tooltipLink": {
          "text": "Westpac Active Series Product Disclosure Statement",
          "url": "https://www.westpac.co.nz/kiwisaver/active-series/pds/"
        }
      }
    }
  ]
}
```

---

### `GET /api/v1/funds/history`

Returns all embedded config versions in ascending `effectiveFrom` order. Use this to confirm a new config file was picked up after a deploy.

```bash
curl http://localhost:8080/api/v1/funds/history
```

---

### `GET /api/v1/funds/preview?date=YYYY-MM-DD`

Returns the config that would be active on the given date without affecting the live data. Primary QA tool — use this to verify new return figures before the activation date arrives.

```bash
# What will serve on the activation date?
curl "http://localhost:8080/api/v1/funds/preview?date=2025-07-01"

# What was serving last quarter?
curl "http://localhost:8080/api/v1/funds/preview?date=2025-03-15"
```

---

## Emergency Rollback (management port only)

The public API has no activation endpoint. Emergency version override is available exclusively on the management port (8081), which is firewalled to internal/ops access:

```bash
curl -X POST http://localhost:8081/actuator/fund-activate \
  -H "Content-Type: application/json" \
  -d '{"version": "2025.04.01"}'
# { "success": true, "message": "Activated version: 2025.04.01" }
```

Resets on the next restart or midnight scheduler tick. See [Rollback Options](#rollback-options) for the full decision tree.

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

>>> POST /api/v1/funds/activate | query="version=2025.04.01" | content-type=
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
| `FundConfigServiceTest` | Config loads; correct version active on/before/after `effectiveFrom`; `forceActivateVersion` switches correctly; per-fund tooltip content; chooser response shape |
| `FundControllerTest` | All five endpoints; ETag returns CalVer version string; `304` when ETag matches; `200` when ETag differs; error cases (503, 404, 400) |
| `FundConfigServiceEdgeCaseTest` | No configs loaded; malformed file skipped; `toChooserResponse` returns null when no active config |

---

## CORS

`GET` and `POST` on `/api/**` are allowed from:

- `http://localhost:3000` — local micro frontend dev server
- `https://your-microfrontend.example.com` — production micro frontend
