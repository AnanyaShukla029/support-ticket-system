# Test Strategy

Sources: `spec/requirements.md` (acceptance criteria), `spec/data-model.md` (entities and constraints), `spec/state-machine.md` (transition matrix), `spec/api-contract.md` (HTTP contract), `spec/ui-flow.md` (screens). Tooling and naming: `.cursor/rules/testing.md`.

## Objectives

“Done” means behavior is **specified and verified**, not merely a green build.

- Every acceptance criterion in `spec/requirements.md` maps to at least one automated test (backend unit/slice/IT or frontend component test).
- The **state machine** is covered by a parameterized unit matrix **and** persistence-backed integration tests (all 5 allow + 20 reject edges).
- **Data model** constraints (lengths, defaults, FK, `updatedAt` rules) are enforced in service/repository tests against **PostgreSQL** (Testcontainers), not H2.
- API errors use `ApiError` shape and documented `code` values.
- No secrets in test code or committed config.

## Test pyramid

```text
        [ UI component tests ]     few, mocked API
       [ Web slice / MockMvc ]      controllers, validation, status codes
      [ Integration (*IT) ]         DB + migrations + state machine persistence
     [ Unit tests ]                services, mappers, transition policy, handler
```

| Layer | Tools | Spring context | Docker |
|---|---|---|---|
| **Unit** | JUnit 5, Mockito, AssertJ | No | No |
| **Web slice** | `@WebMvcTest`, MockMvc | Slice | No |
| **Integration** | `@SpringBootTest`, Testcontainers PostgreSQL, Flyway/Liquibase | Full | Yes |
| **Frontend** | Vitest/Jest + React Testing Library | — | No |

E2E (browser against running UI + API) is **optional** for v1; acceptance criteria 1–13 can be met with IT + UI component tests. Add Playwright/Cypress later if desired.

**H2:** not the default for repository or `@SpringBootTest` tests. Runtime and IT fidelity target PostgreSQL (`spec/data-model.md`).

## Naming and layout

One test class per class under test, same package as production code (`src/test/java`):

| Production | Test |
|---|---|
| `TicketService` | `TicketServiceTest` |
| `TicketController` | `TicketControllerTest` |
| `TicketRepository` | `TicketRepositoryIT` |
| `TicketMapper` | `TicketMapperTest` |
| `TicketStatusMachine` (or policy in service) | `TicketStatusMachineTest` |
| `GlobalExceptionHandler` | `GlobalExceptionHandlerTest` |

- Integration classes suffix `IT`, tagged `@Tag("integration")`.
- Method names: `should<Behavior>When<Condition>` or `@DisplayName` quoting the spec.
- Fixtures: `TicketFixtures`, `CommentFixtures` — builders, not copy-pasted entity graphs.
- Inject `Clock` in tests; freeze time for `createdAt` / `updatedAt` assertions.

**Commands**

- Unit + slice (no Docker): `./mvnw test -Dgroups='!integration'`
- Integration: `./mvnw verify -Dgroups=integration` (Docker required)

---

## Unit test scope

Fast, no I/O. `@ExtendWith(MockitoExtension.class)`, `STRICT_STUBS`, mock repositories when testing services.

### State machine (`spec/state-machine.md`)

Target: `TicketStatusMachine` or `TicketService` transition logic with mocked `TicketRepository`.

- `@ParameterizedTest` over all **25** `(from, to)` pairs:
  - **5 allow:** no exception; verify target status would be applied (mock save or return value).
  - **20 reject:** `IllegalTicketTransitionException` (or equivalent); repository `save` never called.
- Create default `OPEN` is **not** a transition (no `null → OPEN` test in the matrix).

### `TicketService`

| Area | Cases |
|---|---|
| Create | `OPEN` default; trim title; blank assignee → `null`; `Clock` sets timestamps |
| Get | `TicketNotFoundException` when missing |
| Update fields | Partial patch; assignee clear (`null`); **reject `status` in patch** if passed through DTO layer |
| List/search/filter | Delegates to repository with `q`, `status`, page — assert arguments, not SQL |
| Comments | Append; `404` if ticket missing; **ticket `updatedAt` unchanged** on comment-only add |
| Transitions | Delegates to machine; maps `409` exception |

### `TicketMapper`

- Entity ↔ `TicketResponse` / `TicketSummaryResponse` / `CommentResponse`.
- `assignee` null ↔ unset; comments ordered oldest-first; empty comment list.

### `GlobalExceptionHandler`

- Each mapped exception → HTTP status + `ApiError` (`code`, `message`, `details`, `correlationId`).
- `MethodArgumentNotValidException` → `400` with field `details`.
- `IllegalTicketTransitionException` → `409` `ILLEGAL_TICKET_TRANSITION`.

### Out of unit scope

- SQL dialect, index usage, migration scripts, HTTP wire format (covered in IT/slice).

---

## Web slice scope (`@WebMvcTest`)

Mock `TicketService`. Assert routing, validation, and response status/body shape without DB.

| Endpoint | Must verify |
|---|---|
| `POST /api/v1/tickets` | `201`, `Location`, body; `400` blank title / bad priority |
| `GET /api/v1/tickets` | `200` pagination envelope; `400` `INVALID_STATUS` / `INVALID_PAGE` |
| `GET /api/v1/tickets/{id}` | `200`; `404`; `400` bad UUID |
| `PATCH /api/v1/tickets/{id}` | `200`; `400` including **`status` field rejected** |
| `POST .../status` | `200` mock; `409` when service throws |
| `POST .../comments` | `201`; `400` blank body |

Use JSON samples from `spec/api-contract.md`. Assert `ApiError` JSON on failures, not Spring default errors.

---

## Integration test scope (`*IT`, Testcontainers PostgreSQL)

