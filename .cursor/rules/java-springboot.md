# Java & Spring Boot Conventions

Apply when editing backend Java/Spring code (`**/*.java`, `**/pom.xml`, `**/application*.yml`). Follow `spec/` first; do not invent APIs or entities.

## Java 21 — Prefer

- **Records** for request/response DTOs, value objects, and mapper outputs. Keep them immutable.
- **Sealed interfaces/classes** for closed domain sets (ticket status events, error types). Pair with exhaustive `switch`.
- **Pattern matching** (`instanceof`, `switch`) instead of casts and visitor boilerplate.
- **Text blocks** for multi-line SQL fragments, JSON fixtures, and error messages in tests.
- **`Optional<T>`** only as a method return for “maybe absent”. Never as a field, DTO property, or collection element.
- **Sequenced collections** (`List`/`SequencedMap` order) when API order matters; do not rely on `HashMap` iteration order.
- **Virtual threads**: enable via Spring’s virtual-thread executor for blocking I/O only; do not mix with `synchronized` on hot paths or assume `ThreadLocal` safety.
- Use `final` locals where it clarifies; prefer explicit types at public API boundaries. Do not use `var` for DTOs, entities, or anything a reader cannot infer in one glance.
- Do not use: raw types, `Date`/`Calendar` (use `Instant`/`OffsetDateTime`), field injection, `null` as a sentinel when `Optional` or a sealed type is clearer.

## Package-by-Feature + Layers

Organize by **feature first**, then layer. Do not use a single global `controller` / `service` / `repository` package for the whole app.

Within each feature package, use **layer sub-packages** (not a flat package of mixed types):

```
{base}.ticket/
  controller/              # @RestController classes (e.g. TicketController)
  service/                 # @Service, domain helpers (e.g. TicketService, TicketStatusMachine)
  repository/              # Spring Data interfaces (e.g. TicketRepository)
  entity/                  # @Entity JPA models (e.g. Ticket, Comment)
  enums/                   # feature enums (e.g. TicketStatus, Priority)
  dto/                     # API request/response records only
  mapper/                  # entity <-> DTO mapping (e.g. TicketMapper)
{base}.shared/
  exception/               # @ControllerAdvice, ApiError, base exceptions
  config/
```

Placement rules (strict):

| Sub-package | Put here | Examples |
|---|---|---|
| `entity` | `@Entity` persistence models | `Ticket`, `Comment` |
| `enums` | feature-specific enums | `TicketStatus`, `Priority` |
| `repository` | `JpaRepository` and custom query interfaces | `TicketRepository` |
| `service` | business logic, state machines, orchestration | `TicketService`, `TicketStatusMachine` |
| `mapper` | entity ↔ DTO mapping | `TicketMapper` |
| `dto` | API contracts (records) | `CreateTicketRequest`, `TicketResponse` |
| `controller` | HTTP boundary only | `TicketController` |

Do not place entities, repositories, or services directly in the feature root package.

Layer rules (strict):

| Layer | May depend on | Must not |
|---|---|---|
| **Controller** | Service, DTOs, Bean Validation | Entities, `Repository`, transactions, business rules |
| **Service** | Repository, entities, mappers, other services | HTTP types (`HttpServlet*`, `ResponseEntity` except at controller), JSON annotations as domain |
| **Repository** | Entity, Spring Data | DTOs, other repositories’ internals, HTTP |

- One public `*Service` per feature; keep `@Transactional` on the service (write methods), not on controllers or repositories.
- Constructor injection only (`@RequiredArgsConstructor` or an explicit constructor). No `@Autowired` on fields.
- Controllers are `@RestController`. Map HTTP in the controller; keep methods thin (validate → call service → return DTO).

## DTO vs Entity

- **Entities** (`@Entity`) are persistence models: JPA annotations, lazy relations, no JSON serialization. Never return an entity from a controller or accept one as `@RequestBody`.
- **DTOs** are API contracts: Java **records** in `dto/`. Names: `CreateTicketRequest`, `UpdateTicketRequest`, `TicketResponse`. Match `spec/api-contract.md`.
- Do not put JPA annotations on DTOs or Jackson annotations on entities.
- Mapping lives in a dedicated `*Mapper` (or package-private methods on the service). No `BeanUtils.copyProperties`.
- Collections of entities stay inside the service; controllers see `List<TicketResponse>` (or a pagination envelope), never `Page<Ticket>`.

## Exception Handling (`@ControllerAdvice`)

- Throw **domain exceptions** from services (`TicketNotFoundException`, `IllegalTicketTransitionException`, `ForbiddenOperationException`). Do not build `ResponseEntity` error bodies in services.
- Handle them in one `shared.exception.GlobalExceptionHandler` annotated with `@RestControllerAdvice` (`@ControllerAdvice` + JSON).
- Map consistently:

  | Exception | Status |
  |---|---|
  | Validation / `MethodArgumentNotValidException` | 400 |
  | Unauthenticated | 401 |
  | Forbidden | 403 |
  | Not found | 404 |
  | Conflict (duplicate, stale version) | 409 |
  | Illegal state transition | 409 or 422 (pick one in `spec/api-contract.md` and stick to it) |
  | Unhandled | 500; log stack trace; generic client message |

- Error JSON is a single `ApiError` record (e.g. `code`, `message`, `details`, `correlationId`). No stack traces or SQL in responses.
- Do not add per-controller `@ExceptionHandler` unless the case is unique to that feature; prefer the global handler.
- Log at `WARN` for 4xx that indicate client mistakes we care about, `ERROR` for 5xx. Never log passwords, tokens, or secrets.

## Spring Boot Defaults

- Bind config with `@ConfigurationProperties`; no scattered `@Value` for feature settings.
- Profiles: `local` (H2 optional), `test` (H2), default/prod (PostgreSQL). Do not hardcode JDBC URLs.
- Keep Flyway/Liquibase migrations as the schema source of truth; do not use `ddl-auto=update` outside local experiments.
