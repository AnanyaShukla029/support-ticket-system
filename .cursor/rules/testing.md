# Testing Conventions

Apply when adding or changing behavior (`**/*Test.java`, `**/*Tests.java`, `**/*.test.ts`, `**/*.test.tsx`). Map tests to `spec/` (requirements, API contract, state machine, UI flow, test strategy). Do not invent endpoints, fields, or transitions that are not in the spec.

## Stack

| Layer | Tools | Spring context? |
|---|---|---|
| **Unit** | JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`), AssertJ | No |
| **Slice / web** | `@WebMvcTest` + MockMvc, Mockito for the service | Slice only |
| **Integration** | `@SpringBootTest` + **Testcontainers PostgreSQL** + Flyway/Liquibase | Yes |

- Do not use H2 as the default for repository or `@SpringBootTest` tests. H2 is local-dev only unless a test cannot use Docker and that is documented in the test class.
- Do not use JUnit 4 (`@RunWith`, `@Rule`), PowerMock, or `MockitoAnnotations.initMocks`.

## One Test Class Per Class Under Test

- Mirror the production type: `TicketService` → `TicketServiceTest`, `TicketController` → `TicketControllerTest`, `TicketRepository` → `TicketRepositoryIT` (suffix `IT` for Testcontainers/Spring Boot tests).
- Same package as the class under test (Maven/Gradle `src/test/java`), not a parallel `...test.ticket` tree that hides package-private collaborators.
- Do not dump unrelated features into a `TicketTest` kitchen-sink class. Nested types (`@Nested`) are for scenarios of **that** class (e.g. `Create`, `Transition`).
- One assertion-focus per `@Test` method: name `should<Behavior>When<Condition>` or `methodName_condition_expectedResult`.

## Unit Tests (JUnit 5 + Mockito)

Target: services, mappers, pure domain (state transitions). Fast, deterministic, no I/O.

- `@ExtendWith(MockitoExtension.class)`. `@Mock` collaborators; `@InjectMocks` the class under test (constructor injection).
- `@MockitoSettings(strictness = Strictness.STRICT_STUBS)`. No unused stubbings; no `any()` when a real argument is known.
- Arrange–Act–Assert. Never assert on mocks of the class under test.
- Do not mock:
  - the class under test
  - records/DTOs, enums, value objects
  - `TicketRepository` **in** `TicketRepositoryIT` (test the real adapter)
- Do mock: repositories and other services when testing a service; the service when testing a controller slice.
- Use `assertThatThrownBy` (AssertJ) for domain exceptions (`TicketNotFoundException`, illegal transitions). Do not swallow exceptions.
- Clock/time: inject `Clock` (or a test `Instant` source). Do not call `Instant.now()` inside domain logic under test.
- `@ParameterizedTest` + `@EnumSource` / `@MethodSource` for state-machine matrices from `spec/state-machine.md`.

## Integration Tests (Testcontainers)

Target: repositories, persistence mapping, and a thin set of API happy/error paths that need a real DB.

- One shared PostgreSQL container per test class **or** a suite-level container (`@Testcontainers` + `@Container static`, or a Spring `DynamicPropertyRegistrar` / `DynamicPropertySource`).
- Point datasource URL/user/password at the container. Run the same migrations as production.
- `@Transactional` + rollback **or** explicit cleanup (`@Sql` / truncate) so tests do not leak rows. Prefer isolation over shared mutable seed data.
- `TicketRepositoryIT`: save/load, unique constraints, query methods, lazy vs eager as used by the service.
- `TicketControllerIT` / `TicketApiIT` (optional, few tests): MockMvc or WebTestClient against `@SpringBootTest` + Testcontainers; assert HTTP status and `ApiError` JSON, not Hibernate internals.
- Tag with `@Tag("integration")`. Keep unit tests runnable without Docker.

## What Each Layer Must Cover

| Class | Test class | Must cover |
|---|---|---|
| `*Service` | `*ServiceTest` | Happy path, not-found, forbidden, illegal transitions, mapping to DTOs |
| `*Controller` | `*ControllerTest` (`@WebMvcTest`) | Routing, validation 400s, status codes; service mocked |
| `*Repository` | `*RepositoryIT` | Queries and constraints against PostgreSQL |
| `GlobalExceptionHandler` | `GlobalExceptionHandlerTest` | Status + `ApiError` body for each mapped exception |
| `*Mapper` | `*MapperTest` | Entity ↔ DTO; null/empty collections |

Behavior changes require tests in the same change. Spec acceptance criteria should appear as test names or `@DisplayName`.

## Fixtures

- Test-data builders / factory methods (`TicketFixtures.newOpenTicket()`), not copied entity graphs in every test.
- No production secrets in tests. Use throwaway credentials only for the container.
- Do not depend on test order. No `static` mutable fixtures shared across methods unless reset in `@BeforeEach`.

## Frontend (React / Next.js)

- One test file per component/hook/page under test (`TicketList.test.tsx`).
- Test user-visible behavior (Testing Library), not internals. API clients mocked; e2e is separate and follows `spec/ui-flow.md`.

## Commands

- Unit (no Docker): `./mvnw test -Dgroups='!integration'` (or Gradle equivalent).
- Integration: `./mvnw verify -Dgroups=integration` with Docker available.
- Do not merge if unit tests fail or if changed persistence/API behavior has no `*IT` coverage.
