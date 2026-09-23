# Ticket State Machine

Source: `spec/requirements.md` FR-8. Enum storage: `spec/data-model.md` (`TicketStatus`, VARCHAR). HTTP: unknown enum → `400`; illegal edge → `409` (`.cursor/rules/api-standards.md`).

The **backend service** is the only authority. The UI may hide illegal actions; tests and API clients must still be rejected if they send them.

## Status enum (`TicketStatus`)

| Value | Terminal? | How entered |
|---|---|---|
| `OPEN` | No | Default on **create** only (not a transition) |
| `IN_PROGRESS` | No | `OPEN` → `IN_PROGRESS` |
| `RESOLVED` | No | `IN_PROGRESS` → `RESOLVED` |
| `CLOSED` | **Yes** | `RESOLVED` → `CLOSED` |
| `CANCELLED` | **Yes** | `OPEN` → `CANCELLED` or `IN_PROGRESS` → `CANCELLED` |

Create is not a transition: there is no `null → OPEN` API. Initial status is always `OPEN` and cannot be chosen by the client on create.

## Who may transition

Authentication is out of scope. Any caller who can update the ticket may request a transition. No extra fields (comment, assignee) are **required** to change status. Assignee changes are FR-4 (field update), not a status edge.

## Side effects of a **valid** transition

- Persist `ticket.status` = target.
- Set `ticket.updatedAt` to now (`Clock`).
- Do not create a comment automatically.
- Do not change `title`, `description`, `priority`, or `assignee`.
- After restart, the new status is still present.

## Invalid request vs illegal transition

| Situation | HTTP | Persist? |
|---|---|---|
| Ticket id not found | `404` | No |
| Target not a `TicketStatus` value | `400` | No |
| Target is a valid enum but the edge is not allowed (including same status) | `409` | **No** — row unchanged |
| Allowed edge | `200` | Yes |

Error body is `ApiError`. Illegal-transition `message` must state that the **current** status cannot move to the **requested** status.

## Complete transition table

Legend: **Allow** = persist target status. **Reject** = `409`, no write. Rows = **from** (current). Columns = **to** (requested).

| From \ To | `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED` |
|---|---|---|---|---|---|
| `OPEN` | Reject (same status) | **Allow** | Reject (skip `IN_PROGRESS`) | Reject (skip) | **Allow** |
| `IN_PROGRESS` | Reject (backward) | Reject (same status) | **Allow** | Reject (skip `RESOLVED`) | **Allow** |
| `RESOLVED` | Reject (reopen) | Reject (backward) | Reject (same status) | **Allow** | Reject |
| `CLOSED` | Reject (reopen) | Reject | Reject | Reject (same status) | Reject |
| `CANCELLED` | Reject (reopen) | Reject | Reject | Reject | Reject (same status) |

### Every valid transition (5)

| From | To |
|---|---|
| `OPEN` | `IN_PROGRESS` |
| `OPEN` | `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED` |
| `IN_PROGRESS` | `CANCELLED` |
| `RESOLVED` | `CLOSED` |

Happy path: `OPEN` → `IN_PROGRESS` → `RESOLVED` → `CLOSED`.  
Cancel paths: `OPEN` → `CANCELLED`; `OPEN` → `IN_PROGRESS` → `CANCELLED`.

### Every invalid transition (20)

Same status (5):

| From | To | Why |
|---|---|---|
| `OPEN` | `OPEN` | No-op is not a transition |
| `IN_PROGRESS` | `IN_PROGRESS` | No-op is not a transition |
| `RESOLVED` | `RESOLVED` | No-op is not a transition |
| `CLOSED` | `CLOSED` | No-op is not a transition |
| `CANCELLED` | `CANCELLED` | No-op is not a transition |

Skip / backward / reopen / other (15):

| From | To | Why |
|---|---|---|
| `OPEN` | `RESOLVED` | Must pass through `IN_PROGRESS` |
| `OPEN` | `CLOSED` | Must pass through `IN_PROGRESS` then `RESOLVED` |
| `IN_PROGRESS` | `OPEN` | Backward |
| `IN_PROGRESS` | `CLOSED` | Must pass through `RESOLVED` |
| `RESOLVED` | `OPEN` | Reopen (out of scope / forbidden) |
| `RESOLVED` | `IN_PROGRESS` | Backward |
| `RESOLVED` | `CANCELLED` | Cancel only from `OPEN` or `IN_PROGRESS` |
| `CLOSED` | `OPEN` | Reopen |
| `CLOSED` | `IN_PROGRESS` | Terminal |
| `CLOSED` | `RESOLVED` | Terminal |
| `CLOSED` | `CANCELLED` | Terminal |
| `CANCELLED` | `OPEN` | Reopen |
| `CANCELLED` | `IN_PROGRESS` | Terminal |
| `CANCELLED` | `RESOLVED` | Terminal |
| `CANCELLED` | `CLOSED` | Terminal |

There are no other pairs: 5 × 5 = 25 = 5 allow + 20 reject.

## Tests

State-machine **integration tests** (Testcontainers PostgreSQL, `.cursor/rules/testing.md`) must cover:

- Each of the **5** allowed edges: status persisted, `updatedAt` changed, survives reload.
- Each of the **20** rejected edges: `409`, status and `updatedAt` unchanged (parameterized matrix).
- Unknown target string: `400`.
- Missing ticket: `404`.

Unit tests on the service (or a dedicated transition policy) may encode the same matrix without I/O; ITs prove persistence.

## Decisions (2026-09-21)

- Graph is closed: only the five Allow cells; including same-status as `409`.
- No comment required on transition.
- Terminal states have zero outbound edges.
