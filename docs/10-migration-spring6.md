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

**Commit:** `625b642`

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

**Commit:** `b3fc89b`

### What changed

**`pom.xml` (root) — maven-compiler-plugin:**
- Added `<parameters>true</parameters>` to the compiler plugin configuration.

### Why

Spring 6.1 removed `LocalVariableTableParameterNameDiscoverer`, which previously read
method parameter names from bytecode debug symbols (`-g` compiler flag). Spring 6 now
uses `StandardReflectionParameterNameDiscoverer`, which requires the `-parameters` javac
flag to preserve parameter names in bytecode.

Without this flag, every controller method that uses `@RequestParam` or `@PathVariable`
without an explicit `value` attribute fails at runtime:

```
java.lang.IllegalArgumentException: Name for argument of type [int] not specified,
and parameter name information not available via reflection. Ensure that the compiler
uses the '-parameters' flag.
```

Affected methods in this project (all four controllers have unannotated-value params):
- `ProfileController`: `offset`, `limit`, `q`, `id`
- `BookingController`: `travellerId`, `id`
- `FlightController`: `origin`, `destination`, `fromDate`, `toDate`, `lengthOfStay`, `passengerCount`, `currencyCode`
- `ReportController`: `from`, `to`, `format`

The fix is a single compiler flag rather than adding `value = "name"` to every annotation.

---

## Step 3 — Hibernate 6 query API

**Commit:** `79341be`

### Result: no code changes required

Every Hibernate 6 API removal that applied to this codebase was a **compile-time** error
that was already forced into the Step 0+1 commit. After a full review of all entity
mappings, DAOs, and service-layer interaction patterns, the code was already written in a
Hibernate 6-compatible style. Specific findings:

**Entity mappings — no issues:**
- `GenerationType.SEQUENCE` with an explicit `@SequenceGenerator(allocationSize = 1)` is
  unchanged in Hibernate 6. No reliance on the changed `AUTO` default.
- `@Enumerated(EnumType.STRING)` works identically.
- `@ManyToOne(fetch = FetchType.LAZY)` proxy mechanism unchanged.
- `LocalDate` / `LocalDateTime` — Hibernate 6 maps these natively; no custom `UserType`
  was in use that could have broken.

**HQL queries — no issues:**
- All queries use `session().createQuery(hql, Class<T>)` which returns
  `SelectionQuery<T>` in Hibernate 6. The `list()`, `setParameter()`,
  `setFirstResult()`, `setMaxResults()`, and `uniqueResult()` methods are all present.
- `FROM Booking b WHERE b.traveller.id = :travellerId` — Hibernate 6's SQM engine
  recognises that `.id` on a `@ManyToOne` is a FK column value and optimises it without
  issuing a JOIN (more efficient than Hibernate 5 in this case).
- `WHERE b.status = :status` with a `BookingStatus` enum — Hibernate 6 infers string
  binding from the `@Enumerated(EnumType.STRING)` mapping context. No explicit type hint
  needed.
- `SELECT COUNT(p) FROM TravellerProfile p WHERE p.email = :email` — returns `Long` as
  before; `uniqueResult()` on a COUNT always produces exactly one row.
- No legacy `org.hibernate.Criteria` API usage anywhere in the codebase.

**Service-layer patterns — no issues:**
- `ProfileServiceImpl.update()` and `BookingServiceImpl.cancel()` both rely on
  Hibernate dirty-checking rather than calling `dao.update()` explicitly. This works via
  `JpaTransactionManager`'s flush-on-commit the same way it did via `HibernateTransactionManager`.
- `BookingServiceImpl.reserve()` uses `TransactionTemplate` with `PROPAGATION_REQUIRES_NEW`.
  `JpaTransactionManager` suspends and resumes transactions correctly; the
  `@PersistenceContext EntityManager` proxy automatically binds to the active transaction
  in each phase.
- `session().persist(booking)` followed immediately by `booking.getId()` — `GenerationType.SEQUENCE`
  assigns the ID at `persist()` time (sequence is queried before the INSERT), not at flush
  time. Behaviour identical to Hibernate 5.

**`JdbcReportingDao` — completely unaffected:** pure JDBC, no Hibernate API.

---

## Step 4 — Tomcat 10

**Commit:** `<!-- hash -->`

### What changed

**`mini-midoffice-web/src/main/webapp/WEB-INF/web.xml`:**

```xml
<!-- Before (Servlet 4.0 / javaee) -->
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                             http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">

<!-- After (Servlet 6.0 / jakartaee) -->
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">
```

**`mini-midoffice-web/pom.xml`** (Cargo plugin — dev workflow):
- `<containerId>tomcat9x</containerId>` → `<containerId>tomcat10x</containerId>`
- Download URL updated to Apache Tomcat 10.1.28

