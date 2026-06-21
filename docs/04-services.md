# JCarranza Mini-Midoffice — Service Layer

> Phase 4 of 9. Three services: ProfileService, BookingService, SearchService.
> Spring transactions, business validations, REQUIRES_NEW pattern.

---

## 1. Service Layer Role

The service layer is the only place where:

- Transactions are opened and closed (never in DAOs or controllers).
- Business rules are validated and `BusinessException` is thrown.
- GDS calls are executed (never in controllers or DAOs).
- Domain entities are converted to DTOs before going up to the controller.

Services are Spring beans (`@Service`) registered in the **Root Application Context** (loaded by `ContextLoaderListener`). Controllers in the Web Context receive them via `@Autowired`.

---

## 2. ProfileService

### Contract

```java
public interface ProfileService {
    ProfileDTO create(CreateProfileRequest request);
    ProfileDTO update(Long id, UpdateProfileRequest request);
    ProfileDTO findById(Long id);
    List<ProfileDTO> findAll(int offset, int limit);
    List<ProfileDTO> search(String term, int offset, int limit);
    void delete(Long id);
}
```

### Business Rules

| Rule | Implementation |
|---|---|
| Unique email | `profileDao.existsByEmail()` before `save()` |
| On update: uniqueness only if email changes | Comparison with the current value before going to DB |
| Required fields | Explicit validation in `create()`: firstName, lastName, email |
| Delete with active bookings | The `ON DELETE RESTRICT` FK in DB prevents the delete; the SQL exception is converted to 409 by `@ControllerAdvice` |

### Transactions

| Method | Propagation | readOnly |
|---|---|---|
| `create()` | REQUIRED | false |
| `update()` | REQUIRED | false — dirty checking persists changes on commit |
| `findById()` | REQUIRED | true |
| `findAll()` | REQUIRED | true |
| `search()` | REQUIRED | true |
| `delete()` | REQUIRED | false |

In `update()`, `profileDao.update()` is not called explicitly: Hibernate dirty checking detects modifications to the managed entity and issues the `UPDATE` when flushing before commit.

---

## 3. BookingService — REQUIRES_NEW Pattern

### Why @Transactional Alone Is Not Enough

If `reserve()` had a standard `@Transactional`, the transaction would remain active during the HTTP call to the GDS (2–5 s). With `HikariCP` configured with 10 connections, 10 concurrent reservations would exhaust the pool. In travel agency systems, booking peaks are real.

### Solution: TransactionTemplate with Explicit Demarcation

Two `TransactionTemplate` instances created in the constructor:

```java
readTemplate  = new TransactionTemplate(txManager);          // REQUIRED, readOnly
writeTemplate = new TransactionTemplate(txManager);
writeTemplate.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
```

**Why not @Transactional on private methods:**
Spring AOP only intercepts calls that go through the proxy. `this.method()` within the same class does not go through the proxy → the annotation is ignored. `TransactionTemplate` does not have this problem: it operates on `PlatformTransactionManager` directly.

### reserve() Flow

The flow creates a RESERVED booking directly in a single POST — no intermediate state exists.

```
reserve(BookingReserveRequest{flightKey, travellerId, searchedPrice, currencyCode})
│
├─► FlightCheckRequest.fromFlightKey(flightKey, currency, pcc)
│      Fails fast if the flightKey format is invalid (before touching DB or GDS)
│
├─► readTemplate.execute()              [short read TX — opens]
│      profileDao.findById(travellerId) → or BusinessException("Traveller not found")
│   [TX commit — DB connection released to pool]
│
├─► flightCheckPort.check(checkRequest) [NO TX, NO DB connection]  ← 2-5 s HTTP
│
├─► if (!result.isAvailable())          → BusinessException("Flight is no longer available")
├─► if (price > searched * tolerance)   → BusinessException("Price exceeds tolerance")
│
└─► writeTemplate.execute()             [new REQUIRES_NEW TX — opens]
       profileDao.findById(travellerId) → TravellerProfile managed in this session
       new Booking()
           .setTraveller(traveller)
           .setOrigin(checkRequest.getOriginCode())
           .setDestination(checkRequest.getDestinationCode())
           .setFlightKey(flightKey)
           .setDepartureDate(checkRequest.getDepartureDate())
           .setSearchedPrice(searchedPrice)
           .setConfirmedPrice(result.getTotalPrice())
           .setProvider(SABRE)
           .setStatus(RESERVED)
           .setReservationDate(now())
       bookingDao.save(booking)         ← Hibernate issues INSERT, generates ID from sequence
       return toDTO(booking)
    [TX commit]
```

