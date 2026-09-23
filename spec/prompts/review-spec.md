# Review spec

Compare implementation to `spec/`. Report mismatches only — do not reconcile unless asked. Same workflow as `.cursor/commands/review-spec.md`.

## Prompt

```text
Using .cursor/commands/review-spec.md, check whether the current implementation still matches spec/.

Inspect:
- backend/ (Spring Boot)
- frontend/ (Next.js), if present

Read all spec files: requirements.md, architecture.md, data-model.md, api-contract.md, state-machine.md, ui-flow.md, test-strategy.md.

Start with one of: Match | Mismatch: N difference(s) | Incomplete.

For each mismatch, group by spec file:
- what the spec says
- what the code does
- risk if left unfixed

Include test-strategy alignment (local PostgreSQL for *IT, not Testcontainers).

Do not edit spec or code. End with: No files were modified. Ask whether to update spec or code if mismatches exist.
```

## When to run

- After completing a `spec/tasks.md` milestone
- Before opening a PR
- When unsure if UI and API still match the contract
