---
description: Compare current implementation to spec/ and list mismatches
---

# Review Spec

Compare the **current implementation** to the files under `spec/`. Goal: state whether the code still matches the spec, and list every mismatch. Do not treat Cursor rules as a substitute for spec files (rules constrain style; `spec/` constrains behavior).

## Do not rewrite

- **State mismatches.** Do not change spec files or implementation unless the user explicitly asks to reconcile them.
- Do not fill TODOs in spec as if they were decided by the code.

## Inputs

Read all of:

- `spec/requirements.md`
- `spec/architecture.md`
- `spec/data-model.md`
- `spec/api-contract.md`
- `spec/state-machine.md`
- `spec/ui-flow.md`
- `spec/test-strategy.md`

Then inspect the actual backend/frontend (controllers, services, entities, routes, tests). If `spec/` is still all TODOs and there is no matching code, say **spec incomplete / not implemented** — that is not a silent pass.

## What “matches” means

| Spec | Implementation must |
|---|---|
| `requirements.md` | Roles and acceptance criteria exist as behavior (or are explicitly unimplemented) |
| `architecture.md` | Layers, packages, profiles, DB choice agree with the running project |
| `data-model.md` | Entities, fields, constraints, ID strategy match JPA/schema |
| `api-contract.md` | Paths, methods, DTOs, status codes, error `code`s, pagination match controllers |
| `state-machine.md` | Status set and allowed/forbidden transitions match service logic |
| `ui-flow.md` | Routes/screens/journeys match the Next.js app |
| `test-strategy.md` | Test layers and tools in the repo match the strategy (and `.cursor/rules/testing.md`) |

A TODO in spec + working code **is a mismatch** (behavior exists but is not specified). A specified endpoint/field/transition with no code **is a mismatch** (spec ahead of implementation). Extra undocumented endpoints or statuses **are mismatches**.

## Output format

Start with one of:

- `Match: implementation agrees with spec/` (only if every applicable spec section is non-TODO and reflected in code)
- `Mismatch: N spec vs code difference(s)`
- `Incomplete: spec still skeletal; implementation is [absent | ahead of spec]`

Then one block per difference:

```markdown
### Mismatch — `spec/api-contract.md` vs `…/TicketController.java`
- **Spec says:** `PATCH /api/v1/tickets/{id}` returns `200` + `TicketResponse`
- **Code does:** `PUT /tickets/{id}` returning entity, `204`
- **Risk:** clients and tests will follow the wrong contract
```

Group by spec file. If a name differs (e.g. spec `OPEN`, code `Open`), call that out — do not consider it a match.

End with: `No files were modified.` and, if mismatches exist, whether the user should update spec (via the documentation skill) or change code to match spec — **ask**, do not pick and implement.
