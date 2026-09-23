# Implementation Tasks

Ten sequential milestones derived from `spec/`. Complete in order. Each milestone should be reviewable as one PR (or a small stack). Ship tests from `spec/test-strategy.md` **with** the code in that milestone — do not defer all testing to the end.

**Specs:** `requirements.md` · `data-model.md` · `state-machine.md` · `api-contract.md` · `architecture.md` · `ui-flow.md` · `test-strategy.md`

**Database:** Use **PostgreSQL installed locally on Linux** — no Docker Postgres, no `docker-compose` for the database. Create two databases on your instance (names configurable via env):

- `support_tickets` — app runtime (`local` profile)
- `support_tickets_test` — integration tests (`test` / `integration` profile)

Credentials and host come from environment variables only (`.env` documents **names**, not real passwords).

---

## 1. Project scaffolding

- Spring Boot 3 / Java 21 backend (`web`, `data-jpa`, `validation`, PostgreSQL, Flyway); package layout per `architecture.md`
- Next.js (App Router) + TypeScript frontend (`frontend/` or agreed root)
- `.env` with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `NEXT_PUBLIC_API_BASE_URL` — no committed secrets
- README stub: prerequisites (JDK 21, Node, **local PostgreSQL**), how to create the two databases, ports 8080 / 3000, link to `spec/requirements.md`

**Done when:** `./mvnw compile`, `npm run dev`, and local Postgres accepts connections to `support_tickets`.

---

## 2. Backend cross-cutting

- Domain exceptions: `TicketNotFoundException`, `IllegalTicketTransitionException`
- `ApiError` + `GlobalExceptionHandler` (`400` / `404` / `409` / `500`) + `GlobalExceptionHandlerTest`
- Correlation ID filter → MDC → `ApiError.correlationId`
- `Clock` bean, CORS for configured UI origin, `application.yml` `local` profile → **local** `support_tickets` via env vars

**Done when:** App starts against local Postgres; handler unit tests match `api-contract.md` error shape.

---

## 3. Persistence layer

- Flyway V1: `ticket` + `comment` tables, indexes, FK cascade (`data-model.md`)
- `TicketStatus`, `Priority` enums; `Ticket` + `Comment` JPA entities
- `TicketRepository`, `CommentRepository` (CRUD, comment order, list/search/filter queries)
- Integration test profile (`application-test.yml` or `integration`) → **local** `support_tickets_test`; `@Tag("integration")` + **`TicketRepositoryIT`** (persist, `q` search, `status` filter, pagination, comment order, `updatedAt` unchanged on comment-only). Use `@Transactional` rollback or truncate between tests so the shared test DB stays clean.

**Done when:** `./mvnw verify -Dgroups=integration` passes against local `support_tickets_test`.

---

## 4. Domain, DTOs, and mapping

- `TicketStatusMachine` — 5 allow / 20 reject (`state-machine.md`) + parameterized **`TicketStatusMachineTest`**
- `TicketFixtures` / `CommentFixtures`
- All API records in `dto/` with Bean Validation (`api-contract.md`)
- `TicketMapper` + **`TicketMapperTest`**

**Done when:** Unit tests green; no HTTP or repository code in the state machine.

---

## 5. Ticket service

Single `TicketService` with unit tests (`TicketServiceTest`, mocked repos):

| Operation | Rules |
|---|---|
| Create / get | Default `OPEN`; trim title; blank assignee → `null`; `Clock` timestamps; `404` |
| Update fields | Partial `PATCH` title, description, priority, assignee; bump `updatedAt`; **no `status`** |
| List | `q`, `status`, `page`, `size`, `sort` → `PageResponse` |
| Change status | Delegates to machine; bump `updatedAt`; other fields unchanged |
| Add comment | Append; `404`; ticket `updatedAt` unchanged |

**Done when:** `TicketServiceTest` covers happy paths, not-found, and at least one illegal transition.

---

## 6. REST API

`TicketController` — all six endpoints from `api-contract.md` + **`TicketControllerTest`** (`@WebMvcTest`):

- `POST /api/v1/tickets` — `201` + `Location`
- `GET /api/v1/tickets` — pagination, `q`, `status`
- `GET /api/v1/tickets/{ticketId}`
- `PATCH /api/v1/tickets/{ticketId}` — reject `status` in body
- `POST /api/v1/tickets/{ticketId}/status` — `409` on illegal transition
- `POST /api/v1/tickets/{ticketId}/comments` — `201` + `Location`

**Done when:** `./mvnw test -Dgroups='!integration'` passes slice tests; manual `curl`/HTTPie smoke OK against local API.

---

## 7. Backend integration tests

- **`TicketStatusTransitionIT`**: full **25-edge** matrix via HTTP — 5 persist + `updatedAt` moves; 20 → `409` unchanged; unknown status `400`; missing ticket `404` (acceptance criterion **14**)
- **`TicketApiIT`**: create → get → patch → comment → list with `q` + `status`; reload proves persistence

Both run against **local** `support_tickets_test` (same Flyway migrations as `support_tickets`).

**Done when:** `./mvnw verify -Dgroups=integration` passes with local Postgres running.

---

## 8. Frontend foundation

- `lib/api/`: `fetch` wrapper, `NEXT_PUBLIC_API_BASE_URL`, TypeScript DTO types, typed `ApiError`
- Shared UI: `ErrorBanner` (message + field `details`), loading, empty states (`ui-flow.md`)

**Done when:** Client module compiles; can call a running backend from the browser (CORS).

---

## 9. Frontend screens

| Route | Behavior |
|---|---|
| `/` → `/tickets` | Redirect |
| `/tickets` | List, debounced `q`, status filter, pagination, URL params |
| `/tickets/new` | Create form → `201` → detail; `400` inline errors |
| `/tickets/[ticketId]` | Detail, Save (`PATCH`), comments, status buttons (`POST .../status`), `409` banner |

Component tests with mocked API: `TicketList.test.tsx`, `TicketForm.test.tsx`, detail/status tests.

**Done when:** All three journeys in `ui-flow.md` work against local API; `npm test` passes.

---

## 10. Release checklist

- Full test pass: unit + slice (`!integration`), integration against **local** `support_tickets_test`, frontend tests
- Walk `requirements.md` acceptance criteria **1–15** (including restart persistence smoke on `support_tickets`)
- Final README: create DBs, env vars, start backend + frontend, run test commands — **no secrets** in repo
- Note outcome in `docs/prompt-history.md` if useful

**Done when:** Fresh clone follows README end-to-end on a machine with local PostgreSQL; all acceptance criteria met.

---

## Dependencies

```text
1 → 2 → 3 → 4 → 5 → 6 → 7
              └──────────────→ 8 (after 6 for live API) → 9 → 10
```

Task **8** can start once **6** exposes endpoints; task **9** needs **7** + **8** for full-stack confidence.

## Out of scope

Auth/login, delete ticket, edit/delete comments, attachments, E2E Playwright, Docker Postgres / Testcontainers, H2 as IT database, reopen transitions.
