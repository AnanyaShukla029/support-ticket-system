---
description: Generate JUnit 5 tests for a given class (happy path, edges, validation)
---

# Generate Tests

Generate **JUnit 5** tests for the class the user names (or the class in the active editor). Follow `.cursor/rules/testing.md` exactly. Read that file before writing tests. Also read the class under test and the relevant `spec/` sections (API, state machine, requirements) so tests do not invent behavior.

## Scope

- **One test class per class under test**, same package, `src/test/java`.
  - Unit: `TicketService` → `TicketServiceTest`
  - Web slice: `TicketController` → `TicketControllerTest` (`@WebMvcTest`)
  - Persistence: `TicketRepository` → `TicketRepositoryIT` (`@Tag("integration")`, Testcontainers PostgreSQL)
- Do not add production code, new endpoints, or extra helper types beyond test fixtures.
- Do not use JUnit 4, PowerMock, H2 as the default IT database, or a kitchen-sink test class.

## Required coverage for that class

Every generated class must include, as applicable to **that** type:

1. **Happy path** — typical success (create/get/update/transition as the class’s job).
2. **Edge cases** — empty lists, missing optional fields, not-found, forbidden, illegal state transition (`409` / domain exception), boundary sizes (`page`/`size`), null collections in mappers.
3. **Validation failures** — invalid `@RequestBody` / query params for controllers (`400` + `ApiError`); invalid arguments or invariant breaks for services (`assertThatThrownBy`).

Map method names to spec language: `shouldRejectTransitionWhenTicketIsClosed`, not `test1`.

## Mechanics (from testing.md)

**Unit (services, mappers, exception handler, domain):**

- `@ExtendWith(MockitoExtension.class)`
- `@MockitoSettings(strictness = Strictness.STRICT_STUBS)`
- `@Mock` collaborators, `@InjectMocks` class under test
- AssertJ; AAA structure; do not mock the class under test, records, or enums
- `@Nested` only for scenarios of this class (`Create`, `Transition`, …)
- `@ParameterizedTest` for state-machine matrices when the class owns transitions

**Controller slice:**

- `@WebMvcTest` + MockMvc; **mock the service**
- Assert HTTP status, JSON DTO or pagination envelope, and `ApiError` shape (`code`, `message`, `details`, `correlationId`)
- Validation failures: blank required fields, bad `page`/`sort` → `400`

**Repository IT:**

- Testcontainers PostgreSQL + real migrations; no mocked repository
- Isolation: transactional rollback or truncate between tests

Use fixtures (`TicketFixtures.newOpenTicket()`), not duplicated entity graphs. No secrets.

## Output

- Write the test file(s). If a test class already exists, **add** missing happy/edge/validation cases; do not delete existing tests unless they contradict spec.
- After writing, list what was covered in three bullets: Happy path / Edge cases / Validation failures.
- If the spec is TODO and the class is ambiguous, generate only what the source makes explicit and list **untested gaps** instead of inventing API fields.
