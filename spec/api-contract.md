# API Contract

Sources: `spec/data-model.md`, `spec/state-machine.md`, `.cursor/rules/api-standards.md`. Auth is out of scope — no login endpoints, no `401`/`403` in this version.

## Conventions

| Rule | Value |
|---|---|
| Base path | `/api/v1` |
| Content type | `application/json` (request and response) |
| Field names | camelCase |
| Timestamps | ISO-8601 UTC (`Instant`), e.g. `2026-09-21T10:15:30Z` |
| IDs | UUID strings |
| Enums | `TicketStatus`: `OPEN` \| `IN_PROGRESS` \| `RESOLVED` \| `CLOSED` \| `CANCELLED`. `Priority`: `LOW` \| `MEDIUM` \| `HIGH` |
| Trailing slash | Not used |
| `PUT` / `DELETE` | Not used |

Unknown JSON properties on request bodies → `400` `VALIDATION_FAILED`. Do not expose JPA entities.

**Status vs field update:** `PATCH /api/v1/tickets/{ticketId}` must not accept `status`. Status changes use **only** `POST /api/v1/tickets/{ticketId}/status`.

## Shared schemas

### `ApiError` (every 4xx/5xx)

```json
{
  "code": "TICKET_NOT_FOUND",
  "message": "Ticket 3fa85f64-5717-4562-b3fc-2c963f66afa6 was not found",
  "details": [
    { "field": "title", "message": "must not be blank" }
  ],
  "correlationId": "00-abc123"
}
```

`details` is always an array (empty if none). HTTP status is on the response line only.

