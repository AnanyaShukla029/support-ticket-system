# Review code

Report violations only — do not fix unless asked. Same workflow as `.cursor/commands/review-code.md`.

## Prompt

```text
Using .cursor/commands/review-code.md, review [scope] against .cursor/rules/java-springboot.md and .cursor/rules/api-standards.md.

Scope: [scope]
(e.g. git diff against main, or backend/src/, or files changed in the current branch)

List every violation with:
- severity: blocker | major | nit
- file and line reference
- which rule heading was broken
- what should change (suggestion only — do not apply fixes)

If you see behavior that contradicts spec/ but is not covered by the two rules, note it as a spec drift hint and point to review-spec — still do not fix.

End with: N blocker(s), M major, K nit(s). No files were modified.
```

## Replace

| Placeholder | Typical value |
|---|---|
| `[scope]` | `the entire backend/src directory` or `my current git diff` |

## Severity reminder

- **blocker:** entity leak, wrong HTTP status/ApiError, secrets, layering breaks
- **major:** missing Location on 201, list without pagination envelope, package layout
- **nit:** style only
