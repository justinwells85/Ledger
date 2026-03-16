# Spec 19: Transactional Logging (Developer Trace System)

## Overview

This spec defines a developer-facing transactional logging system for Ledger. It is not user-visible and is intended solely to help developers diagnose issues like "the user submitted a form, the backend appeared to succeed, but the data is not visible in the UI."

The system correlates frontend click events, outbound API calls, backend service layer operations, and database writes into a single trace tied together by a `correlationId`. A developer can query all events for a given `correlationId` to reconstruct exactly what happened during a user action from click to screen update.

All log entries carry a 7-day TTL enforced by a nightly scheduled cleanup job. The endpoints are restricted to admin-authenticated users and can be disabled entirely in production via the `ledger.dev-logging.enabled` configuration flag (see [Spec 18: Admin Configuration](18-admin-configuration.md) for the established pattern).

**This spec is design-only. No implementation is included here.**

---

## 1. Goals

- Provide a correlated, end-to-end trace for any user action in the system.
- Tie together: browser click → HTTP request → Spring service method → repository write → HTTP response → React state update.
- Keep all trace data developer-only; never expose to end users via normal UI flows.
- Automatically expire entries after 7 days to limit storage growth.
- Allow the entire system to be disabled in production with a single config flag.

### Non-Goals (Future Scope)

- Real-time streaming of events via Server-Sent Events.
- Alerting or monitoring on `ERROR` events.
- Promoting events to the user-visible audit trail (deferred to P6-S6; see [Spec 11: Change Management](11-change-management.md)).

---

## 2. Correlation ID

A correlation ID is a UUID generated on the frontend at the moment a meaningful user action begins (button click, form submit). It flows through every layer of the system for the lifetime of that action.

### Lifecycle

1. Frontend generates `correlationId = uuid()` when a user initiates an action.
2. The ID is sent as the `X-Correlation-ID` HTTP header on every API request associated with that action.
3. The backend extracts the header in a `CorrelationFilter` and stores it in both MDC (for structured logging) and a request-scoped bean.
4. All `TraceService` writes for that request include the same `correlationId`.
5. Fallback: if no `X-Correlation-ID` header is present (background jobs, health checks), the backend generates a new UUID and uses it for the duration of that request.

### Rules

- One user action = one `correlationId`.
- A single `correlationId` may span multiple HTTP requests if the action requires them (e.g., a form submit followed by an immediate data reload); the frontend must reuse the same ID for all requests in that action sequence.
- `correlationId` values are never reused.

---

## 3. Frontend Event Types

The frontend captures the following event types and batches them for delivery to the backend. Events are fire-and-forget: the frontend POSTs the batch and does not await the response or block the UI.

| Event Type | When Emitted | Key Fields |
|---|---|---|
| `USER_ACTION` | User clicks a button or submits a form | `correlationId`, `action` (e.g. `"SUBMIT_NEW_CONTRACT"`), `component`, `timestamp` |
| `API_REQUEST` | Immediately before `fetch()` is called | `correlationId`, `method`, `path`, `timestamp` |
| `API_RESPONSE` | After `fetch()` resolves or rejects | `correlationId`, `method`, `path`, `status`, `durationMs`, `success`, `timestamp` |
| `UI_STATE_CHANGE` | After data is re-fetched and React state is updated | `correlationId`, `component`, `changeType` (e.g. `"DATA_LOADED"`), `dataKey`, `timestamp` |

Frontend events are submitted via `POST /api/v1/dev/trace/batch`. Each batch contains an array of event objects. The batch size is capped at `ledger.dev-logging.batch-max-size` (default: 50) to prevent oversized payloads.

---

## 4. Backend Event Types

The backend captures the following event types automatically via filter and AOP instrumentation.

| Event Type | When Emitted | Key Fields |
|---|---|---|
| `API_RECEIVED` | On every incoming request, in the correlation filter | `correlationId`, `method`, `path`, `userId`, `timestamp` |
| `SERVICE_CALL` | Entry into a `@Service` method that performs a mutation | `correlationId`, `service`, `method`, `entityType`, `entityId`, `timestamp` |
| `DB_WRITE` | After a `repository.save()` or `repository.delete()` | `correlationId`, `operation` (`INSERT`/`UPDATE`/`DELETE`), `table`, `entityId`, `timestamp` |
| `API_RESPONSE` | Request completion, in the correlation filter | `correlationId`, `status`, `durationMs`, `timestamp` |
| `ERROR` | Any unhandled exception | `correlationId`, `errorType`, `message`, `timestamp` |

`SERVICE_CALL` events are captured only for mutations, not reads. Read-only service methods (getters, finders) are excluded from AOP instrumentation to avoid noise.

