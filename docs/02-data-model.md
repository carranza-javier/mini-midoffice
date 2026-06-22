# JCarranza Mini-Midoffice — Data Model

> Phase 2 of 9. Hibernate entities, DAOs with native Session, PostgreSQL scripts.

---

## 1. Entity Diagram

```
┌──────────────────────────────────┐         ┌──────────────────────────────────────────┐
│         traveller_profile        │         │                  booking                 │
├──────────────────────────────────┤         ├──────────────────────────────────────────┤
│ id                    BIGINT PK  │◄────────│ id                BIGINT PK              │
│ first_name            VARCHAR    │  1      │ traveller_id      BIGINT FK NOT NULL      │
│ last_name             VARCHAR    │  │      │ origin            VARCHAR(3) NOT NULL     │
│ email                 VARCHAR UQ │  └──N   │ destination       VARCHAR(3) NOT NULL     │
│ company               VARCHAR    │         │ flight_key        VARCHAR(200)            │
│ passport_number       VARCHAR    │         │ departure_date    DATE                    │
│ frequent_flyer_number VARCHAR    │         │ search_date       TIMESTAMP NOT NULL      │
│ created_at            TIMESTAMP  │         │ reservation_date  TIMESTAMP               │
└──────────────────────────────────┘         │ searched_price    NUMERIC(10,2)           │
                                             │ confirmed_price   NUMERIC(10,2)           │
                                             │ provider          VARCHAR(20) NOT NULL    │
                                             │ status            VARCHAR(20) NOT NULL    │
                                             └──────────────────────────────────────────┘
```

Relationship: one `TravellerProfile` can have N `Booking`s. FK with `ON DELETE RESTRICT` (a traveller with bookings cannot be deleted).

---

## 2. Entity `TravellerProfile`

**Class:** `com.jcarranza.minimidoffice.domain.model.TravellerProfile`  
**Table:** `traveller_profile`

| Java Field          | SQL Column             | SQL Type      | Constraints                 |
|---------------------|------------------------|---------------|-----------------------------|
| `id`                | `id`                   | BIGINT        | PK, sequence                |
| `firstName`         | `first_name`           | VARCHAR(100)  | NOT NULL                    |
| `lastName`          | `last_name`            | VARCHAR(100)  | NOT NULL                    |
| `email`             | `email`                | VARCHAR(255)  | NOT NULL, UNIQUE            |
| `company`           | `company`              | VARCHAR(200)  | nullable                    |
| `passportNumber`    | `passport_number`      | VARCHAR(50)   | nullable                    |
| `frequentFlyerNumber`| `frequent_flyer_number`| VARCHAR(50)  | nullable                    |
| `createdAt`         | `created_at`           | TIMESTAMP     | NOT NULL, not updatable     |

**`@PrePersist`:** sets `createdAt = LocalDateTime.now()`. The column has `updatable = false` for additional ORM-level guarantee.

**Validation annotations:** `@NotBlank` on firstName/lastName/email, `@Email` on email, `@Size(max=N)` on all text fields. Evaluated in the service layer with `@Valid`.

---

## 3. Entity `Booking`

**Class:** `com.jcarranza.minimidoffice.domain.model.Booking`  
**Table:** `booking`

| Java Field         | SQL Column          | SQL Type      | Constraints                          |
|--------------------|---------------------|---------------|--------------------------------------|
| `id`               | `id`                | BIGINT        | PK, sequence                         |
| `traveller`        | `traveller_id`      | BIGINT        | FK → traveller_profile, NOT NULL     |
| `origin`           | `origin`            | VARCHAR(3)    | NOT NULL, IATA code (e.g.: ZRH)      |
| `destination`      | `destination`       | VARCHAR(3)    | NOT NULL, IATA code (e.g.: MAD)      |
| `flightKey`        | `flight_key`        | VARCHAR(200)  | nullable in SEARCHED without fixed flight |
| `departureDate`    | `departure_date`    | DATE          | nullable (extracted from flightKey)   |
| `searchDate`       | `search_date`       | TIMESTAMP     | NOT NULL, defaults to now() in @PrePersist |
| `reservationDate`  | `reservation_date`  | TIMESTAMP     | null until it moves to RESERVED       |
| `searchedPrice`    | `searched_price`    | NUMERIC(10,2) | price from the search (may be cached) |
| `confirmedPrice`   | `confirmed_price`   | NUMERIC(10,2) | price post–Flight Check, real         |
| `provider`         | `provider`          | VARCHAR(20)   | NOT NULL, STRING enum                 |
| `status`           | `status`            | VARCHAR(20)   | NOT NULL, STRING enum                 |

### Lifecycle Statuses

```
          GdsFlightSearchPort.search()          GdsFlightCheckPort.check() OK
[initial] ─────────────────────────────► SEARCHED ───────────────────────────► RESERVED
                                            │                                       │
                                            │ check() fails / user cancels          │ user cancels
                                            ▼                                       ▼
                                        CANCELLED                              CANCELLED
```

### `flightKey` Field

