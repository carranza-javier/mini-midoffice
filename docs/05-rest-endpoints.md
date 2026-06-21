# JCarranza Mini-Midoffice — REST Endpoints

> Phase 5 of 9. Spring MVC controllers, GlobalExceptionHandler, web context configuration.

---

## 1. Context: DispatcherServlet at /api/*

The `DispatcherServlet` is mapped **exclusively** to `/api/*` (see `web.xml`). Consequences:

- Controllers use routes without the `/api/` prefix (`@RequestMapping("/profiles")` → `/api/profiles`).
- `index.html` and static resources (`/static/**`) are served by Tomcat DefaultServlet directly — they do not go through Spring MVC.
- Root Context / Web Context separation is mandatory for `@Transactional` (Root) to be correctly intercepted by AOP before reaching the controllers (Web).

---

## 2. Endpoints

### 2.1 Profiles — `/api/profiles`

| Method | URL | Body / Params | 2xx | Errors |
|---|---|---|---|---|
| GET | `/api/profiles` | `?offset=0&limit=20&q=text` | 200 | — |
| GET | `/api/profiles/{id}` | — | 200 | 400 if not found |
| POST | `/api/profiles` | `CreateProfileRequest` JSON | 201 | 400 missing fields · 409 duplicate email |
| PUT | `/api/profiles/{id}` | `UpdateProfileRequest` JSON | 200 | 400 if not found · 409 duplicate email |

`CreateProfileRequest` (required fields: `firstName`, `lastName`, `email`):
```json
{
  "firstName": "Ana",
  "lastName": "Roca",
  "email": "ana.roca@example.com",
  "company": "Viajes Sol",
  "passportNumber": "ES12345678",
  "frequentFlyerNumber": "IB-GOLD-9900"
}
```

`ProfileDTO` (response):
```json
{
  "id": 1,
  "firstName": "Ana",
  "lastName": "Roca",
  "fullName": "Ana Roca",
  "email": "ana.roca@example.com",
  "company": "Viajes Sol",
  "passportNumber": "ES12345678",
  "frequentFlyerNumber": "IB-GOLD-9900",
  "createdAt": "2026-07-17T10:30:00"
}
```

---

### 2.2 Flights — `/api/flights`

| Method | URL | Params | 2xx | Errors |
|---|---|---|---|---|
| GET | `/api/flights/search` | see table | 200 array | 400 missing origin · 502/504 GDS fails |

Parameters for `/api/flights/search`:

| Param | Type | Required | Default | Notes |
|---|---|---|---|---|
| `origin` | String | Yes | — | IATA code, e.g.: `JFK` |
| `destination` | String | No | — | If omitted: open-jaw |
| `fromDate` | ISO date | No | — | `yyyy-MM-dd` |
| `toDate` | ISO date | No | — | `yyyy-MM-dd` |
| `lengthOfStay` | int | No | 7 | Length of stay in days |
| `passengerCount` | int | No | 1 | |
| `currencyCode` | String | No | EUR | |
| `travellerId` | Long | No | — | If provided → persists SEARCHED bookings |

Example: `GET /api/flights/search?origin=JFK&fromDate=2026-07-15&toDate=2026-07-20&travellerId=1`

`FlightOptionDTO` (response, array):
```json
[
  {
    "flightKey": "AA|90|JFK|MAD|2026-07-17|22:50|2026-07-18|11:55|Y",
    "originCode": "JFK",
    "destinationCode": "MAD",
    "departureDate": "2026-07-17",
    "departureTime": "22:50",
    "arrivalTime": "11:55",
    "airlineCode": "AA",
    "flightNumber": 90,
    "flightDisplay": "AA 90",
    "durationMinutes": 425,
    "totalPrice": 487.60,
    "currencyCode": "USD"
  }
]
```

> **Cert sandbox note:** Flight Search only returns complete results with origin **JFK** (or other US origins).
> European origins like ZRH or MAD return prices without flight segments — limitation of the Sabre
> certification dataset, not the code. For bookings use the `flightKey` from FlightCheck (LX 2020 ZRH→MAD
> does work — see section 2.3 and `docs/reference-sabre-api.md`).

---

### 2.3 Bookings — `/api/bookings`

| Method | URL | Body / Params | 2xx | Errors |
|---|---|---|---|---|
| POST | `/api/bookings` | `BookingReserveRequest` JSON | 201 | 400 flight not available · 400 price >5% · 400 no prior SEARCHED · 502/504 GDS |
| GET | `/api/bookings?travellerId={id}` | — | 200 array | 400 traveller not found |
| GET | `/api/bookings/{id}` | — | 200 | 400 not found |
| PUT | `/api/bookings/{id}/cancel` | — | 200 | 409 already cancelled · 400 not found |

`BookingReserveRequest` (body of POST /api/bookings):
```json
{
  "flightKey":     "LX|2020|ZRH|MAD|2026-07-17|06:55|2026-07-17|09:20|Y",
  "travellerId":   1,
  "searchedPrice": 162.34,
  "currencyCode":  "EUR"
}
```

