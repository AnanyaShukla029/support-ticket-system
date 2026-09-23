# Generate tests

Use after implementing a class. Invokes the same workflow as `.cursor/commands/generate-tests.md`.

## Prompt

```text
Using .cursor/commands/generate-tests.md and .cursor/rules/testing.md, generate JUnit 5 tests for [ClassName].

Read the class under test and these specs as needed:
- spec/state-machine.md (if the class handles status transitions)
- spec/api-contract.md (if controller or HTTP-facing)
- spec/data-model.md (if repository or persistence)
- spec/test-strategy.md (layer expectations)

Requirements:
- One test class per class under test, same package (e.g. TicketService → TicketServiceTest, TicketRepository → TicketRepositoryIT).
- Cover happy path, edge cases, and validation failures.
- If [ClassName] owns transitions: parameterized tests for all 25 (from, to) pairs from spec/state-machine.md (5 allow, 20 reject).
- Integration tests (*IT): @Tag("integration"), local PostgreSQL database support_tickets_test — not Testcontainers, not H2.
- Use TicketFixtures / CommentFixtures where applicable; inject Clock for timestamps.

Do not add production code or invent API fields not in spec/.

After writing tests, list coverage in three bullets: Happy path / Edge cases / Validation failures.
```

## Replace

| Placeholder | Example |
|---|---|
| `[ClassName]` | `TicketStatusMachine`, `TicketService`, `TicketController`, `TicketRepository` |

## Expected test class names

| Class | Test class | Layer |
|---|---|---|
| `TicketStatusMachine` | `TicketStatusMachineTest` | Unit |
| `TicketService` | `TicketServiceTest` | Unit |
| `TicketMapper` | `TicketMapperTest` | Unit |
| `GlobalExceptionHandler` | `GlobalExceptionHandlerTest` | Unit |
| `TicketController` | `TicketControllerTest` | `@WebMvcTest` |
| `TicketRepository` | `TicketRepositoryIT` | Integration |