Opaque key that uniquely identifies the specific flight chosen by the user. Format: `airline|number|date|cabin`.

Example: `IB3456|2026-07-15|Y`

- Persisted in `SEARCHED` when the user selects a flight from the results.
- It is the key that `BookingService` passes to `GdsFlightCheckPort.check()` for revalidation.
- The partial index `idx_booking_flightkey_traveller WHERE status = 'SEARCHED'` makes the `findSearchedByFlightKeyAndTraveller` query efficient.

### Dual Price Fields (`searchedPrice` / `confirmedPrice`)

Both exist by design: `searchedPrice` is what the user saw in the exploratory search (may be outdated due to Sabre's cache). `confirmedPrice` is what `GdsFlightCheckPort` returns in real time just before `RESERVED`. Storing both allows auditing discrepancies and for revenue reporting.

### `FetchType.LAZY` in `@ManyToOne`

The `traveller` is loaded LAZY. DAOs return `Booking` and the service layer accesses `booking.getTraveller()` only when needed (within the same transaction). If accessed outside a transaction, Hibernate throws `LazyInitializationException` — a signal that the access is incorrectly placed.

---

## 4. Domain Enums

### `BookingStatus`

| Value       | Meaning                                              |
|-------------|------------------------------------------------------|
| `SEARCHED`  | The user ran a search and chose a flight             |
| `RESERVED`  | The Flight Check passed and the booking is confirmed |
| `CANCELLED` | The traveller or agent cancelled the booking         |

### `GdsProvider`

| Value     | Meaning                                          |
|-----------|--------------------------------------------------|
| `SABRE`   | Active integration (Sabre Sandbox)               |
| `AMADEUS` | Reserved for future integration                  |
| `GALILEO` | Reserved for future integration                  |

Both enums are stored as `VARCHAR` with `@Enumerated(EnumType.STRING)`. Never `ORDINAL` — the value changes if the enum is reordered.

---

## 5. ID Generation

Native PostgreSQL sequences (`CREATE SEQUENCE`) with `allocationSize = 1` are used.

**Why `SEQUENCE` and not `IDENTITY` (serial/generated always):**

- `IDENTITY` requires an individual INSERT to obtain the generated ID (`RETURNING id`), incompatible with batch inserts.
- `SEQUENCE` allows Hibernate to pre-fetch the next value with `SELECT nextval(...)` before the INSERT, enabling `hibernate.jdbc.batch_size = 25` for bulk load operations.
- PostgreSQL supports sequences natively and efficiently.

`allocationSize = 1` because neither Hi-Lo nor pooled optimizer is used: it guarantees that IDs in DB and Hibernate are always consistent, at the cost of one `SELECT nextval` per INSERT. Acceptable for this system's volume.

---

## 6. DAO Contracts

### `TravellerProfileDao`

| Method                                          | HQL/Hibernate Implementation                                |
|-------------------------------------------------|-------------------------------------------------------------|
| `findById(Long)`                                | `session.get(TravellerProfile.class, id)`                   |
| `findAll(int offset, int limit)`                | HQL `FROM TravellerProfile ORDER BY lastName, firstName`     |
| `search(String term, int offset, int limit)`    | HQL LOWER LIKE on firstName, lastName, email, company        |
| `existsByEmail(String)`                         | HQL `SELECT COUNT(p) ... WHERE p.email = :email`            |
| `save(TravellerProfile)`                        | `session.save(profile)` — generates ID via sequence         |
| `update(TravellerProfile)`                      | `session.update(profile)` — re-attaches detached entity     |
| `delete(Long)`                                  | `session.get()` + `session.delete()` — respects ORM cascades |

### `BookingDao`

| Method                                                        | HQL/Hibernate Implementation                                             |
|---------------------------------------------------------------|--------------------------------------------------------------------------|
| `findById(Long)`                                              | `session.get(Booking.class, id)`                                         |
| `findByTravellerId(Long)`                                     | HQL `WHERE b.traveller.id = :travellerId ORDER BY b.searchDate DESC`     |
| `findByStatus(BookingStatus)`                                 | HQL `WHERE b.status = :status`                                           |
| `findSearchedByFlightKeyAndTraveller(String, Long)`           | HQL `WHERE flightKey = :fk AND traveller.id = :tid AND status = SEARCHED`|
| `save(Booking)`                                               | `session.save(booking)`                                                  |
| `update(Booking)`                                             | `session.update(booking)`                                                |

**Note on dirty checking:** within a transaction, if the service loads a `Booking` with `findById`, modifies its fields, and the transaction commits, Hibernate automatically detects the changes and issues the UPDATE without explicitly calling `update()`. The DAO's `update()` method is only needed when the entity comes from another transaction (detached) — as in the SEARCHED→RESERVED flow with `REQUIRES_NEW`.

---

## 7. Relevant HQL Queries

### Profile Search (with pagination)

```hql
FROM TravellerProfile p
WHERE LOWER(p.firstName) LIKE :term
   OR LOWER(p.lastName)  LIKE :term
   OR LOWER(p.email)     LIKE :term
   OR LOWER(p.company)   LIKE :term
ORDER BY p.lastName, p.firstName
```

`LOWER()` in HQL is compatible with PostgreSQL. For production with high volume, a functional index `LOWER(last_name)` would be added or PG full-text search would be used. For now LIKE is sufficient for the MVP.

### Pre-Flight Check Verification

```hql
FROM Booking b
WHERE b.flightKey    = :flightKey
  AND b.traveller.id = :travellerId
  AND b.status       = :status      -- SEARCHED
```

The partial index `WHERE status = 'SEARCHED'` in the DDL makes this query O(log n) over the subset of pending searches.

### Traveller Booking History

```hql
FROM Booking b WHERE b.traveller.id = :travellerId ORDER BY b.searchDate DESC
```

Covered by `idx_booking_traveller`.

---

## 8. PostgreSQL Schema — Design Decisions

> Runnable DDL: `mini-midoffice-persistence/src/main/resources/db/V1__create_schema.sql` and
> `V2__remove_searched_status.sql`. Apply procedure: **HOW-TO-RUN.md — section 0**.

| Decision | Alternative | Reason |
|---|---|---|
| Explicit `SEQUENCE` + `DEFAULT nextval()` | `SERIAL` / `BIGSERIAL` | Explicit control of the sequence name for Hibernate's `@SequenceGenerator`; no implicit PG magic |
| `NUMERIC(10,2)` for prices | `FLOAT` / `DOUBLE` | Never `float` for money — rounding errors in binary floating point. `NUMERIC` is exact |
| `TIMESTAMP` without timezone | `TIMESTAMPTZ` | The app stores `LocalDateTime` (no zone); all infrastructure runs in UTC. If multi-zone is needed, migrate to `TIMESTAMPTZ` + `OffsetDateTime` |
| `VARCHAR(3)` for IATA codes | `CHAR(3)` | Avoids `CHAR` padding in comparisons; PostgreSQL has no performance difference |
| `ON DELETE RESTRICT` on FK | `CASCADE` / `SET NULL` | A traveller with bookings cannot be accidentally deleted; requires cancelling the bookings first |
| CHECK constraints on `status` and `provider` | Java validation only | Double barrier: if someone inserts via direct SQL (ad-hoc support, reporting), the DB rejects invalid values |
| Partial index `WHERE status = 'SEARCHED'` | Normal index | The `findSearchedByFlightKeyAndTraveller` query only works with SEARCHED; the partial index is smaller and faster |
| `DATE` for `departure_date` | `TIMESTAMP` | For reporting by travel month, `DATE` is sufficient. Departure time is not relevant for aggregates |

---

## 9. Hibernate Configuration — PersistenceConfig

**Class:** `com.jcarranza.minimidoffice.persistence.config.PersistenceConfig`

Key points:

- Spring's `LocalSessionFactoryBean` — creates the `SessionFactory` from `DataSource` + package scan + properties.
- `HibernateTransactionManager` — manages Hibernate transactions (not JTA, not DataSourceTransactionManager). Required for `@Transactional` in services to work with `sessionFactory.getCurrentSession()`.
- `hibernate.current_session_context_class = SpringSessionContext` — the hook that connects `getCurrentSession()` with the Spring transaction context. Without this, `getCurrentSession()` throws `HibernateException: No CurrentSessionContext configured!`
- `hbm2ddl.auto = validate` — Hibernate verifies that the existing schema matches the entities. If there is a difference, it fails at startup. Safe for production.
- `HikariDataSource` with a pool of 10 connections and configured timeouts. `destroyMethod = "close"` ensures the pool closes cleanly when Tomcat stops.

---

## 10. Files Generated in This Phase

```
mini-midoffice/
├── pom.xml                                              ← parent BOM
├── mini-midoffice-domain/
│   ├── pom.xml
│   └── src/main/java/com/jcarranza/minimidoffice/domain/
│       ├── enums/
│       │   ├── BookingStatus.java
│       │   └── GdsProvider.java
│       ├── exception/
│       │   ├── BusinessException.java
│       │   └── IntegrationException.java
│       └── model/
│           ├── TravellerProfile.java
│           └── Booking.java
├── mini-midoffice-persistence/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/jcarranza/minimidoffice/persistence/
│       │   ├── dao/
│       │   │   ├── TravellerProfileDao.java
│       │   │   └── BookingDao.java
│       │   ├── hibernate/
│       │   │   ├── HibernateTravellerProfileDao.java
│       │   │   └── HibernateBookingDao.java
│       │   └── config/
│       │       └── PersistenceConfig.java
│       └── resources/
│           ├── db.properties                            ← template (no real credentials)
│           └── db/
│               └── V1__create_schema.sql
├── mini-midoffice-integration/pom.xml                   ← stub for this phase
├── mini-midoffice-service/pom.xml                       ← stub for this phase
└── mini-midoffice-web/pom.xml                           ← stub for this phase
```

---

*Next phase → docs/03-sabre-integration.md: Sabre HTTP client, OAuth2, DTOs, mappers, GdsFlightSearchPort and GdsFlightCheckPort.*