**Origin and destination** are extracted from the `flightKey` via `FlightCheckRequest.fromFlightKey()` — no additional fields in the payload are needed.

**Price tolerance** configurable via `booking.priceTolerance` (default `1.05` = 5%). In cert, `100.0` is recommended to absorb the difference between the BFM reference price and the real FlightCheck price.

### BookingService Transactions

| Method | Mechanism | Notes |
|---|---|---|
| `reserve()` | TransactionTemplate (read + write) | FlightCheck HTTP between the two transactions |
| `cancel()` | `@Transactional` | Single TX: read → setStatus(CANCELLED) → commit |
| `findById()` | `@Transactional(readOnly=true)` | |
| `findByTraveller()` | `@Transactional(readOnly=true)` | Validates that the traveller exists |

### Booking Lifecycle

```
[POST /api/bookings]  →  FlightCheck OK  →  RESERVED
                                          ↓
                                       CANCELLED  (PUT /api/bookings/{id}/cancel)
```

The SEARCHED status does not exist. The booking is created directly in RESERVED if the FlightCheck confirms availability and price. This eliminates orphan records in DB and simplifies the data model.

---

## 4. SearchService

### Contract

```java
public interface SearchService {
    List<FlightOptionDTO> search(FlightSearchRequest request);
}
```

### search() Flow

`SearchService` is stateless with respect to DB: it only calls the GDS and returns results. It does not persist anything.

```
search(FlightSearchRequest{originCode, destinationCode?, fromDate?, toDate?, ...})
│
├─► flightSearchPort.search(criteria)   ← BFM call to Sabre (~1 s)
│      → List<FlightOption> (up to ~1,470 results from JFK)
│
├─► if destinationCode is not null:
│      filter client-side by FlightOption.destinationCode
│      (the BFM is open-destination — Sabre does not accept a destination filter in the request)
│
└─► return List<FlightOptionDTO>
```

No `@Transactional` — no DB operations. The only cost is the HTTP call to the GDS.

The `travellerId` that the SPA optionally sends in the search form **does not reach the service** — the controller ignores it. The search is always anonymous; the traveller identity only comes into play at the time of booking.

---

## 5. Service Layer DTOs

Domain entities (`Booking`, `TravellerProfile`) never leave the service layer.

| DTO | Direction | Fields |
|---|---|---|
| `ProfileDTO` | Service → Controller | id, firstName, lastName, email, company, passportNumber, frequentFlyerNumber, createdAt |
| `CreateProfileRequest` | Controller → Service | firstName, lastName, email, company, passportNumber, frequentFlyerNumber |
| `UpdateProfileRequest` | Controller → Service | all optional (only non-null fields are updated) |
| `BookingDTO` | Service → Controller | id, travellerId, travellerFullName, origin, destination, flightKey, departureDate, searchDate, reservationDate, searchedPrice, confirmedPrice, provider, status |
| `FlightOptionDTO` | Service → Controller | flightKey, originCode, destinationCode, departureDate, departureTime, arrivalTime, airlineCode, flightNumber, durationMinutes, totalPrice, currencyCode |
| `FlightSearchRequest` | Controller → Service | originCode, destinationCode?, fromDate?, toDate?, lengthOfStay, passengerCount, currencyCode |
| `BookingReserveRequest` | Controller → Service | flightKey, travellerId, searchedPrice, currencyCode |

---

## 6. Error Handling

| Exception | When | Thrown by |
|---|---|---|
| `BusinessException` | Business rule violated (duplicate email, flight not available, price out of tolerance, entity not found) | ServiceImpl |
| `IntegrationException` | HTTP error from GDS (timeout, 4xx, 5xx) or deserialization failure | SabreHttpClient, propagated without catching by the services |

The `@ControllerAdvice` maps these exceptions to HTTP codes:
- `BusinessException` → 400 (or 409 depending on semantics)
- `IntegrationException` → 502 Bad Gateway or 504 Gateway Timeout
- `DataIntegrityViolationException` (FK restrict on delete) → 409 Conflict

---

## 7. Package Structure

```
mini-midoffice-service/src/main/java/com/jcarranza/minimidoffice/service/
├── ProfileService.java
├── ProfileServiceImpl.java
├── BookingService.java
├── BookingServiceImpl.java
├── SearchService.java
├── SearchServiceImpl.java
└── dto/
    ├── ProfileDTO.java
    ├── CreateProfileRequest.java
    ├── UpdateProfileRequest.java
    ├── BookingDTO.java
    ├── FlightOptionDTO.java
    ├── FlightSearchRequest.java
    └── BookingReserveRequest.java
```

---

*Next phase → docs/05-rest-endpoints.md: REST controllers, @ControllerAdvice, Spring MVC configuration.*