**`HOW-TO-RUN.md`** (Docker run command):
- `tomcat:9.0-jdk17` → `tomcat:10.1-jdk17`

### Smoke test results (Tomcat 10.1-jdk17)

Deployed `mini-midoffice-web-1.0.0-SNAPSHOT.war` on `tomcat:10.1-jdk17`.
Startup time: **12,372 ms**.

```
POST /api/profiles → 201 Created
{"id":1,"firstName":"Ana","lastName":"Garcia","email":"ana@corp.com",
 "company":"Iberia","createdAt":"2026-06-21T13:45:25.863...","fullName":"Ana Garcia"}

GET /api/flights/search?origin=JFK&fromDate=2026-07-17&toDate=2026-07-20
    &passengerCount=1&currencyCode=USD → 200 OK (130 KB — full Sabre BFM result set)
```

All Jakarta EE namespaces, Spring 6 / Hibernate 6, and JPA transaction management
confirmed working end-to-end on Tomcat 10.1.

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

### G-9 · Spring 6.1 `-parameters` flag: silent compile, hard crash at runtime

**Symptom:** the project compiles cleanly and the WAR deploys, but every request to an
endpoint that has a `@RequestParam` or `@PathVariable` without an explicit `value` attribute
throws at runtime:

```
java.lang.IllegalArgumentException: Name for argument of type [int] not specified,
and parameter name information not available via reflection. Ensure that the compiler
uses the '-parameters' flag.
```

**Cause:** Spring 6.1 removed `LocalVariableTableParameterNameDiscoverer`, which previously
read parameter names from bytecode debug symbols (present when javac is invoked with `-g`).
Spring 6 now relies exclusively on `StandardReflectionParameterNameDiscoverer`, which requires
the `-parameters` javac flag to embed parameter names in the compiled `.class` file. Without
that flag, Spring cannot resolve the parameter name and throws at the first HTTP request.

**Fix:** add `<parameters>true</parameters>` to the `maven-compiler-plugin` configuration in
the root `pom.xml`. One line; no changes to any `@RequestParam` or `@PathVariable` annotation
needed.

### G-10 · Transaction manager changed: `HibernateTransactionManager` → `JpaTransactionManager`

**This is a real architectural change, not a rename.**

**Before (Spring 5 / Hibernate 5):**
```java
@Bean
public LocalSessionFactoryBean sessionFactory(DataSource dataSource) { ... }

@Bean
public HibernateTransactionManager transactionManager(SessionFactory sf) {
    return new HibernateTransactionManager(sf);
}
```
`HibernateTransactionManager` bound a Hibernate `Session` directly to Spring's
`TransactionSynchronizationManager`. DAOs called `sessionFactory.getCurrentSession()`,
which returned the Session registered by the transaction manager for the current thread.

**After (Spring 6 / Hibernate 6):**
```java
@Bean
public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) { ... }

@Bean
public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
}
```
`JpaTransactionManager` manages a JPA `EntityManager` (whose implementation is a Hibernate
`Session`). DAOs inject a Spring-managed `EntityManager` proxy via `@PersistenceContext`
and call `entityManager.unwrap(Session.class)` when they need the native Hibernate API.

**Why the change was forced:** Spring 6 removed `org.springframework.orm.hibernate5`
entirely (see G-6) and did not ship a `hibernate6` replacement. `JpaTransactionManager`
is the only Spring-provided transaction manager for Hibernate 6.

**Why application behaviour is preserved:**
- `@Transactional` AOP interception works identically — `JpaTransactionManager` opens an
  `EntityManager`, binds it to the thread, and commits/rolls back on method exit.
- Dirty checking still fires on commit: `JpaTransactionManager` calls `em.flush()` before
  commit, which triggers Hibernate's dirty-detection and issues UPDATE SQL for any modified
  managed entities. `ProfileServiceImpl.update()` and `BookingServiceImpl.cancel()` both
  rely on this and continue to work unchanged.
- `@Transactional(readOnly = true)` still sets flush mode to NEVER, preventing accidental
  writes in read-only service methods.
- `PROPAGATION_REQUIRES_NEW` via `TransactionTemplate` (used in `BookingServiceImpl.reserve()`)
  still works: `JpaTransactionManager` suspends the outer `EntityManager` binding, opens a
  new one for the inner transaction, then restores the outer binding.
- `@PersistenceContext EntityManager` is a Spring proxy: within any `@Transactional` scope
  it transparently delegates to the transaction-bound `EntityManager`, so `entityManager.unwrap(Session.class)`
  always returns the correct session for the current thread and transaction.

**Net result:** the persistence infrastructure changed (Session API → JPA + Session unwrap),
but every transactional guarantee the application relies on — dirty checking, read-only hint,
REQUIRES_NEW suspension — is provided by `JpaTransactionManager` with identical semantics.

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
