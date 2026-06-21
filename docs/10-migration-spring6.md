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

Steps 0 and 1 (POM version bumps + `javax.*` → `jakarta.*` sweep) are merged into a single
atomic commit because all three changes are tightly coupled: bumping Spring to 6.x removes
the `org.springframework.orm.hibernate5` package entirely, and bumping Hibernate to 6.x
removes the old `session.save/update/delete` API. Separating them would leave at least
one intermediate state that does not compile.

Steps 2, 3, and 4 remain as separate focused commits.

| Step | Description | Focus |
|------|-------------|-------|
| 0+1  | POM bumps + namespace sweep | Spring 5→6, Hibernate 5→6, `javax.*`→`jakarta.*`, Spring ORM `hibernate5`→`hibernate6`, Hibernate Session API removals |
| 2    | Spring 6 API adaptations | Any remaining Spring 6 breaking changes not covered by Step 0+1 |
| 3    | Hibernate 6 query API | HQL/Criteria API changes, if any remain after Step 0+1 |
| 4    | Tomcat 10 | `web.xml` Jakarta namespace, Docker image `tomcat:10.1-jdk17`, Cargo `tomcat10x` |

---

## Step 0+1 — POM bumps + `javax.*` → `jakarta.*` sweep (merged)

**Commit:** `<!-- hash -->`

### What changed

**POMs:**
- `spring.version`: 5.3.39 → 6.1.14
- `hibernate.version`: 5.6.15.Final → 6.4.4.Final
- `servlet.api.version`: 4.0.1 → 6.0.0
- `javax.persistence:javax.persistence-api:2.2` → `jakarta.persistence:jakarta.persistence-api:3.1.0`
- `javax.validation:validation-api:2.0.1.Final` → `jakarta.validation:jakarta.validation-api:3.0.2`
- `javax.servlet:javax.servlet-api` → `jakarta.servlet:jakarta.servlet-api` (groupId + artifactId)

**Java imports (namespace sweep):**
- `javax.persistence.*` → `jakarta.persistence.*` (domain model: `Booking`, `TravellerProfile`)
- `javax.validation.*` → `jakarta.validation.*` (domain model)
- `javax.servlet.*` → `jakarta.servlet.*` (`GlobalExceptionHandler`, `ReportController`, `RequestLoggingFilter`, `RequestIdFilter`)
- `javax.sql.DataSource` left as-is — it is a JDK class (`java.sql` module), not a Jakarta EE class

**Spring ORM package rename (required for compilation):**
- `org.springframework.orm.hibernate5.LocalSessionFactoryBean` → `hibernate6`
- `org.springframework.orm.hibernate5.HibernateTransactionManager` → `hibernate6`
- `hibernate.current_session_context_class` value → `org.springframework.orm.hibernate6.SpringSessionContext`

**Hibernate 6 Session API removals (required for compilation):**
- `session.save(entity)` → `session.persist(entity)` (both Hibernate DAOs)
- `session.update(entity)` → `session.merge(entity)` (both Hibernate DAOs)
- `session.delete(entity)` → `session.remove(entity)` (`HibernateTravellerProfileDao`)

**Hibernate 6 dialect rename:**
- `org.hibernate.dialect.PostgreSQL95Dialect` → `org.hibernate.dialect.PostgreSQLDialect`

---

## Step 2 — Spring 6 API adaptations

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Step 3 — Hibernate 6 query API

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Step 4 — Tomcat 10

**Commit:** `<!-- hash -->`

> *(To be filled.)*

---

## Gotchas encountered

### G-1 · Steps 0 and 1 cannot be separated

The original plan had POM bumps (Step 0) as one commit and the namespace sweep (Step 1) as
the next. This is unworkable: bumping Spring to 6.x eliminates the
`org.springframework.orm.hibernate5` package, so any intermediate state with the old import
paths does not compile. The two steps were merged into one atomic commit.

### G-2 · `hibernate5` → `hibernate6` is part of the namespace sweep, not Step 2

The Spring ORM package rename (`org.springframework.orm.hibernate5.*` →
`org.springframework.orm.hibernate6.*`) feels like a Spring 6 adaptation (Step 2), but it
causes compile errors the moment the POM version is bumped. It must go into the same commit
as the version bump.

### G-3 · `session.save/update/delete` removed in Hibernate 6

These three `Session` methods were deprecated in Hibernate 5.x and removed in 6.x. They
cause compile errors when the Hibernate version is bumped, so they were fixed in the
Step 0+1 commit alongside the POM bump:

| Old (Hibernate 5) | New (Hibernate 6 / JPA) |
|-------------------|--------------------------|
| `session.save(e)` | `session.persist(e)` |
| `session.update(e)` | `session.merge(e)` |
| `session.delete(e)` | `session.remove(e)` |

### G-4 · `PostgreSQL95Dialect` removed in Hibernate 6

`org.hibernate.dialect.PostgreSQL95Dialect` was removed. Replacement is
`org.hibernate.dialect.PostgreSQLDialect` (no version suffix — Hibernate 6 auto-detects
the PostgreSQL version at runtime). Fixed in the Step 0+1 commit.

### G-5 · `javax.sql.DataSource` stays as `javax.sql` — it is a JDK class

`javax.sql.DataSource` is part of the Java SE platform (`java.sql` module), not Jakarta EE.
It is NOT renamed to `jakarta.sql`. `PersistenceConfig.java` and `JdbcReportingDao.java`
retain `import javax.sql.DataSource` unchanged.

### G-6 · Spring 6 does NOT ship `org.springframework.orm.hibernate6`

There is no `hibernate6` package in `spring-orm` 6.x — it was never added. Spring 6 removed
the native Hibernate Session API integration (`LocalSessionFactoryBean`,
`HibernateTransactionManager`) and promotes using JPA exclusively. The correct Spring 6
approach is `LocalContainerEntityManagerFactoryBean` + `HibernateJpaVendorAdapter` +
`JpaTransactionManager`. Updated `PersistenceConfig.java` accordingly.

### G-7 · DAOs must use `@PersistenceContext EntityManager`, not `@Autowired SessionFactory`

After migrating to JPA-based Hibernate configuration, `SessionFactory.getCurrentSession()`
no longer works reliably within Spring `@Transactional` contexts (the `SpringSessionContext`
helper that made it work was in the now-gone `hibernate5` package). The clean fix is
`@PersistenceContext EntityManager entityManager` with `entityManager.unwrap(Session.class)`
inside the DAO's private `session()` helper. This is the canonical Spring 6 + Hibernate 6
pattern when you still want access to Hibernate's native `Session` API.

### G-8 · Hibernate 6 `groupId` changed to `org.hibernate.orm`

`org.hibernate:hibernate-core` was relocated. The correct Maven coordinate in Hibernate 6
is `org.hibernate.orm:hibernate-core`. Maven's relocation redirect still works but emits a
warning; the root `pom.xml` and `mini-midoffice-persistence/pom.xml` were updated to the
correct groupId.

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
