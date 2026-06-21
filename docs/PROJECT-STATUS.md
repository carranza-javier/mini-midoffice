# JCarranza Mini-Midoffice — Project Status

> Read this first in any new session. Then read `docs/00-project-brief.md` through
> `docs/09-support-simulation.md` for full design detail on each phase.

---

## 1. Current State

All 9 development phases are complete on `main`. A Spring 6 / Hibernate 6 migration is
**in progress** on branch `feature/spring6-hibernate6` (Step 0+1 committed; Steps 2–4 pending).

**Completed (on `main`):**
- Phases 1–9 implemented (domain model, Sabre GDS integration, persistence, services, REST API,
  Bootstrap/jQuery/Handlebars SPA frontend, CSV/Excel reporting, Logback observability with MDC,
  support simulation documentation)
- All source files in English; naming consistent: `JCarranza Mini-Midoffice` everywhere
- Reports screen: on-screen tables, Apply/Clear date filter, CSV/Excel downloads (see `docs/07-reporting.md`)
- Git repository initialized, `.gitignore` configured, pushed to GitHub:
  **https://github.com/carranza-javier/mini-midoffice** (public, branch `main`)

**In progress (on `feature/spring6-hibernate6`):**
- Step 0+1 (merged commit `625b642`): Spring 5→6, Hibernate 5→6, `javax.*`→`jakarta.*` sweep — **DONE, build passes**
- Step 2 (commit `b3fc89b`): Spring 6 API adaptations (`-parameters` flag) — **DONE**
- Step 3 (commit `79341be`): Hibernate 6 query API — **DONE (no code changes required)**
- Step 4: Tomcat 10 (Docker image, `web.xml` namespace, Cargo plugin) — **DONE, smoke test passed**

See `docs/10-migration-spring6.md` for the full migration log (step details + gotchas G-1 through G-10).

---

## 2. What This Project Is

A portfolio application modelling a travel-agency mid-office system: booking management,
GDS integrations (Sabre sandbox), and ad-hoc SQL reporting. Built to demonstrate senior
Java backend skills on a realistic, production-representative stack.

**Stack (`main`):** Java 17, Spring Framework 5.3 (no Spring Boot — explicit Root/Web context
separation), Hibernate 5.6, PostgreSQL 16 (HikariCP pool), Maven multi-module WAR deployed on
Tomcat 9, jQuery + Handlebars + Bootstrap 3 SPA frontend, Apache POI for Excel export, Sabre
GDS REST sandbox integration (OAuth2, BFM flight search + flight check + booking reserve).

**Stack (`feature/spring6-hibernate6`, Step 0+1 done):** Spring 6.1.14, Hibernate 6.4.4.Final,
Jakarta EE 10 (`jakarta.*`), JPA-based persistence config (`LocalContainerEntityManagerFactoryBean`
+ `JpaTransactionManager`). Tomcat still on 9 until Step 4.

---

## 3. Architecture

### Maven Modules (root: `com.jcarranza:mini-midoffice`)

| Module | Role |
|--------|------|
| `mini-midoffice-domain` | JPA entities (`TravellerProfile`, `Booking`), enums, exceptions, `ReportResult` DTO |
| `mini-midoffice-integration` | Anti-corruption ports (`GdsFlightSearchPort`, `GdsFlightCheckPort`) + Sabre adapters, HTTP client, OAuth2 |
| `mini-midoffice-persistence` | Hibernate DAOs (`TravellerProfileDao`, `BookingDao`) + JDBC reporting DAO |
| `mini-midoffice-service` | Business logic (`BookingServiceImpl` with TransactionTemplate pattern, `ProfileServiceImpl`, `SearchServiceImpl`, `ReportingServiceImpl`) + service DTOs |
| `mini-midoffice-web` | Spring MVC controllers, `GlobalExceptionHandler`, filters, CSV/Excel exporters, static SPA, `web.xml`, `logback.xml` |

### Spring Context Setup

Two-context pattern (mandatory for `@Transactional` to intercept correctly):

- **Root Context** — loaded by `ContextLoaderListener` (web.xml). Config class: `AppConfig`.
  Contains: services, DAOs, JPA `EntityManagerFactory`, HikariCP `DataSource`, Sabre beans.
  Imports `PersistenceConfig` and `IntegrationConfig`.
