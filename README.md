# Airepro Jenkins CI/CD

Job factory for **deploy + Playwright test + stage→prod promotion** across Airepro services.

| Repo | Role |
|------|------|
| **`jenkins`** (this repo) | Pipelines, Job DSL seed, shared library |
| **`airepro_cicd`** | All Playwright tests (`src/tests/{area}/`) |
| **`airepro-stage`** | Hire frontend + backend deploy (pilot) |

## Release flow (Hire pilot)

```
Airepro/release/hire-promote
  1. Deploy airepro-stage @ branch stage  →  stage.airepro.in / server.airepro.in
  2. Clone airepro_cicd, run Playwright tests (BASE_URL=server.airepro.in/api/v1)
  3. If green → deploy airepro-stage @ branch prod → production
  4. If red  → STOP (no production deploy)
```

## One-time Jenkins setup (~20 min)

### 1. Plugins

- **Pipeline**
- **Job DSL**
- **Pipeline: Groovy Libraries**
- **Git**

### 2. Register shared library

**Manage Jenkins → System → Global Pipeline Libraries → Add**

| Field | Value |
|-------|-------|
| Name | `airepro-pipeline` |
| Default version | `main` |
| Retrieval method | Modern SCM → Git |
| Project repository | URL of **this** `jenkins` repo |
| Credentials | `github-rajathakur` |

The library loads `vars/aireproDeploy.groovy`, `vars/aireproTest.groovy`, `vars/aireproPromote.groovy`.

### 3. Bootstrap seed job (first time only)

1. **New Item** → **Freestyle** → name: `seed-jobs-bootstrap`
2. **Build** → **Process Job DSLs** → look on filesystem OR paste from SCM
3. Point at `seed/seedJob.groovy` in this repo (or run Job DSL script manually once)
4. Build → creates `Airepro/` folder and all jobs
5. Delete bootstrap job; use **`Airepro/seed-jobs`** to regenerate later

**Alternative:** Create **`Airepro/seed-jobs`** manually as Pipeline from SCM → `seed/seedJob.groovy` won't work for DSL — use Freestyle with Job DSL build step for first run.

### 4. Credentials

- **`github-rajathakur`** — Git access to `airepro-stage`, `airepro_cicd`, `airepro-cicd`
- SSH keys on agent for `hireFrontend/deploy-docker.sh` prod deploy (if used)

## Jobs created by seed

| Job | Purpose |
|-----|---------|
| **`Airepro/release/hire-promote`** | Stage deploy → test → prod (recommended) |
| `Airepro/deploy/hire-stage` | Deploy stage branch only |
| `Airepro/deploy/hire-prod` | Manual prod (requires CONFIRM_PROD) |
| `Airepro/test/{area}` | Run one Playwright area from airepro_cicd |
| `Airepro/orchestrators/nightly-regression` | All test areas in parallel |
| `Airepro/seed-jobs` | Regenerate jobs after catalog changes |

## Configuration

| File | Purpose |
|------|---------|
| [config/environments.yaml](config/environments.yaml) | Stage vs prod branches and URLs |
| [config/test-defaults.yaml](config/test-defaults.yaml) | airepro_cicd repo defaults |
| [config/test-areas.json](config/test-areas.json) | Test area manifest (sync from airepro_cicd) |
| [services.yaml](services.yaml) | Service catalog |

### Stage URLs (Hire)

| Layer | URL |
|-------|-----|
| Frontend | `https://stage.airepro.in` |
| Backend API (tests) | **`https://server.airepro.in/api/v1`** |
| Git branch | `stage` |

### Production URLs (Hire)

| Layer | URL |
|-------|-----|
| Backend API | `https://vps.airepro.in/api/v1` |
| Git branch | `prod` |

## Local commands

```bash
# Sync test areas from airepro_cicd
node scripts/sync-test-areas.mjs --airepro-cicd=../airepro_cicd

# Validate catalog
node scripts/validate-catalog.js
```

In **airepro_cicd**:

```bash
npm run test:stage              # hire tests vs server.airepro.in
npm run test:area -- --area=ems --env=stage
npm run test:areas              # print JSON manifest
```

## Adding a new service

1. Add entry to `services.yaml` with `deploy.git_url`, `environments.stage/production`, `test.area`
2. Re-run **`Airepro/seed-jobs`**
3. Or use `pipelines/Jenkinsfile.create-service` (future)

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `@Library not found` | Register `airepro-pipeline` shared library |
| Tests hit wrong host | Set `ENV=stage` and `BASE_URL=https://server.airepro.in/api/v1` |
| Prod deployed without tests | Never use `SKIP_STAGE_GATE` except emergencies |
| Deploy script fails | Run on Linux agent with Docker, PM2, SSH as required by airepro-stage scripts |