### Backend Instrumentation Components

**`CorrelationFilter`** (`OncePerRequestFilter`)
- Runs on every request.
- Extracts `X-Correlation-ID` from the request header, or generates a new UUID if absent.
- Stores the ID in MDC under `correlationId` and in a request-scoped `CorrelationContext` bean.
- Writes `API_RECEIVED` at request start and `API_RESPONSE` at request completion.
- Clears MDC on exit.

**`TraceService`**
- Accepts structured event data and persists rows to `transaction_log`.
- Used directly by `CorrelationFilter` and by other services/AOP aspects as needed.
- Reads `correlationId` from `CorrelationContext`.

**`TraceAspect`** (`@Aspect`)
- Pointcut targets `@Service`-annotated classes.
- Intercepts mutation methods (identified by name convention or a custom `@Traced` annotation).
- Writes `SERVICE_CALL` before the method body executes.
- Writes `DB_WRITE` events after confirmed `repository.save()` / `repository.delete()` calls (injected as advice around repository calls, or triggered explicitly from service methods).

---

## 5. Database Schema

### Table: `transaction_log`

```sql
CREATE TABLE transaction_log (
    id              BIGSERIAL PRIMARY KEY,
    correlation_id  UUID NOT NULL,
    source          VARCHAR(20) NOT NULL,   -- 'FRONTEND' or 'BACKEND'
    event_type      VARCHAR(30) NOT NULL,
    component       VARCHAR(200),           -- service name, React component name, or API path
    entity_type     VARCHAR(50),
    entity_id       VARCHAR(100),
    event_data      JSONB,                  -- flexible per-event payload
    user_id         VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_txlog_correlation ON transaction_log(correlation_id);
CREATE INDEX idx_txlog_created_at  ON transaction_log(created_at);
```

- `source`: `FRONTEND` for events submitted via the batch endpoint; `BACKEND` for events written by the filter and AOP layer.
- `event_data`: a JSONB column holding all event-specific fields not covered by the fixed columns (e.g. `durationMs`, `status`, `action`, `changeType`).
- `created_at` is indexed to support the TTL cleanup query.

### Flyway Migration

File: `src/main/resources/db/migration/V009__transaction_log.sql`

---

## 6. API Endpoints (Dev-Only)

All endpoints are under the `/api/v1/dev/` prefix and are restricted to `ROLE_ADMIN` in `SecurityConfig`:

```java
requestMatchers("/api/v1/dev/**").hasRole("ADMIN")
```

If `ledger.dev-logging.enabled` is `false`, the backend returns `503 Service Unavailable` for all `/api/v1/dev/trace/**` requests.

### POST /api/v1/dev/trace/batch

Accepts an array of frontend trace events. The frontend calls this fire-and-forget after batching events.

- Request body: JSON array of event objects (max `batch-max-size` entries).
- Response: `202 Accepted` with no body.
- Silently drops events if the batch exceeds the size limit rather than rejecting the entire request.

### GET /api/v1/dev/trace/{correlationId}

Returns all events for a given `correlationId`, ordered by `created_at` ascending.

- Path variable: `correlationId` (UUID string).
- Response: JSON array of `TransactionLogEntry` objects representing the full trace.
- Returns `404 Not Found` if no events exist for the given ID.

### GET /api/v1/dev/trace

Returns recent traces matching optional query filters. Results are always limited to events within the last 7 days.

Query parameters:

| Parameter | Type | Description |
|---|---|---|
| `from` | ISO-8601 datetime | Lower bound on `created_at` |
| `to` | ISO-8601 datetime | Upper bound on `created_at` |
| `userId` | String | Filter by `user_id` |
| `eventType` | String | Filter by `event_type` |

Response: JSON array of matching log entries.

---

## 7. 7-Day TTL Cleanup

A Spring `@Scheduled` task runs nightly to purge entries older than 7 days:

```java
@Scheduled(cron = "0 0 2 * * *")  // 2:00 AM daily
public void purgeOldTraces() {
    transactionLogRepository.deleteByCreatedAtBefore(
        Instant.now().minus(7, ChronoUnit.DAYS)
    );
}
```

The `ttl-days` value is read from `ledger.dev-logging.ttl-days` so it can be adjusted without a code change (see Section 10: Configuration).

The cleanup uses the `idx_txlog_created_at` index for efficient range deletes.

---

## 8. Frontend Integration Points

The following locations in the frontend codebase are the primary integration points.

### `api/client.ts` — Automatic Request/Response Events

Wrap the existing `request()` function to automatically emit `API_REQUEST` before calling `fetch()` and `API_RESPONSE` after it resolves or rejects. A module-level `currentCorrelationId` variable holds the ID set by the current user action. The wrapper attaches `X-Correlation-ID: <currentCorrelationId>` to every outbound request header.