- **Web Context** — loaded by `DispatcherServlet` mapped to `/api/*`. Config class:
  `WebMvcConfig`. Contains: controllers, `@ControllerAdvice`. Inherits all Root beans.
- Static resources and `index.html` served by Tomcat `DefaultServlet` (not DispatcherServlet).

> **`main` vs migration branch:** on `main` the root context uses `LocalSessionFactoryBean` +
> `HibernateTransactionManager`. On `feature/spring6-hibernate6` (after Step 0+1) it uses
> `LocalContainerEntityManagerFactoryBean` + `JpaTransactionManager`.

### GDS Integration Pattern (Anti-Corruption Layer)

```
Service layer → GdsFlightSearchPort (interface, domain types)
                     └── SabreFlightSearchAdapter (maps to/from Sabre DTOs)
                               └── SabreHttpClient (OAuth2, HTTP, timing logs)

Service layer → GdsFlightCheckPort (interface, domain types)
                     └── SabreFlightCheckAdapter
                               └── SabreHttpClient
```

### Booking Reserve Transaction Pattern

```
BookingServiceImpl.reserve():
  1. readTemplate  (TX)       → verify traveller exists
  2. gdsFlightCheckPort.check (no TX) → real-time Sabre FlightCheck
  3. writeTemplate REQUIRES_NEW (TX) → INSERT booking with status=RESERVED
```

### Logging (Logback, three separate files)

Named loggers route to separate physical files in `${catalina.base}/logs/mini-midoffice/`:
- `com.jcarranza.minimidoffice.business` → `business.log` (profile/booking/search events)
- `com.jcarranza.minimidoffice.integration.sabre` → `sabre-integration.log` (GDS calls + timing)
- `com.jcarranza.minimidoffice.errors` → `errors.log` (500s only, full stack trace)

All three carry the MDC `requestId` (8-char UUID hex, injected by `RequestIdFilter`).

---

## 4. How to Build and Run

Full detail in `HOW-TO-RUN.md`. Short version:

### PostgreSQL (one-time setup)

```powershell
docker run -d --name mini-midoffice-db -p 5432:5432 `
  -e POSTGRES_PASSWORD=miniumbrella -e POSTGRES_DB=miniumbrella `
  postgres:16
```

Then connect and run the DDL from `docs/02-data-model.md` (CREATE TABLE traveller_profile,
booking — schema is not auto-created).

### Build

```powershell
docker run --rm `
  -v "C:/workspace/mini-midoffice:/workspace" `
  -v "$env:USERPROFILE/.m2:/root/.m2" `
  -w /workspace `
  maven:3.9.6-eclipse-temurin-17 `
  mvn clean package -DskipTests --no-transfer-progress
```

Output: `mini-midoffice-web/target/mini-midoffice-web-1.0.0-SNAPSHOT.war`

### Run

```powershell
docker run -d --name mini-midoffice-app -p 8080:8080 `
  -e JAVA_OPTS="-Ddb.url=jdbc:postgresql://host.docker.internal:5432/miniumbrella -Ddb.username=postgres -Ddb.password=miniumbrella" `
  -v "C:/workspace/mini-midoffice/mini-midoffice-web/target/mini-midoffice-web-1.0.0-SNAPSHOT.war:/usr/local/tomcat/webapps/ROOT.war:ro" `
  tomcat:10.1-jdk17
```

Wait ~10 s, then: `docker logs mini-midoffice-app --tail 5` — should end with
`Server startup in [N] milliseconds`.

**Important:** use `curl.exe` in PowerShell (not `curl`, which aliases to `Invoke-WebRequest`).

```powershell
curl.exe -s http://localhost:8080/api/profiles
```

---

## 5. Sabre Cert Specifics

- **Endpoint:** `https://api.cert.platform.sabre.com`
- **Auth:** OAuth2 client-credentials with double-Base64 encoding
  (`Base64(clientId:clientSecret)` → then `Base64("Basic <that>")`)
- **Credentials:** stored in `mini-midoffice-integration/src/main/resources/sabre.properties`
  (gitignored — never commit). See `sabre.properties.example` in the same directory for the
  required keys and format.
