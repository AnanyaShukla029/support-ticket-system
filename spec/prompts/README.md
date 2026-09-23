# Prompts

Copy-paste templates for working through `spec/tasks.md`. Each prompt points at the matching **Cursor command** (`.cursor/commands/`) and **rules** (`.cursor/rules/`) so the agent follows the same workflow every time.

## When to use which prompt

| Prompt | Use when | Cursor command |
|---|---|---|
| [implement-task.md](implement-task.md) | Starting milestone **1–10** from `spec/tasks.md` | — |
| [generate-tests.md](generate-tests.md) | A production class exists and needs tests | `generate-tests` |
| [review-code.md](review-code.md) | Backend (or named paths) changed; check rules | `review-code` |
| [review-spec.md](review-spec.md) | Check code vs `spec/` before merge | `review-spec` |
| [review-secrets.md](review-secrets.md) | Before commit / PR; scan for leaked credentials | — |

## Full milestone review

After finishing a task, run in order:

1. [review-code.md](review-code.md)
2. [review-spec.md](review-spec.md)
3. [review-secrets.md](review-secrets.md)

Or paste [review-full.md](review-full.md) once to run all three.

## Placeholders

Replace bracketed values before sending:

| Placeholder | Example |
|---|---|
| `[N]` | Task number `3` |
| `[ClassName]` | `TicketService` |
| `[path]` | `backend/src/main/java/.../ticket` |

## Spec index

`spec/requirements.md` · `data-model.md` · `state-machine.md` · `api-contract.md` · `architecture.md` · `ui-flow.md` · `test-strategy.md` · `tasks.md`
