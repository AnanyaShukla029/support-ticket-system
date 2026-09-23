---
description: Review the diff against java-springboot.md and api-standards.md; report violations only
---

# Review Code

Read `.cursor/rules/java-springboot.md` and `.cursor/rules/api-standards.md` first. Then review **differences** (git diff vs the repo’s default base, or the files/range the user named). Do not review unrelated untouched files.

## Do not rewrite

- **Report violations only.** Do not edit production code, tests, or specs unless the user explicitly asks to fix them.
- Do not apply drive-by refactors, renames, or “while we’re here” cleanups.
- You may quote a short **suggested** snippet in the report; do not apply it.

## What to check

Against **java-springboot.md**:

- Java 21 usage (records, sealed types, `Optional` only as return, no `Date`/`Calendar`, no field `@Autowired`)
- Package-by-feature + controller → service → repository (controller must not touch entities/`Repository`; no business rules in controllers)
- DTO vs entity (no entity as `@RequestBody` or response)
- `@Transactional` on service write methods only
- Domain exceptions + single `@RestControllerAdvice` / `ApiError`

Against **api-standards.md**:

- `/api/v1`, plural resources, camelCase JSON, no `PUT` unless spec says so
- Create `201` + `Location`; missing resource `404` not empty `200`
- Error body is `ApiError` (`code`, `message`, `details`, `correlationId`) — no extra error shapes
- List endpoints use the pagination envelope (`items`, `page`, `size`, `totalItems`, `totalPages`)
- Status mapping: validation `400`, auth `401`/`403`, not found `404`, conflicts and illegal transitions `409` (not `422`)

If the diff also contradicts `spec/` (e.g. an undocumented path), note it as a spec mismatch but still **do not** rewrite. Point the user at `/review-spec` for a full spec audit.

## Output format

If there are no violations, say so in one sentence.

Otherwise a markdown list, one finding per item:

```markdown
### [blocker | major | nit] `path/to/File.java:12`
- **Rule:** java-springboot.md — DTO vs entity
- **Diff:** Controller returns `Ticket` entity from `GET /tickets/{id}`
- **Should be:** `TicketResponse` record; entity stays in the service
```

- **blocker:** must fix before merge (layering, entity leak, wrong status/`ApiError`, secrets)
- **major:** convention break that will spread (package layout, missing `Location`, list as a raw array)
- **nit:** style (a `var` on a public DTO, extra error field)

Cite the rule heading, not a paraphrase of “best practice.” End with a count: `N blocker(s), M major, K nit(s). No files were modified.`