- **Key cert dataset limitations:**
  - BFM flight search (`/v4.1.0/shop/flights/fares`) only returns full segment data
    (airline, flight number, times) for **US origins**: `JFK`, `ORD`, `MIA`.
    European origins return pricing rows but no flight segments — this is a Sabre cert
    restriction, not a code bug.
  - FlightCheck (`/v1/offers/flightCheck`) confirmed working for:
    `LX|23|JFK|GVA|2026-07-18|19:25|2026-07-19|09:15|Y` (LX 23 JFK→GVA)
  - Most BFM results fail FlightCheck with "No such flight" in cert — expected behaviour.

For Sabre API raw responses and field mappings, see `docs/reference-sabre-api.md`.

---

## 6. Roadmap

### 6a. Git Repository — DONE

The repository was initialized on 2026-06-20 and pushed to GitHub.

**What was committed (initial commit):**
- 94 files, 10,352 insertions
- Author: `carranza-javier <kettenki@gmail.com>`
- Message: `Initial commit: JCarranza Mini-Midoffice — travel agency mid-office system (Java 17, Spring 5, Hibernate 5)`
- Remote: `origin` → `https://github.com/carranza-javier/mini-midoffice`
- Branch: `main`

**What the `.gitignore` excludes:**
- `**/sabre.properties`, `**/db.properties` — real credentials (`.example` counterparts are committed)
- `**/target/` — all Maven build output
- `*.war`, `*.jar`
- `.idea/`, `*.iml`
- `*.log`, `logs/`
- Root-level debug/export files: `sabre-dump.json`, `sabre-raw.json`, `sabre-raw.txt`,
  `by-destination.csv`, `by-destination.xlsx`, `by-provider.csv`, `by-month.csv`,
  and any future `/*.csv` or `/*.xlsx` dropped at the project root
- `.DS_Store`, `Thumbs.db`

**Known gotchas — commit author identity:**

The correct Git author for this project is:
```
user.name  = carranza-javier
user.email = kettenki@gmail.com
```

`carranzaragoza@gmail.com` is tied to an unrelated old GitHub account (`lledysile`) and must
**not** be used. If cloning onto a new machine, verify before the first commit:

```powershell
git config --global user.name
git config --global user.email   # must be kettenki@gmail.com
```

If the global config is wrong, set it:
```powershell
git config --global user.name  "carranza-javier"
git config --global user.email "kettenki@gmail.com"
```

Or set it repo-locally only (omit `--global`) if the machine is shared.

---

### 6b. Spring 6 / Hibernate 6 Migration — IN PROGRESS

Branch `feature/spring6-hibernate6` (baseline tagged `v1.0-spring5-hibernate5`).

| Step | Description | Commit | Status |
|------|-------------|--------|--------|
| 0+1 | POM bumps + `javax.*`→`jakarta.*` sweep | `625b642` | **DONE — build passes** |
| 2 | Spring 6 API adaptations (`-parameters` flag) | `b3fc89b` | **DONE** |
| 3 | Hibernate 6 query API | `79341be` | **DONE — no code changes** |
| 4 | Tomcat 10 (Docker, `web.xml`, Cargo) | `<!-- hash -->` | **DONE — smoke test passed** |

**Key decisions made in Step 0+1:**
- Spring 6 has no `org.springframework.orm.hibernate6` package — dropped `LocalSessionFactoryBean`
  and `HibernateTransactionManager`, replaced with `LocalContainerEntityManagerFactoryBean` +
  `JpaTransactionManager` + `HibernateJpaVendorAdapter`.
- DAOs now use `@PersistenceContext EntityManager` + `entityManager.unwrap(Session.class)`
  (instead of `@Autowired SessionFactory` + `getCurrentSession()`).
- `session.save/update/delete` → `persist/merge/remove` (removed in Hibernate 6).
- `PostgreSQL95Dialect` → `PostgreSQLDialect` (removed in Hibernate 6).
- `org.hibernate:hibernate-core` → `org.hibernate.orm:hibernate-core` (groupId relocated).

See `docs/10-migration-spring6.md` for the full migration log with all gotchas documented.

After all steps pass, tag: `git tag v2.0-spring6-hibernate6`

---

## 7. Full Detail

Read `docs/00-project-brief.md` through `docs/09-support-simulation.md` for complete
design decisions, data model, API contracts, Sabre integration detail, reporting queries,
observability architecture, and support simulation exercises.
