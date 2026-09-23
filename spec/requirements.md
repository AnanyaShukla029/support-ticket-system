# Requirements

Support Ticket Management System: create, list, view, update, comment on, search, and filter tickets; persist them in a database; enforce a ticket status state machine on the backend; validate input server-side; show meaningful errors in the UI.

Stack (project constraint): Java 21, Spring Boot, PostgreSQL (runtime) / Testcontainers for integration tests, React/Next.js. HTTP style follows `.cursor/rules/api-standards.md`. Lifecycle details that repeat this document belong in `spec/state-machine.md` when that file is filled.

## Functional Requirements

### FR-1 Create ticket

- A user can create a ticket from the UI.
- Create requires a **title**, **description**, and **priority**. **Assignee** may be set on create or left unset.
- A new ticket is persisted with status **`OPEN`**.
- The UI shows the created ticket (or navigates to its details) after success.

### FR-2 List tickets

- A user can list tickets from the UI.
- The list shows at least: identifier, title, status, priority, assignee (or empty), and last-updated time.
- Collection responses use the pagination envelope defined in `.cursor/rules/api-standards.md` (`items`, `page`, `size`, `totalItems`, `totalPages`).

### FR-3 View ticket details

- A user can open a single ticket and see its details: title, description, priority, assignee, status, timestamps, and comments (oldest first unless specified otherwise in `spec/ui-flow.md`).
- A request for an unknown ticket id is not found (`404`); the UI shows a meaningful error, not a blank page presented as success.

### FR-4 Update ticket fields

- A user can update **title**, **description**, **priority**, and **assignee** on an existing ticket.
- Assignee can be changed independently of other fields (including set or cleared), using the same update capability.
- Status is **not** a free-form field on this update: status changes go through the state machine (FR-8).
- Updates are rejected when the ticket does not exist (`404`) or when field validation fails (NFR-V).

### FR-5 Comments

- A user can add a **comment** (non-empty body) on an existing ticket.
- Comments are stored with the ticket and shown on the details view after a successful add.
- Adding a comment on a missing ticket is `404`.
- Comments are append-only in this version (no edit/delete comment requirement).

### FR-6 Search by keyword

- A user can search tickets by a keyword from the UI.
- The backend matches the keyword against **title** and **description** (case-insensitive substring is sufficient unless `spec/api-contract.md` later specifies full-text).
- Results use the same list representation and pagination as FR-2. Empty match set is a successful empty list, not an error.

### FR-7 Filter by status

- A user can filter the ticket list by **status** (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`).
- An unknown status value is a validation error (`400`), not an empty list.
- Search and status filter may be combined on the list endpoint.

### FR-8 Status state machine (backend-enforced)

The backend is the authority. The UI may hide illegal actions but **must not** be the only check.

**Allowed transitions**

```
OPEN         → IN_PROGRESS
IN_PROGRESS  → RESOLVED
RESOLVED     → CLOSED

OPEN         → CANCELLED
IN_PROGRESS  → CANCELLED
```

**Terminal states:** `CLOSED`, `CANCELLED` (no outbound transitions).

**Invalid transitions must be rejected** by the backend (HTTP `409`, conflict with current state — see `.cursor/rules/api-standards.md`). Examples that must fail:

| From | To | Result |
|---|---|---|
| `CLOSED` | `OPEN` | Reject |
| `RESOLVED` | `OPEN` | Reject |
| `CANCELLED` | `OPEN` | Reject |
| `OPEN` | `RESOLVED` | Reject (must go through `IN_PROGRESS`) |
| `OPEN` | `CLOSED` | Reject |
| `IN_PROGRESS` | `OPEN` | Reject |
| `IN_PROGRESS` | `CLOSED` | Reject (must go through `RESOLVED`) |
| `RESOLVED` | `IN_PROGRESS` | Reject |
| `RESOLVED` | `CANCELLED` | Reject |
| `CLOSED` | any | Reject |
| `CANCELLED` | any | Reject |
| any | same status | Reject (no-op is not a transition) |

Valid transitions succeed, persist the new status, and are visible after reload.

## Non-Functional Requirements

### Validation (backend)

- All mutating requests are validated **on the backend** even if the UI also validates.
- Title: required, non-blank, maximum length enforced (exact limit in `spec/data-model.md` / API contract).
- Description: required on create, non-blank.
- Priority: required on create; must be one of the allowed values (defined in `spec/data-model.md`; until then treat as a closed enum, not free text).
- Comment body: required, non-blank.
- Pagination: `page` ≥ 0; `size` within the API max; unknown `sort` → `400`.
- Status filter and transition target must be a known `TicketStatus` value; unknown → `400`.
- Illegal **state** (valid enum, disallowed edge) is **not** a validation `400`; it is FR-8 / `409`.

### Error handling

- Backend returns a consistent `ApiError` body (`code`, `message`, `details`, `correlationId`) for 4xx/5xx, per `.cursor/rules/api-standards.md`.
- **UI displays meaningful errors** from that body (user-facing `message` and field `details` where present): validation failures, not found, illegal transition, persistence/unavailable failures.
- Illegal transitions show that the **current status cannot move to the requested status**, not a generic “error.”
- Do not show stack traces, SQL, or secrets in the UI or in API responses.

### Persistence

- Tickets, comments, and status changes are **persisted in a database**.
- **Data survives application restart** (no in-memory-only store for runtime).
- Runtime database: PostgreSQL. Tests that hit the database use Testcontainers PostgreSQL (`.cursor/rules/testing.md`).
- Schema is evolved via migrations, not ad-hoc `ddl-auto=update` in non-local profiles.

### Other quality bars

- **No secrets are committed** (passwords, tokens, private keys, `.env` with real credentials). Config uses environment variable **names** only.
- State-machine **integration tests** must pass (allowed edges persist; illegal edges rejected and unchanged in the DB).
- Stack: Java 21 / Spring Boot backend; React/Next.js UI.

## Out of Scope

The following are **not** required for this version. Do not implement them unless a later spec change says so.

- Authentication, authorization, roles, or login UI (assignee is a ticket field, not a full user-management product).
- Email / push / in-app notifications.
- File attachments.
- SLA, due dates, or escalation.
- Reopen: `CLOSED`/`RESOLVED`/`CANCELLED` → `OPEN` (explicitly invalid).
- Editing or deleting comments; deleting tickets.
- Multi-tenancy, audit-export, reporting dashboards, real-time websockets.
- Full-text ranking, saved filters, bulk status updates.
- Using H2 as the production or default integration-test database.

## Core Acceptance Criteria

The solution is complete when all of the following hold:

1. Ticket can be created from the UI.
2. Tickets can be listed.
3. Ticket details can be viewed.
4. Ticket fields can be updated (title, description, priority).
5. Assignee can be changed.
6. Comments can be added.
7. Search works (keyword against title/description).
8. Status filter works.
9. Valid status transitions work.
10. Invalid status transitions are rejected by the backend.
11. Data survives application restart.
12. Backend validation works.
13. UI shows meaningful errors.
14. State-machine integration tests pass.
15. No secrets are committed.

## Open questions

Resolved in `spec/data-model.md` (2026-09-21): **Priority** is `LOW` | `MEDIUM` | `HIGH`; **assignee** is nullable free-text (`varchar(120)`), not a user FK; **comment author** is omitted.
