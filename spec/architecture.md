# Architecture

Sources: `spec/requirements.md`, `spec/data-model.md`, `spec/state-machine.md`, `spec/api-contract.md`, `.cursor/rules/java-springboot.md`, `.cursor/rules/api-standards.md`. Screen-by-screen UX: `spec/ui-flow.md` (still to be filled; this document names the screens the UI must have).

## Context

Two runtimes plus one database. No auth provider, email, or file storage (out of scope).

```text
[Browser]
    |  HTTPS/HTTP, JSON
    v
[Next.js UI]  -------------------- CORS + REST -------------------->  [Spring Boot API]
    pages/screens, fetch client           /api/v1/...                      Java 21
                                                                                |
                                                                                v
                                                                          [PostgreSQL]
                                                                          ticket, comment
```

| Piece | Responsibility |
|---|---|
| **Browser** | Render UI; send user actions; show `ApiError.message` / `details` |
| **Next.js** | Pages, forms, list/detail state; **calls the Spring API**; no business rules that the API does not also enforce |
| **Spring Boot** | REST contract, validation, state machine, persistence |
| **PostgreSQL** | Source of truth; data survives process restart |

The UI may hide illegal status buttons; **invalid transitions must still be rejected in the service** (`spec/state-machine.md`).

## High-level design

- Communication: **REST JSON** only (`spec/api-contract.md`). No JWT/session in this version.
- Backend port (local): `8080`. Frontend: `3000`. Configurable; do not hardcode secrets.
- API base URL for the browser: `{API_ORIGIN}/api/v1` (e.g. `http://localhost:8080/api/v1`), injected as an env **name** such as `NEXT_PUBLIC_API_BASE_URL`.
- Next.js does **not** reimplement ticket rules in Route Handlers. Optional rewrite/proxy later is not required; **direct browser → Spring** with CORS is the default.

## Backend — Spring Boot layering

Java 21, Spring Boot, constructor injection, package-**by-feature**, then layer. See `.cursor/rules/java-springboot.md`.

### Package layout

```text
{base}.ticket/
  TicketController          @RestController  /api/v1/tickets
  TicketService             @Service, @Transactional on writes
  TicketRepository          Spring Data JPA
  CommentRepository
  Ticket.java               @Entity
  Comment.java              @Entity
  TicketStatus              enum
  Priority                  enum
  TicketStatusMachine       allowed-edge policy (no HTTP)
  TicketMapper              entity ↔ DTO
  dto/
    CreateTicketRequest
    UpdateTicketRequest
    ChangeTicketStatusRequest
    AddCommentRequest
    TicketSummaryResponse
    TicketResponse
    CommentResponse
    PageResponse
{base}.shared/
  exception/
    ApiError
    GlobalExceptionHandler  @RestControllerAdvice
    TicketNotFoundException
    IllegalTicketTransitionException
  config/                   CORS, Clock, Jackson
```

`{base}.user/` is **not** used (no User entity). Comments live with tickets because the only comment API is nested under `/tickets/{ticketId}`.

### Layer rules

| Layer | Does | Must not |
|---|---|---|
| **Controller** | Map HTTP ↔ DTOs; Bean Validation; `ResponseEntity` + `Location` on `201` | Touch entities or repositories; encode the transition graph; build ad-hoc error JSON |
| **DTO** | Records matching `spec/api-contract.md` | JPA annotations; leaking entities |
| **Service** | Create/list/get/update; **state machine**; load comments for detail; `@Transactional` writes | `HttpServlet*` / `ResponseEntity`; persist illegal status |
| **Repository** | Spring Data on `Ticket` / `Comment`; query methods for `q` + `status` + paging | DTOs; HTTP |
| **Entity** | Tables `ticket` / `comment` | JSON serialization on the API |

**TicketController** exposes exactly the contract:

| Method | Path |
|---|---|
| `POST` | `/api/v1/tickets` |
| `GET` | `/api/v1/tickets` |
| `GET` | `/api/v1/tickets/{ticketId}` |
| `PATCH` | `/api/v1/tickets/{ticketId}` |
| `POST` | `/api/v1/tickets/{ticketId}/status` |
| `POST` | `/api/v1/tickets/{ticketId}/comments` |

`PATCH` never applies `status`. `TicketService.changeStatus` delegates to `TicketStatusMachine` (the 5×5 table). Illegal edge → `IllegalTicketTransitionException` → `409` `ILLEGAL_TICKET_TRANSITION`. Unknown enum on the wire → `400` before the graph.

