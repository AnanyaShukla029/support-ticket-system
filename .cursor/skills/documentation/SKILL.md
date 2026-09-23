---
name: documentation
description: Writes Javadoc, keeps README.md current, and updates spec/ when implementation diverges from the original spec. Use when adding public Java APIs, changing endpoints or domain behavior, updating setup instructions, or when code no longer matches spec/.
---

# Documentation

## When to apply

Use this skill in the same change as the code when any of these happen:

- Public Java types/methods are added or their contract changes
- Run/setup/stack instructions change → `README.md`
- Behavior, API, data, states, UI, or tests differ from what `spec/` currently says

Do not ship code that contradicts `spec/`. Either change the implementation to match the spec, or update the spec (and dependents) in the **same** change. Never “fix” a spec silently to match an accidental bug.

## Javadoc

Document the **contract**, not the implementation.

**Must have Javadoc**

- Public classes, interfaces, enums, records that are part of the API or domain (`*Controller`, `*Service`, `*Repository`, entities, public DTOs, exceptions, `GlobalExceptionHandler`)
- Public methods that are not obvious from the signature (`transition`, `assign`, query methods with non-trivial semantics)
- `@throws` for every domain exception a public method can raise

**Skip or keep one-line**

- Package-private/test code
- Record compact constructors and accessors when fields are self-explanatory
- Standard Spring overrides (`toString`/`equals` generated, trivial getters)

**Style**

- First sentence is a third-person summary ending with a period (Javadoc parser).
- `{@code TicketStatus}`, `{@link TicketService#assign}`, `@param`, `@return`, `@throws` — no `@author`, no version history in comments.
- Name types and statuses exactly as in `spec/` (`OPEN`, `ApiError`, `/api/v1/tickets`). Do not invent synonyms.
- Do not paste stack traces, secrets, or sample passwords. Do not restate the method name with no extra meaning.
- Implementation notes (`this uses JPQL because…`) belong in a short `//` next to the code, not in public Javadoc, unless callers must know.

```java
/**
 * Moves a ticket to {@code IN_PROGRESS} and records the assignee.
 *
 * @param ticketId ticket identifier
 * @param assigneeId agent who takes the ticket
 * @return updated ticket as an API DTO
 * @throws TicketNotFoundException if {@code ticketId} does not exist
 * @throws IllegalTicketTransitionException if the current status cannot move to {@code IN_PROGRESS}
 */
public TicketResponse startProgress(UUID ticketId, UUID assigneeId) { ... }
```

When a public signature changes, update Javadoc in that same edit (params, throws, status codes).

## README.md

`README.md` is how a developer runs the repo. It is **not** a second copy of `spec/`.

Keep current:

- What the project is (Support Ticket Management System)
- Stack: Java 21, Spring Boot, PostgreSQL (runtime) / Testcontainers in tests, React/Next.js
- Prerequisites (JDK 21, Node, Docker for integration tests)
- How to start backend and frontend, test commands (unit vs `@Tag("integration")`)
- Link to specs: `spec/requirements.md` as the product entry, plus architecture/API as needed
- Env vars by **name only** — never real secrets

Update README in the same change when modules, ports, profiles, or commands change. Do not document uncommitted or speculative features.

## Spec files — original spec vs implementation

**Default:** implement what `spec/` says. If the spec is incomplete, fill the spec first (or ask), then code.

**When implementation must differ** (framework limits, security, pagination defaults already in `.cursor/rules/api-standards.md`, naming collisions):

1. Change the relevant `spec/` file(s) **before or with** the code — not in a follow-up.
2. Patch every spec that repeats the old fact (see map below).
3. Note the deviation in a short “Decision” or “Changelog” bullet at the bottom of the spec file: what changed, why, date.
4. If the deviation was an AI error, also append `docs/ai-mistakes.md`.
5. Align Javadoc, tests, and README with the **new** spec, not the old one.

Do not leave TODOs in spec for behavior that is already implemented. Replace TODOs with the actual contract.

| If you change… | Update |
|---|---|
| User-facing capability, roles, acceptance criteria | `spec/requirements.md` |
| Layers, packages, profiles, integrations | `spec/architecture.md` |
| Entities, fields, constraints, IDs | `spec/data-model.md` |
| Paths, DTOs, status codes, `ApiError` codes, pagination | `spec/api-contract.md` (must match `.cursor/rules/api-standards.md`) |
| Ticket statuses or allowed transitions | `spec/state-machine.md` + tests |
| Screens, routes, journeys | `spec/ui-flow.md` |
| Test layers, tools, quality gates | `spec/test-strategy.md` (must match `.cursor/rules/testing.md`) |

**Consistency:** a new endpoint is not done until requirements (if user-visible), API contract, data model (if new fields), UI flow (if a screen), and test strategy all agree. Illegal transitions live in the state machine **and** the API error/`409` mapping.

## Prompt history and AI mistakes

After a significant spec or doc pass, append `docs/prompt-history.md` (`date`, prompt/intent, artifacts, outcome).

When the agent invented an API, skipped a spec, or the user had to revert docs, append `docs/ai-mistakes.md` (`what happened`, expected, root cause, prevention). Do not log secrets.

## Checklist (every behavior-changing PR)

- [ ] Public Java contracts have accurate Javadoc (`@param` / `@return` / `@throws`)
- [ ] `README.md` still starts the app the way the repo actually works
- [ ] `spec/` matches shipped behavior; no silent drift
- [ ] Cross-spec names (statuses, paths, DTO fields) are identical
- [ ] Tests and rules still describe the same stack (JUnit 5, Mockito, Testcontainers, `ApiError`)
