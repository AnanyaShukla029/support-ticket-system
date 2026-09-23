# AI Mistakes

## Enum Lookup NPE

**Issue:** `TicketStatus.valueOf(null)` was called when the status filter was set to "All", causing an NPE.

**Fix:** Skip enum parsing when no status is selected.

---

## LazyInitializationException

**Issue:** Ticket comments (lazy `@OneToMany`) were accessed
outside the transaction boundary that loaded the ticket, since the
generated service method wasn't transactional.

**Fix:** Made ticket detail loading `@Transactional`

---

## PostgreSQL Nullable Search Parameter

**Issue:** PostgreSQL could not infer the type of a nullable search parameter used with `LOWER()`, causing a `lower(bytea)` error.

**Fix:** Pass an empty string instead of `null` when no search keyword is provided.

---

## Missing `createdAt` on Comments

**Issue:** New comments were saved without `createdAt`, causing insert failures.

**Fix:** Set `createdAt` during comment creation and added `@PrePersist` as a fallback.

---