`Clock` is a bean so tests can freeze time (`createdAt` / `updatedAt`).

### Cross-cutting (backend)

- **Errors:** one `GlobalExceptionHandler`; body always `ApiError` (`code`, `message`, `details`, `correlationId`). Correlation id from a filter/MDC.
- **CORS:** allow the Next.js origin from config (not `*` in production). Methods and headers needed by the contract (`Content-Type`, `Location`).
- **Validation:** annotations on request records; unknown JSON properties fail (`400`).
- **Logging:** no secrets; `WARN` for notable 4xx, `ERROR` for 5xx.
- **Migrations:** Flyway or Liquibase; `ddl-auto` not for shared/test/prod.

## Database

**Runtime and integration tests: PostgreSQL.** Not H2 as the default IT or production store (`.cursor/rules/testing.md`).

| Concern | Choice |
|---|---|
| Engine | PostgreSQL |
| Schema | `ticket`, `comment` + indexes in `spec/data-model.md` |
| IDs | UUID |
| Enums | VARCHAR + Java enums, not PG `ENUM` types |
| Tests that hit the DB | Testcontainers PostgreSQL + same migrations |
| Local run | PostgreSQL via Docker or a declared local instance; JDBC URL from env **names** only |

H2 may exist only for throwaway local experiments, never as the documented persistence path.

## Frontend — React / Next.js

App Router (Next.js). TypeScript. No auth pages.

### Screens / routes

| Route | Screen | API |
|---|---|---|
| `/` or `/tickets` | **List** — rows, pagination, `q` search, `status` filter | `GET /api/v1/tickets` |
| `/tickets/new` | **Create** — title, description, priority, optional assignee | `POST /api/v1/tickets` then navigate to detail |
| `/tickets/[ticketId]` | **Detail** — fields, comments (oldest first), field edit, status actions, add comment | `GET` detail; `PATCH` fields; `POST .../status`; `POST .../comments` |

No login, admin, or user-management screens.

List does not need description/comments (`TicketSummaryResponse`). Detail uses `TicketResponse`. After a successful comment `201`, either append `CommentResponse` or re-fetch `GET` detail.

### How the UI calls the backend

1. A single **API client** module (e.g. `lib/api/tickets.ts`) wraps `fetch`.
2. Base URL = `process.env.NEXT_PUBLIC_API_BASE_URL` (no trailing slash) + `/api/v1`.
3. `Content-Type: application/json` on POST/PATCH. Parse JSON DTOs that match the contract (shared types, duplicated TS types, or generated later — do not invent fields).
4. **Success:** read body; on create/comment, may also read `Location`.
5. **Failure:** parse `ApiError`; throw/return a typed error. UI shows `message` and field `details` (toasts or inline). Never show stack traces or raw HTTP-only failures without that body when it exists.
6. List query string: `q`, `status`, `page`, `size`, `sort` as in the contract.
7. Status buttons: only edges allowed **for the current status** in `spec/state-machine.md`; if the user still hits an illegal call, show the `409` message.

Do not send `status` on `PATCH`. Do not call Spring from random `useEffect` URLs scattered in components — go through the client module.

### Frontend structure (indicative)

```text
app/
  tickets/page.tsx              list
  tickets/new/page.tsx          create
  tickets/[ticketId]/page.tsx   detail
components/
  TicketList, TicketForm, TicketDetail, CommentList, StatusActions, ErrorBanner
lib/api/                        fetch wrappers + TS types for DTOs
```

Client components where interactivity is required (search, filters, forms). Loading / empty / error states belong on each screen (`spec/ui-flow.md` will specify copy).

## Environments

| Profile / env | UI | API | DB |
|---|---|---|---|
| Local | `next dev` :3000 | Spring :8080 | PostgreSQL |
| Test (backend) | — | Spring Boot tests | Testcontainers PostgreSQL for `*IT`; unit tests have no DB |
| CI | lint/test UI if present | `./mvnw test` + integration group with Docker | container |

Config via environment variables (JDBC URL, user, password **names**; CORS origin; `NEXT_PUBLIC_API_BASE_URL`). **No secrets in git.**

## Deployment view

Local two-process is required. Production topology (single host, containers, reverse proxy) is **TBD** and not needed to satisfy acceptance criteria. If a reverse proxy is added later, it must not change `/api/v1` paths.

## Decisions (2026-09-21)

- Package-by-feature; comments under the ticket feature.
- Browser → Spring REST + CORS; Next.js is not a second backend.
- PostgreSQL only for runtime and DB integration tests.
- Dedicated `POST .../status` remains the only status write path.
