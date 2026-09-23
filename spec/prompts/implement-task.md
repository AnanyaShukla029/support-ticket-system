# Implement task

Use when starting one milestone from `spec/tasks.md`. Implement **only** that task.

## Prompt

```text
Implement task [N] from spec/tasks.md.

Read the task section and its "Done when" criteria first. Use these as source of truth:
- spec/tasks.md (scope for this milestone only)
- spec/architecture.md (layers, packages, local PostgreSQL)
- Relevant spec files for this task (e.g. data-model.md, api-contract.md, state-machine.md, ui-flow.md, test-strategy.md)

Follow:
- .cursor/rules/java-springboot.md (backend)
- .cursor/rules/api-standards.md (REST)
- .cursor/rules/testing.md (tests ship with this task, not later)

When finished, summarize what changed, which files were touched, and how "Done when" is satisfied.
```

## Replace

| Placeholder | Value |
|---|---|
| `[N]` | `1` … `10` |

## Spec files by task (quick reference)

| Task | Primary specs |
|---|---|
| 1 | architecture.md, requirements.md |
| 2 | api-contract.md, architecture.md |
| 3 | data-model.md, test-strategy.md |
| 4 | state-machine.md, api-contract.md, data-model.md |
| 5 | requirements.md, state-machine.md, api-contract.md |
| 6 | api-contract.md |
| 7 | state-machine.md, test-strategy.md, api-contract.md |
| 8 | api-contract.md, ui-flow.md, architecture.md |
| 9 | ui-flow.md, api-contract.md, state-machine.md |
| 10 | requirements.md (acceptance criteria) |
