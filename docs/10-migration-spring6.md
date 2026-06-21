# JCarranza Mini-Midoffice — Spring 6 / Hibernate 6 Migration

> This document records a real, incremental migration of a multi-module Maven WAR application
> from Spring Framework 5.3 / Hibernate 5.6 / Tomcat 9 / `javax.*` to
> Spring Framework 6.x / Hibernate 6.x / Tomcat 10 / `jakarta.*`.
>
> Each step below corresponds to a single focused commit on the branch
> `feature/spring6-hibernate6`. The commit hashes are recorded here so the document
> and the Git history cross-reference each other. The baseline is tagged
> `v1.0-spring5-hibernate5`; the completed migration will be tagged
> `v2.0-spring6-hibernate6`.

---

## Migration order & rationale

> *(To be filled after the order is agreed and the first commits land.)*

---

## Step 1 — Jakarta EE namespace (`javax.*` → `jakarta.*`)

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Step 2 — Spring 6

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Step 3 — Hibernate 6

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Step 4 — Tomcat 10

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Gotchas encountered

> *(Populated as issues are hit during the migration — compile errors, runtime surprises,
> API removals, behaviour changes.)*

---

## Rollback & schema-versioning strategy

This migration is **code-only** — it does not alter the database schema in any way.
Rollback is therefore a simple:

```bash
git checkout v1.0-spring5-hibernate5
```

and a WAR rebuild from that tag. No database intervention required.

### If the migration had included schema changes

In a real production scenario where the migration also touched the schema, the rollback
strategy would be:

- **Liquibase** (preferred): versioned changesets with an explicit rollback block per
  changeset. A rollback to a given state is `liquibase rollbackToDate` or
  `liquibase rollback <tag>`. Each changeset would be applied and rolled back atomically.
- **Manual DDL** (this project's approach for demo simplicity): the initial schema lives in
  `mini-midoffice-persistence/src/main/resources/db/V1__create_schema.sql` and the one
  migration in `V2__remove_searched_status.sql`. These are applied manually and have no
  automated rollback mechanism — acceptable for a single-developer demo, not for production.
