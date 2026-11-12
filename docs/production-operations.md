# Production operations

## Environments and secrets

Use `dev`, `test`, `staging`, or `prod` through
`SPRING_PROFILES_ACTIVE`. Development has safe localhost defaults. Staging and
production require `MONGODB_URI`; keep that value in the hosting provider's
secret store, never in Git. `.env.example` documents all supported variables.

The production process is stateless. It can be replaced or restarted without
local application data because MongoDB is external. Start with one backend
instance; the in-memory rate limiter intentionally assumes that topology.

## Health, metrics, and logs

- `/actuator/health/liveness` reports whether the process should be restarted.
- `/actuator/health/readiness` includes MongoDB connectivity and reports
  whether traffic should be sent to the process.
- `/actuator/prometheus` exposes request latency/error metrics plus
  `discover_recommendations_*` and `discover_weather_provider_failures_total`.
- Staging and production write Logstash-compatible structured JSON logs. Every
  API response and log context includes `X-Request-ID`/`requestId`.

Alert initially on readiness failures, elevated `http_server_requests` 5xx
rates, p95 request latency, weather-provider failures, and sustained empty
recommendation responses.

## Backups

Create a compressed MongoDB archive with:

```bash
MONGODB_URI='mongodb+srv://...' ./scripts/mongodb-backup.sh
```

Restore is deliberately guarded because it drops collections before restoring:

```bash
CONFIRM_RESTORE=YES MONGODB_URI='mongodb+srv://...' \
  ./scripts/mongodb-restore.sh backups/discover-vancouver-TIMESTAMP.archive.gz
```

Schedule managed MongoDB snapshots as the primary backup mechanism and retain
independent archives according to the project's recovery requirements. Run
`./scripts/verify-mongodb-backup.sh` periodically against the local Docker
MongoDB. It creates, backs up, drops, restores, verifies, and removes only the
`discover_vancouver_backup_verification` database.

## Deployment

Build and test the service:

```bash
mvn clean verify
docker build -t discover-vancouver-api:local .
```

For a generic HTTPS host, provision a managed MongoDB database, inject the
environment variables, deploy the container, and terminate TLS at the platform
load balancer. `compose.production.yml` is a one-instance reference topology;
it deliberately does not contain MongoDB, Redis, queues, or Kubernetes.

Set `DISCOVER_VANCOUVER_API_BASE_URL` to the deployed HTTPS origin when building
the Android release APK. Keep the trailing slash.

Before directing users to a release, verify readiness, one recommendation
request, structured logs, Prometheus scraping, and a backup restoration in a
non-production database.