Full Spring context, **same Flyway/Liquibase migrations as production**, real JDBC.

### Setup

- Shared PostgreSQL container per class or suite (`@Testcontainers`, `@DynamicPropertySource`).
- `@Transactional` rollback **or** truncate between tests — no order-dependent shared rows.
- `@Tag("integration")`.

### `TicketRepositoryIT` (`spec/data-model.md`)

| Concern | Tests |
|---|---|
| Persist ticket | All columns round-trip; `status` stored as VARCHAR string enum |
| Defaults | New row `status = OPEN`; `createdAt` / `updatedAt` set |
| Constraints | NOT NULL on title, description, priority; comment `body` NOT NULL |
| FK | Comment requires existing `ticket_id`; cascade on ticket delete (DB only) |
| Search `q` | Case-insensitive match on **title** and **description** |
| Filter `status` | Returns only matching status |
| Pagination / sort | `updatedAt` desc default; `page` / `size` |
| Comments | `findByTicketIdOrderByCreatedAtAsc`; multiple comments order |
| `updatedAt` | Bumps on field update and status change; **does not** bump on comment-only insert |

### State machine IT (`spec/state-machine.md`) — **required**

Class e.g. `TicketStatusTransitionIT` or methods on `TicketServiceIT` / `TicketApiIT` hitting `POST .../status` through MockMvc/WebTestClient.

For **each allowed edge (5)**:

1. Seed ticket in **from** status (via DB or create + transitions).
2. `POST /api/v1/tickets/{id}/status` `{ "status": "<to>" }`.
3. Assert `200`; reload from DB: `status == to`, `updatedAt` advanced (fixed `Clock` or before/after).
4. Other fields unchanged.

For **each rejected edge (20)** — parameterized:

1. Seed **from** status.
2. `POST` with **to**.
3. Assert `409`, `code` `ILLEGAL_TICKET_TRANSITION`, `ApiError.message` mentions current and requested status.
4. Reload: `status` still **from**, `updatedAt` unchanged.

Additional IT cases from state machine spec:

- Unknown `status` string → `400` `INVALID_STATUS`; row unchanged.
- Missing ticket → `404` `TICKET_NOT_FOUND`.

This satisfies acceptance criterion **14** (“state-machine integration tests pass”).

### API integration (thin)

Optional `TicketApiIT` for end-to-end HTTP + DB paths not fully covered above:

- Create → get detail includes empty `comments`.
- `PATCH` then `GET` reflects fields.
- Add comment → appears on `GET`, ticket `updatedAt` unchanged.
- List `q` + `status` combined.

### Persistence / restart (acceptance **11**)

- IT: insert ticket + comment; new repository read in same JVM proves durability.
- Manual or optional IT: stop/start container is heavy; **reload after write in a fresh transaction** is the automated proxy. Document manual restart check in README if needed.

---

## Frontend test scope (`spec/ui-flow.md`)

Mock `lib/api` client. React Testing Library — user-visible behavior, not implementation details.

| Screen | Cases |
|---|---|
| List | Renders rows; search debounce calls API with `q`; status filter; empty state vs error banner on `400` |
| Create | Submit success navigates; `400` shows field errors from `details` |
| Detail | Save `PATCH`; status buttons only when allowed; `409` banner, status unchanged; add comment |

No live backend in unit/component tests. Optional E2E later for full journey smoke.

---

## Mapping: specs → tests

| Spec | Test layer |
|---|---|
| `requirements.md` acceptance 1–10, 12–14 | UI components + service unit + IT + slice |
| `data-model.md` fields, lengths, FK, `updatedAt` | `TicketRepositoryIT`, service unit |
| `state-machine.md` 5 + 20 edges | `TicketStatusMachineTest` + **status IT matrix** |
| `api-contract.md` endpoints, `ApiError`, pagination | `TicketControllerTest` + `TicketApiIT` |
| `ui-flow.md` errors, filters, navigation | `*.test.tsx` |

Do not invent endpoints, fields, or transitions absent from spec.

---

## Fixtures and isolation

- `TicketFixtures.openTicket()`, `inProgressTicket()`, etc. set `TicketStatus` and minimal valid fields per `spec/data-model.md` limits.
- `CommentFixtures` with valid `body` ≤ 4000 chars.
- UUIDs: random per test or fixed constants — no collision across parallel methods if sharing DB (prefer rollback).
- No production credentials; container uses throwaway user/password.

---

## Quality gates (CI)

Minimum before merge:

1. `./mvnw test -Dgroups='!integration'` — pass (unit + slice).
2. `./mvnw verify -Dgroups=integration` — pass when Docker available (state machine IT included).
3. Frontend `npm test` / `pnpm test` — pass when UI changes exist.
4. No committed secrets (`.env`, keys) — lint/review.
5. Behavior change without new/updated tests — reject in review.

Coverage target: **not** a percentage goal for v1; **matrix and acceptance mapping** are the bar. Add JaCoCo later if desired.

---

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| UI hides illegal transition but API allows bug | IT matrix + never skip `409` cases |
| H2 vs PostgreSQL SQL differences | Testcontainers only for `*IT` |
| Flaky IT from shared data | Transaction rollback / truncate; no static mutable tickets |
| Time-dependent assertions | `Clock` bean fixed in tests |
| Comment `updatedAt` confusion | Explicit IT asserting ticket `updatedAt` unchanged after comment |

---

## Decisions (2026-09-21)

- State machine: unit matrix + **full 25-edge IT** on PostgreSQL.
- Integration tests use Testcontainers, not H2.
- `TicketRepositoryIT` owns data-model persistence; status IT owns transition persistence.
- E2E optional; component + backend IT satisfy v1 acceptance.
