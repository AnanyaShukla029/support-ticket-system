# Review secrets

Scan the repo for credentials that must live in environment variables only.

## Prompt

```text
Scan the repository for hardcoded secrets, credentials, or connection strings that should be in environment variables instead.

Check:
- Source code (Java, TypeScript, config files)
- application.yml / application-*.yml / .properties
- frontend env files (.env, .env.local) — flag if committed
- docker-compose, scripts, README examples
- Test resources

Flag:
- passwords, API keys, tokens, private keys
- full JDBC URLs with embedded credentials (jdbc:postgresql://user:password@...)
- NEXT_PUBLIC_* values that contain secrets (public env is visible in the browser)

OK to list:
- env var NAMES in .env.example (no real values)
- localhost URLs without credentials
- placeholder values clearly marked (e.g. your-password-here)

List every instance found with file path and line. Severity: blocker if real secret, major if pattern encourages committing secrets.

Do not fix unless I ask. No files were modified.
```

## When to run

- Before every commit that touches config
- Task 10 (release checklist)
- After adding `application.yml` or `.env`
