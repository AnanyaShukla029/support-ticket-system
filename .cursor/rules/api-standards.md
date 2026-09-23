# API Standards

Apply when changing REST controllers, API DTOs, OpenAPI, or frontend clients (`**/*Controller.java`, `**/dto/**/*.java`, `spec/api-contract.md`). `spec/api-contract.md` is the endpoint source of truth; this rule is the style those endpoints must follow. Do not expose JPA entities on the wire.

## REST Conventions

- Base path: `/api/v1`. Resource names are **plural nouns**, lowercase kebab-case path segments (`/api/v1/tickets`, `/api/v1/tickets/{ticketId}/comments`).
- Identify resources with path IDs (`{ticketId}`), not query params. No trailing slash; no file extensions.
- JSON only: `Content-Type` / `Accept` = `application/json`. Field names **camelCase**. Timestamps ISO-8601 UTC (`OffsetDateTime` / `Instant`).
- Nest only when the child cannot exist alone (comments under a ticket). Do not nest tickets under users (`GET /api/v1/tickets?assigneeId=` instead of `/users/{id}/tickets`).
- Prefer nouns over verbs in paths. State changes that are part of the resource use `PATCH` on the ticket. Dedicated action URLs (`POST .../assign`) only when the operation is not a simple field patch and is documented in `spec/api-contract.md`.

| Method | Use | Body | Idempotent |
|---|---|---|---|
| `GET` | Read one or list | None | Yes |
| `POST` | Create, or documented non-idempotent action | Request DTO | No (create returns 201) |
| `PATCH` | Partial update (status, assignee, title, …) | Request DTO; omitted fields unchanged | Yes, same body + current state |
| `PUT` | Do not use unless the spec defines full replace | — | — |
| `DELETE` | Remove a resource if the spec allows it | None | Yes |

- Create: `201 Created` + `Location: /api/v1/tickets/{id}` + `TicketResponse` body.
- Read: `200` + DTO or pagination envelope. Never return an empty `200` with `null` for a missing resource — use `404`.
- Update: `200` + updated DTO. Do not use `204` when the client needs the new representation.
- Delete: `204 No Content`.
- Request/response types are records in `dto/` (`CreateTicketRequest`, `UpdateTicketRequest`, `TicketResponse`). Validation annotations on request DTOs (`@NotBlank`, `@Size`, …).

## Error Response Shape

Every 4xx/5xx JSON body is the same `ApiError` record (from `GlobalExceptionHandler`). Never a Spring default HTML/JSON error, stack trace, SQL, or validation map with a different shape.

```json
{
  "code": "TICKET_NOT_FOUND",
  "message": "Ticket 3fa85f64-5717-4562-b3fc-2c963f66afa6 was not found",
  "details": [
    { "field": "title", "message": "must not be blank" }
  ],
  "correlationId": "00-abc123..."
}
```

| Field | Rules |
|---|---|
| `code` | Stable machine constant (`SCREAMING_SNAKE`). Not a translated sentence. |
| `message` | Safe, human-readable; no internals. |
| `details` | Always an array (empty if none). Use `{ "field", "message" }` for validation; omit `field` for non-field errors. |
| `correlationId` | Request id from MDC / header; same value in logs. |

Do not add ad-hoc keys (`error`, `timestamp`, `path`, `status`) on this object. HTTP **status** lives in the response line, not the body.

## Pagination (list endpoints)

All collection `GET`s use **offset pagination** with the same query params and the same envelope. No raw JSON arrays for lists that can grow.

**Query** (0-based page):

- `page` — default `0`, min `0`
- `size` — default `20`, max `100`
- `sort` — optional `property,dir` (`createdAt,desc`). Unknown properties → `400` `INVALID_SORT`

**Response envelope:**

```json
{
  "items": [ ],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

- `items` is always an array (never `null`). Empty list is `200` with `items: []` and `totalItems: 0`.
- Do not wrap a single resource in this envelope.
- Filters stay as additional query params (`status`, `assigneeId`, `q`) documented per endpoint. Do not put filters in the body of `GET`.

## Status Codes

| Status | When |
|---|---|
| `200` | Successful GET/PATCH (and POST only if the spec says it is not a create) |
| `201` | Resource created |
| `204` | Successful DELETE |
| `400` | Malformed JSON, constraint violations, bad query params (`page`, `size`, `sort`) |
| `401` | Missing or invalid authentication |
| `403` | Authenticated but not allowed (wrong role or not owner/assignee as spec defines) |
| `404` | Resource id does not exist **or** must be hidden as not found (do not leak existence with `403` unless the spec says so) |
| `409` | Duplicate create, optimistic-lock / stale version, **illegal ticket state transition** |
| `415` | Unsupported `Content-Type` |
| `500` | Unhandled; generic `INTERNAL_ERROR` message; log the stack trace server-side |

Do **not** use `422` in this project — semantic/business conflicts are `409`; input shape/validation is `400`.

Do **not** use `200` with an error payload, `302` for API auth failure, or `404` for “empty list”.

## Contract Discipline

- New or changed endpoints, fields, error `code`s, or status mappings land in `spec/api-contract.md` in the same change as the controller.
- Additive fields may be optional; renaming/removing fields or changing status/`code` meaning is breaking and needs a `/api/v2` (or an explicit spec decision).
- Auth: unauthenticated API calls get `401` + `ApiError`, never a login HTML redirect. Do not log tokens or passwords.
