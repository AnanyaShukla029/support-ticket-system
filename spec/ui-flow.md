# UI Flow

Source: `spec/api-contract.md` (behavior), `spec/architecture.md` (routes and API client), `spec/state-machine.md` (status buttons). No login. All calls go through `lib/api` to `{NEXT_PUBLIC_API_BASE_URL}/api/v1`.

## Routes

| Route | Screen |
|---|---|
| `/` | Redirect to `/tickets` |
| `/tickets` | Ticket list (search + status filter) |
| `/tickets/new` | Ticket create form |
| `/tickets/[ticketId]` | Ticket detail / edit |

Invalid UUID in `[ticketId]`: treat as not found (do not call the API with a non-UUID, or show the `400` `message` if the request is made).

## Errors — how every screen behaves

The UI **must** show meaningful errors from `ApiError` (`message` plus `details[].field` / `details[].message`). Never stack traces, SQL, or `correlationId` as the primary copy (optional small print for support is allowed).

| HTTP / `code` | UI |
|---|---|
| `400` `VALIDATION_FAILED` / `INVALID_PRIORITY` | Inline under the named field when `details[].field` is set; otherwise a banner with `message`. Form stays put; submitted values kept. |
| `400` `INVALID_STATUS` / `INVALID_PAGE` / `INVALID_SORT` | Banner on the **list**; do not render an empty table as “no tickets.” |
| `404` `TICKET_NOT_FOUND` | Detail (or post-create navigation): full-page “Ticket not found” + link to `/tickets`. Not a blank success. |
| `409` `ILLEGAL_TICKET_TRANSITION` | Banner on detail: `message` (current vs requested status). Ticket fields and status display stay on the **server** values (reload or keep last successful `GET`/`PATCH`/`POST` body). |
| `500` `INTERNAL_ERROR` / network failure | Banner: `message` if JSON, else a generic “Could not reach the server.” Retry stays enabled. |
| `415` | Should not happen if the client sends JSON; if it does, banner with `message`. |

Optional **client-side** checks (blank title, max length) may run before submit to avoid a round trip. They do **not** replace backend validation. If both fire, show field errors; if the client missed something, still show the `400` `details`.

Disable the submit/status/comment button while that request is in flight. Do not send `status` on `PATCH`.

---

## Screen: Ticket list (`/tickets`)

**API:** `GET /api/v1/tickets?q&status&page&size&sort`

**Layout**

- Title “Tickets”
- Primary action: **New ticket** → `/tickets/new`
- **Search bar:** text input bound to query param `q` (keyword on title and description)
- **Status filter:** select — `All` (omit `status`) plus `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`
- Table/list of `TicketSummaryResponse`: title (link), status, priority, assignee (`—` if `null`), `updatedAt`
- Pagination: previous/next (or page numbers) using `page`, `size`, `totalPages` / `totalItems`
- Default sort `updatedAt,desc` (no sort UI required)

**Interactions**

1. On load: `GET` with current URL search params (`q`, `status`, `page`). Show a loading state.
2. Typing in search: debounce, then set `q`, reset `page` to `0`, refetch. Empty search clears `q`.
3. Changing status filter: set or omit `status`, reset `page` to `0`, refetch. Options are only valid enums so the UI should not send `INVALID_STATUS`.
4. Click a row/title → `/tickets/{id}`.
5. Pagination → update `page`, refetch. Do not request `page` < 0 or `size` outside 1–100.

**Empty vs error**

- `200` + `items: []`: empty state “No tickets match” (or “No tickets yet” when no `q`/status). Not an error.
- `400` on list: banner; keep last good list if any, or empty + error — **do not** label it as zero results.

Keep `q` and `status` in the browser URL so refresh/share preserves filters.

---

## Screen: Ticket create form (`/tickets/new`)

**API:** `POST /api/v1/tickets`  
Do **not** send `status` (server sets `OPEN`).

**Fields**

| Field | Control | Required |
|---|---|---|
| Title | text, max 200 | Yes |
| Description | textarea, max 10000 | Yes |
| Priority | select `LOW` / `MEDIUM` / `HIGH` | Yes (no free text) |
| Assignee | text, max 120 | No; blank → omit or `null` |

Actions: **Create ticket**, **Cancel** → `/tickets`.

**Interactions**