### Key UI Action Handlers — USER_ACTION Events

When a user clicks a meaningful button (Submit, Save, Reconcile, Commit, etc.), the handler must:

1. Generate a new `correlationId = uuid()`.
2. Set `currentCorrelationId` in the API client module.
3. Emit a `USER_ACTION` event to the trace batch queue.
4. Proceed with the normal action (form submission, API call, etc.).

Action names should be descriptive constants (e.g. `SUBMIT_NEW_CONTRACT`, `SAVE_PROJECT`, `COMMIT_SAP_IMPORT`).

### `useApi` Hook — UI_STATE_CHANGE Events

After `setData(d)` is called to load data into React state, emit a `UI_STATE_CHANGE` event with the relevant `component`, `changeType` (e.g. `DATA_LOADED`), and `dataKey` (e.g. the resource name or route).

### Trace Batch Queue

Events are accumulated in a module-level queue. A flush function sends the queue contents to `POST /api/v1/dev/trace/batch` as a fire-and-forget call (no `await`, no error handling that would affect the UI). The queue is flushed after each user action completes (or on a short debounce timer) to avoid holding events indefinitely.

---

## 9. Primary Test Flows

The logging system must be validated end-to-end against the following flows. These flows are the primary scenarios referenced in `20-e2e-scenario-matrix.md`.

### P1-S1: Create Contract

Expected trace (in order):

1. `FRONTEND / USER_ACTION` — action: `SUBMIT_NEW_CONTRACT`
2. `FRONTEND / API_REQUEST` — `POST /api/v1/contracts`
3. `BACKEND / API_RECEIVED` — `POST /api/v1/contracts`
4. `BACKEND / SERVICE_CALL` — `ContractService.createContract`
5. `BACKEND / DB_WRITE` — `INSERT` on `contract`
6. `BACKEND / API_RESPONSE` — status `201`
7. `FRONTEND / API_RESPONSE` — status `201`, success: true
8. `FRONTEND / UI_STATE_CHANGE` — component navigated to contract detail

### P1-S2: Add Project

Expected trace includes `DB_WRITE` on `project` and a `UI_STATE_CHANGE` event confirming the projects list was reloaded.

### P3-S3: Reconcile Actual

Expected trace includes two `DB_WRITE` events: one for `reconciliation` and one for `journal_entry`.

### P2-S4: SAP Import Commit

Expected trace includes a `DB_WRITE` for the `sap_import` status update followed by multiple `DB_WRITE` events for `actual_line` inserts.

---

## 10. Configuration

The dev-logging system is controlled via `application.yml`:

```yaml
ledger:
  dev-logging:
    enabled: true          # set to false in production to disable all /api/v1/dev/trace endpoints
    ttl-days: 7            # number of days before log entries are purged
    batch-max-size: 50     # maximum number of events accepted per frontend batch POST
```

The `enabled` flag follows the same pattern established in [Spec 18: Admin Configuration](18-admin-configuration.md) for `ledger.*` feature flags. When `enabled: false`:

- The `CorrelationFilter` still extracts/generates a correlation ID and populates MDC (for structured log correlation), but it does not write to `transaction_log`.
- All `/api/v1/dev/trace/**` endpoints return `503 Service Unavailable`.
- The `TraceAspect` is a no-op.
- The nightly cleanup job does not run.

The frontend should check the enabled state via a bootstrap config endpoint (or simply tolerate `503` responses on the batch endpoint gracefully without surfacing errors to the user).

---

## 11. Security Considerations

- All query endpoints (`GET /api/v1/dev/trace/**`) require `ROLE_ADMIN`.
- The batch ingest endpoint (`POST /api/v1/dev/trace/batch`) is called by the browser without interactive authentication; it should be IP-restricted in production environments or, preferably, disabled entirely via `ledger.dev-logging.enabled: false`.
- Trace data may contain user IDs and entity IDs but should not contain PII field values; `event_data` payloads must be reviewed to confirm they do not capture form field contents (passwords, free-text notes, etc.).
- Log entries are not exposed through any user-facing API or UI component.

---

## 12. Future Scope

The following items are explicitly out of scope for this spec and deferred to later work:

- **User-facing audit trail**: Selected `SERVICE_CALL` and `DB_WRITE` events may eventually be promoted to the user-visible audit log established in Spec 25 (Entity Audit Log). The `transaction_log` schema is intentionally separate to keep the developer trace system independent.
- **Real-time streaming**: A Server-Sent Events endpoint could push new trace events to an open developer console tab without polling.
- **Error alerting**: `ERROR` events in `transaction_log` could trigger notifications or dashboard counters for proactive issue detection.