### `CommentResponse`

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "body": "Investigating logs.",
  "createdAt": "2026-09-21T10:16:00Z"
}
```

### `TicketSummaryResponse` (list rows)

```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "title": "Cannot reset password",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "Alex",
  "createdAt": "2026-09-21T10:15:30Z",
  "updatedAt": "2026-09-21T10:15:30Z"
}
```

`assignee` is `null` when unset.

### `TicketResponse` (create, get, field update, status change)

`TicketSummaryResponse` plus:

```json
{
  "description": "Reset form returns 500.",
  "comments": [ ]
}
```

`comments` is always an array, oldest `createdAt` first. Empty on create.

### Pagination envelope (`PageResponse<TicketSummaryResponse>`)

```json
{
  "items": [ ],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

`items` is never `null`. Empty search/filter is `200` with `items: []`.

### Error `code` catalog

| `code` | HTTP | When |
|---|---|---|
| `VALIDATION_FAILED` | `400` | Bean Validation, blank/too-long fields, malformed JSON, unknown properties, empty-string rules |
| `INVALID_STATUS` | `400` | `status` query or body is not a `TicketStatus` value |
| `INVALID_PRIORITY` | `400` | `priority` is not a `Priority` value |
| `INVALID_SORT` | `400` | `sort` property/direction not allowed |
| `INVALID_PAGE` | `400` | `page` < 0 or `size` out of range |
| `TICKET_NOT_FOUND` | `404` | `{ticketId}` does not exist (including comment/status on missing ticket) |
| `ILLEGAL_TICKET_TRANSITION` | `409` | Valid enum pair but edge not allowed (`spec/state-machine.md`) |
| `UNSUPPORTED_MEDIA_TYPE` | `415` | Content-Type not JSON when a body is required |
| `INTERNAL_ERROR` | `500` | Unhandled; generic message, no internals |

Malformed `{ticketId}` (not a UUID) → `400` `VALIDATION_FAILED`.

---

## `POST /api/v1/tickets`

Create a ticket. Status is always `OPEN`; the client must not send `status`.

**Request** (`CreateTicketRequest`)

```json
{
  "title": "Cannot reset password",
  "description": "Reset form returns 500.",
  "priority": "HIGH",
  "assignee": "Alex"
}
```

| Field | Required | Rules |
|---|---|---|
| `title` | Yes | non-blank, max 200, trimmed |
| `description` | Yes | non-blank, max 10000 |
| `priority` | Yes | `LOW` \| `MEDIUM` \| `HIGH` |
| `assignee` | No | omit or `null` = unset; blank string stored as `null`; max 120 |

**Success:** `201 Created`  
Headers: `Location: /api/v1/tickets/{id}`  
Body: `TicketResponse` (`status` = `OPEN`, `comments` = `[]`).

**Errors:** `400` `VALIDATION_FAILED` / `INVALID_PRIORITY`; `415`; `500`.

---

## `GET /api/v1/tickets`

List tickets. Search and status filter are query params on this endpoint (not a separate search resource).

**Query**

| Param | Required | Default | Rules |
|---|---|---|---|
| `q` | No | (none) | Keyword; case-insensitive substring on **title** and **description**. Blank/`q` omitted = no keyword filter |
| `status` | No | (none) | One `TicketStatus`. Omitted = all statuses. Unknown value → `400` `INVALID_STATUS` (not an empty list) |
| `page` | No | `0` | ≥ 0 |
| `size` | No | `20` | 1–100 |
| `sort` | No | `updatedAt,desc` | Allowed properties: `updatedAt`, `createdAt`, `title`, `priority`, `status`. Direction `asc` or `desc` |

`q` and `status` may be combined (AND).

**Request body:** none.

**Success:** `200 OK` + pagination envelope of `TicketSummaryResponse`.

**Errors:** `400` `INVALID_STATUS` / `INVALID_PAGE` / `INVALID_SORT`; `500`.

---

## `GET /api/v1/tickets/{ticketId}`

Ticket details including comments.

**Request body:** none.

**Success:** `200 OK` + `TicketResponse`.

**Errors:** `400` invalid UUID; `404` `TICKET_NOT_FOUND`; `500`.

---

## `PATCH /api/v1/tickets/{ticketId}`

Partial update of **title, description, priority, assignee only**. Omitted fields stay unchanged. `assignee: null` clears assignee.

**Must not include `status`.** Sending `status` → `400` `VALIDATION_FAILED` (`field`: `status`, message: use `POST /api/v1/tickets/{ticketId}/status`).

**Request** (`UpdateTicketRequest`) — all properties optional; at least one recognized field should be present. Empty `{}` is `200` with no field changes.

```json
{
  "title": "Cannot reset password (prod)",
  "description": "Still 500 on /reset.",
  "priority": "MEDIUM",
  "assignee": null
}
```

| Field | If present |
|---|---|
| `title` | non-blank, max 200, trimmed |
| `description` | non-blank, max 10000 |
| `priority` | `LOW` \| `MEDIUM` \| `HIGH` |
| `assignee` | `null` clears; blank string → `null`; else max 120 |

Does **not** change status. Bumps `updatedAt`.

**Success:** `200 OK` + `TicketResponse` (comments included, unchanged).

**Errors:** `400` `VALIDATION_FAILED` / `INVALID_PRIORITY`; `404` `TICKET_NOT_FOUND`; `415`; `500`.

---

## `POST /api/v1/tickets/{ticketId}/status`

Dedicated status change. This is the **only** endpoint that mutates `status`. Graph: `spec/state-machine.md`.

**Request** (`ChangeTicketStatusRequest`)

```json
{
  "status": "IN_PROGRESS"
}
```

| Field | Required | Rules |
|---|---|---|
| `status` | Yes | Must be a `TicketStatus` value |

**Success (allowed edge):** `200 OK` + `TicketResponse`. Side effects: new `status`, `updatedAt` now; other fields and comments unchanged.

**Errors:**

| HTTP | `code` | When |
|---|---|---|
| `400` | `VALIDATION_FAILED` | Missing `status` |
| `400` | `INVALID_STATUS` | Not a `TicketStatus` literal |
| `400` | invalid UUID | Path id |
| `404` | `TICKET_NOT_FOUND` | No ticket |
| `409` | `ILLEGAL_TICKET_TRANSITION` | Any of the 20 rejected edges, including `{ "status": currentStatus }`. Message names current and requested status. **Row unchanged.** |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | |
| `500` | `INTERNAL_ERROR` | |

Allowed `status` values by current state (must succeed):

| Current | Allowed `status` in body |
|---|---|
| `OPEN` | `IN_PROGRESS`, `CANCELLED` |
| `IN_PROGRESS` | `RESOLVED`, `CANCELLED` |
| `RESOLVED` | `CLOSED` |
| `CLOSED` | *(none — all `409`)* |
| `CANCELLED` | *(none — all `409`)* |

---

## `POST /api/v1/tickets/{ticketId}/comments`

Add an append-only comment. Does not change ticket `status` or `updatedAt`.

**Request** (`AddCommentRequest`)

```json
{
  "body": "Investigating logs."
}
```

| Field | Required | Rules |
|---|---|---|
| `body` | Yes | non-blank, max 4000 |

**Success:** `201 Created`  
Headers: `Location: /api/v1/tickets/{ticketId}/comments/{commentId}`  
Body: `CommentResponse`.

**Errors:** `400` `VALIDATION_FAILED`; `404` `TICKET_NOT_FOUND`; `415`; `500`.

No `GET`/`PATCH`/`DELETE` on comments. Details listing is `GET .../tickets/{ticketId}`.

---

## Out of scope for this contract

- Auth, users, `401`/`403`
- `DELETE /tickets/{ticketId}`, comment edit/delete
- Attachments, bulk updates, reopen
- Separate search URL (use `GET /tickets?q=`)

## Decisions (2026-09-21)

- Status mutation is **only** `POST /api/v1/tickets/{ticketId}/status`, not `PATCH` on the ticket.
- List search param name is `q`; status filter param name is `status`.
- Get-by-id returns comments; list returns summaries without comments or description.
- Comment create returns `CommentResponse` `201`, not a full ticket.
