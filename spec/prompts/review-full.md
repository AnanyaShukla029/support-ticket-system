# Full review (code + spec + secrets)

Run after completing a milestone or before a PR. Combines the three review prompts.

## Prompt

```text
Run a full pre-merge review in three parts. Report only — do not fix anything unless I ask.

---

Part 1 — Code rules
Using .cursor/commands/review-code.md, review [scope] against .cursor/rules/java-springboot.md and .cursor/rules/api-standards.md.
List violations with file:line, severity (blocker | major | nit), and rule reference.

---

Part 2 — Spec drift
Using .cursor/commands/review-spec.md, compare backend/ and frontend/ to all files under spec/.
State Match | Mismatch | Incomplete. List every difference grouped by spec file.

---

Part 3 — Secrets
Scan the repo for hardcoded secrets, credentials, or JDBC URLs with embedded passwords.
List file:line for each finding.

---

End with a summary table: blockers / majors / nits / spec mismatches / secret findings.
No files were modified.
```

## Replace

| Placeholder | Typical value |
|---|---|
| `[scope]` | `backend/src/` and `frontend/` changed in this branch |
