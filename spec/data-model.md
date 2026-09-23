# Data Model

Source: `spec/requirements.md`. Persistence: PostgreSQL (runtime), Testcontainers PostgreSQL (integration tests). JPA entities are persistence models only — never API payloads (`.cursor/rules/java-springboot.md`).

There is **no User entity** in this version (auth is out of scope).

## Enums

Java names: `TicketStatus`, `Priority`. Persist as **VARCHAR** (`@Enumerated(EnumType.STRING)`), never ordinal. Unknown wire values → `400`.

### `TicketStatus`

| Value | Meaning |
|---|---|
| `OPEN` | Default on create |
| `IN_PROGRESS` | Work started |
| `RESOLVED` | Work finished, awaiting close |
| `CLOSED` | Terminal |
| `CANCELLED` | Terminal |

Allowed transitions: `spec/requirements.md` FR-8 / `spec/state-machine.md`. The enum does **not** encode the graph; the service does.

### `Priority`

| Value |
|---|
| `LOW` |
| `MEDIUM` |
| `HIGH` |

Required on create. Not free text.

## Identifier strategy

- Ticket and comment primary keys: **UUID** (`uuid` in PostgreSQL, generated in the application or via `gen_random_uuid()`).
- No separate human ticket number (`TKT-001`) in this version.
- JSON field names: `id` (ticket), `id` (comment). Path param: `{ticketId}`.

## Ticket

Table: `ticket`. Entity: `Ticket`.

| Column | Java type | DB | Constraints |
|---|---|---|---|
| `id` | `UUID` | `uuid` | PK, not null |
| `title` | `String` | `varchar(200)` | not null, blank-rejected in app; trim before persist |
| `description` | `String` | `varchar(10000)` | not null, blank-rejected in app |
| `status` | `TicketStatus` | `varchar(32)` | not null; default **`OPEN`** on insert |
| `priority` | `Priority` | `varchar(16)` | not null |
| `assignee` | `String` | `varchar(120)` | **nullable**; free-text display name (no FK). Empty string on write is stored as **null** |
| `createdAt` | `Instant` | `timestamptz` | not null, set on insert, immutable |
| `updatedAt` | `Instant` | `timestamptz` | not null; set on insert; bump on any ticket field or status change (not required to bump when only a comment is added — see Comments) |

**Indexes**

- `ticket_status_idx` on `status` (filter FR-7)
- `ticket_updated_at_idx` on `updated_at` DESC (list sort default)

Keyword search (FR-6) is **case-insensitive substring** on `title` and `description` (`ILIKE` / `lower(column) LIKE`). No dedicated search document or `tsvector` in this version.

**Create defaults:** `status = OPEN`; `createdAt` / `updatedAt` = now (inject `Clock`).

**Update (FR-4):** title, description, priority, assignee only. Status changes are a separate transition, not a generic column patch from the client’s free-form status field.

## Comment

Table: `comment`. Entity: `Comment`. Append-only (no update/delete API).

| Column | Java type | DB | Constraints |
|---|---|---|---|
| `id` | `UUID` | `uuid` | PK, not null |
| `ticket_id` | `UUID` | `uuid` | not null, FK → `ticket.id` |
| `body` | `String` | `varchar(4000)` | not null, blank-rejected in app |
| `createdAt` | `Instant` | `timestamptz` | not null, set on insert, immutable |

No `author` column (auth out of scope). No `updatedAt`.

**Indexes**

- `comment_ticket_id_created_at_idx` on `(ticket_id, created_at)` — details view, oldest first (FR-3).

## Relationship

```
Ticket 1 ──< Comment
```

- A ticket has **zero or more** comments.
- A comment belongs to **exactly one** ticket.
- JPA: `Ticket.comments` = `@OneToMany(mappedBy = "ticket")`, **LAZY**, ordered by `createdAt ASC`.
- JPA: `Comment.ticket` = `@ManyToOne(optional = false, fetch = LAZY)`.
- **FK:** `ON DELETE CASCADE` so comments cannot orphan if a ticket row is removed at the DB. The HTTP API still does **not** delete tickets (requirements out of scope).
- Orphan removal: may be enabled on the collection; comments are only added through the comment API, not replaced as a detached graph from a ticket DTO.
- Adding a comment does **not** change ticket `status`. Whether `ticket.updatedAt` bumps on comment-only is **no** (comment has its own `createdAt`; list “last updated” is ticket-field/status time).

Do not cascade persist from a ticket DTO that embeds comments on create/update.

## What is not a table

| Omitted | Reason |
|---|---|
| User / Account | Auth out of scope; `assignee` is a string |
| Attachment | Out of scope |
| Audit / history table | Out of scope; current row + comments are enough |
| Ticket number sequence | UUID is the identifier |

## JPA / PostgreSQL notes

- Entities: no Jackson annotations. No `Optional` fields.
- Timestamps: `Instant` + `timestamptz`, UTC.
- Do not use H2-specific types. Avoid PostgreSQL `ENUM` types so Testcontainers + migrations stay portable (`varchar` + check optional).
- Optional CHECK: `status IN (...)` and `priority IN (...)` in Flyway/Liquibase; application validation remains required.

## Migrations

- Flyway or Liquibase is the schema source of truth (`.cursor/rules/java-springboot.md`).
- `ddl-auto` is not used outside local experiments.
- Initial migration: `ticket` and `comment` tables + indexes above.

## Decisions (2026-09-21)

- **Priority** values: `LOW`, `MEDIUM`, `HIGH` (closes requirements open question).
- **Assignee:** nullable `varchar(120)` free text, not a user FK.
- **Comment author:** omitted.
- **IDs:** UUID, no display ticket number.
- **Enums:** stored as strings.