1. Submit → `POST` with `CreateTicketRequest`.
2. `201` + `TicketResponse` → navigate to `/tickets/{id}` (use body `id`).
3. Validation errors → stay on the form (see below).

**Validation errors (`400`)**

- Map `details` to fields: `title`, `description`, `priority`, `assignee`.
- Examples: blank title, over-length, missing priority, `INVALID_PRIORITY` if a bad value were sent.
- Banner with `message` if there are no field details.
- Do not navigate away; do not clear the form.

**Loading:** disable Create while in flight. Cancel remains available.

---

## Screen: Ticket detail / edit (`/tickets/[ticketId]`)

**APIs:**  
`GET /api/v1/tickets/{ticketId}`  
`PATCH /api/v1/tickets/{ticketId}` (title, description, priority, assignee only)  
`POST /api/v1/tickets/{ticketId}/status`  
`POST /api/v1/tickets/{ticketId}/comments`

**Layout (one page, two concerns)**

1. **Read + edit fields:** title, description, priority, assignee, plus **read-only** status, `createdAt`, `updatedAt`, `id`.
2. **Status actions:** buttons/select for **allowed next statuses only** (`spec/state-machine.md` / contract table). Terminal `CLOSED` / `CANCELLED`: no status buttons; short copy that the ticket cannot change status.
3. **Comments:** list oldest first (`createdAt`, `body`). Form: textarea `body` + **Add comment**. Empty comments: “No comments yet.”
4. Nav: **Back to list** → `/tickets` (preserve list query if stored).

**Load**

- Loading skeleton until `GET` succeeds.
- `404`: not-found page, not an editable form.
- `500`/network: banner + retry.

**Edit fields**

1. User changes title / description / priority / assignee and **Save**.
2. `PATCH` only dirty fields (or all four except never `status`). Clearing assignee sends `assignee: null`.
3. `200` `TicketResponse` → replace local ticket (including `updatedAt`); comments unchanged unless the response includes them.
4. `400`: inline field errors from `details`; keep the user’s edits visible.
5. `404` after delete-elsewhere: not-found.

**Change status**

1. User clicks e.g. “Start progress” (`IN_PROGRESS`) or “Cancel” (`CANCELLED`) when allowed.
2. Confirm optional. Then `POST .../status` `{ "status": "<target>" }`.
3. `200` → update status and `updatedAt`; refresh which buttons are shown.
4. `409` → banner with `message`; **do not** change displayed status; do not apply a fake local transition.
5. Do not offer illegal targets in the default UI. If they are triggered anyway, still handle `409`.

**Add comment**

1. Submit non-blank `body` (max 4000) → `POST .../comments`.
2. `201` `CommentResponse` → append to the list (keep oldest-first: append at end) and clear the textarea. Do not expect ticket `updatedAt` to change.
3. `400` → error on the comment field (`details` `body` or `message`). Comment list unchanged.
4. `404` → not-found.

**Field validation on Save (`400`)**

Same mapping as create: `title`, `description`, `priority`, `assignee`. Blank title after edit, over-length, bad priority.

**Comment validation (`400`)**

Blank or over-length `body` — stay on detail, comment not added.

---

## Journeys (acceptance)

| Journey | Screens | Success |
|---|---|---|
| Create from UI | List → New → Detail | Ticket visible with `OPEN` |
| List | List | Rows from `GET` |
| View details | List → Detail | Fields + comments |
| Update fields / assignee | Detail Save | `PATCH` then refreshed values |
| Search | List `q` | Matching rows or empty success |
| Status filter | List `status` | Only that status |
| Valid transition | Detail status action | New status from `200` |
| Invalid transition | Detail (if forced) | `409` message, status unchanged |
| Comment | Detail | New comment in list |
| Backend validation | Create or Detail | Inline/banner errors, no fake success |
| Restart | Any | Data still there after API restart (list/detail refetch) |

## Out of scope for UI

Login, roles, delete ticket, edit/delete comments, attachments, reopen, bulk actions, a separate search route.

## Decisions (2026-09-21)

- List filters live in the URL (`q`, `status`, `page`).
- Create success navigates to detail.
- Detail is the edit surface (no separate `/edit` route).
- Status controls are a distinct action from Save fields.
- `400` field errors stay on the form; `404` is a dedicated empty/not-found state.