`BookingDTO` (response):
```json
{
  "id": 7,
  "travellerId": 1,
  "travellerFullName": "Ana Roca",
  "origin": "ZRH",
  "destination": "MAD",
  "flightKey": "LX|2020|ZRH|MAD|2026-07-17|06:55|2026-07-17|09:20|Y",
  "departureDate": "2026-07-17",
  "searchDate": "2026-07-10T09:00:00",
  "reservationDate": "2026-07-10T09:05:23",
  "searchedPrice": 162.34,
  "confirmedPrice": 162.34,
  "provider": "SABRE",
  "status": "RESERVED"
}
```

---

## 3. Standard Error Body (ApiError)

All error responses have the same format:

```json
{
  "status":    409,
  "error":     "Conflict",
  "message":   "Email already registered: ana.roca@example.com",
  "timestamp": "2026-07-17T10:31:45",
  "path":      "/api/profiles"
}
```

---

## 4. GlobalExceptionHandler — Exception Mapping

```
BusinessException(httpStatus=400) → 400 Bad Request
BusinessException(httpStatus=409) → 409 Conflict
IntegrationException(httpStatus=-1) → 504 Gateway Timeout   ← I/O error / timeout calling GDS
IntegrationException(httpStatus=N)  → 502 Bad Gateway        ← HTTP error from GDS
DataIntegrityViolationException     → 409 Conflict           ← FK restrict, unique at DB level
MissingServletRequestParameterException → 400
HttpMessageNotReadableException     → 400
Exception                           → 500 Internal Server Error
```

400 vs 409 cases in `BusinessException`:
- `400`: flight not available, price out of tolerance, entity not found, missing required field.
- `409`: email already registered (`ProfileServiceImpl.create/update`), booking already cancelled (`BookingServiceImpl.cancel`).

The httpStatus is passed to the `BusinessException` constructor: `new BusinessException("...", 409)`.

---

## 5. Spring MVC Configuration

### Two Contexts (web.xml)

```
Root Context (AppConfig)
  @ComponentScan: com.jcarranza.minimidoffice.service
                  com.jcarranza.minimidoffice.persistence.hibernate
                  com.jcarranza.minimidoffice.integration.sabre.mapper
  @Import: PersistenceConfig, IntegrationConfig
  @EnableTransactionManagement

Web Context (WebMvcConfig)
  @EnableWebMvc
  @ComponentScan: com.jcarranza.minimidoffice.web.controller
                  com.jcarranza.minimidoffice.web.advice
  configureMessageConverters: Jackson + JavaTimeModule (ISO dates)
  addCorsMappings: /api/** allows localhost:8080/8090
  configureDefaultServletHandling: enables Tomcat for statics
```

### MVC ObjectMapper vs Sabre ObjectMapper

| Bean | Owner | Use |
|---|---|---|
| ObjectMapper in `WebMvcConfig` | Web Context | Serializes HTTP JSON responses to the browser |
| `sabreObjectMapper` in `IntegrationConfig` | Root Context | Serializes/deserializes JSON to/from Sabre |

Two independent instances to prevent Sabre's configuration (NON_NULL, IGNORE_UNKNOWN) from affecting REST responses to the browser.

---

## 6. RequestLoggingFilter

Registered in `web.xml` only for `/api/*`. Logs method, URI, query string, HTTP status and duration:

```
[REQUEST] POST /api/bookings → 201 (847 ms)
[REQUEST] GET  /api/flights/search?origin=JFK → 200 (1203 ms)
[REQUEST] PUT  /api/bookings/7/cancel → 409 (12 ms)
```

Useful for integration diagnosis (Phases 8 and 9).

---

## 7. Package Structure

```
mini-midoffice-web/src/main/java/com/jcarranza/minimidoffice/web/
├── config/
│   ├── AppConfig.java            — Root Context: services, DAOs, Sabre
│   └── WebMvcConfig.java         — Web Context: MVC, Jackson, CORS
├── controller/
│   ├── ProfileController.java    — /api/profiles/**
│   ├── FlightController.java     — /api/flights/**
│   └── BookingController.java    — /api/bookings/**
├── advice/
│   └── GlobalExceptionHandler.java — @RestControllerAdvice
├── dto/
│   └── ApiError.java             — standard JSON body for errors
└── filter/
    └── RequestLoggingFilter.java — log per request

mini-midoffice-web/src/main/webapp/WEB-INF/
└── web.xml                       — ContextLoaderListener + DispatcherServlet
```

---

## 8. Complete Booking Flow (end-to-end)

```
Browser
  GET /api/flights/search?origin=JFK&travellerId=1
      → FlightController → SearchService → GdsFlightSearchPort (Sabre)
      → persists Booking(SEARCHED) for each option
      → returns List<FlightOptionDTO>  [200 OK]

Browser shows flight table, user selects LX 2020

  POST /api/bookings  { flightKey: "LX|2020|...", travellerId: 1, searchedPrice: 162.34 }
      → BookingController → BookingService.reserve()
          TX1: reads Booking(SEARCHED)  [TX commit, connection free]
          HTTP: GdsFlightCheckPort.check()  [no TX, 1-3s]
          Validation: available=true, price=162.34 ≤ 162.34×1.05  ✓
          TX2 REQUIRES_NEW: Booking → RESERVED, confirmedPrice=162.34  [TX commit]
      → returns BookingDTO  [201 Created]

Browser shows confirmation with real price
```

---

*Next phase → docs/06-frontend.md: SPA jQuery + Handlebars + Bootstrap 3.*
