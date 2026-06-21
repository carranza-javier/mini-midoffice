# JCarranza Mini-Midoffice — Sabre Integration

> Phase 3 of 9. Complete integration layer: two GDS ports, Sabre adapters, OAuth2, HTTP client, DTOs and mappers.

Technical reference: `docs/reference-sabre-api.md`

---

## 1. Context: what works in the cert environment with DEVCENTER credentials

Critical finding validated in June 2026 (see `reference-sabre-api.md`):

| API | Endpoint | ZRH→MAD | JFK→X | Notes |
|---|---|---|---|---|
| Flight Search | `POST /v1/offers/flightSearch` | ❌ empty response | ✅ 1430 offers, 73 destinations | European origin has no data in cert cache |
| Flight Check | `POST /v1/offers/flightCheck` | ✅ LX 2020 works | n/a | Not a search — revalidates a known flight |
| InstaFlights (live) | `GET /v1/shop/flights?mode=live` | ❌ 404 | ✅ JFK→LAX | 404 = no data in cache, not an auth error |

**Design consequence:** the two ports have different behaviors and availability. This is the underlying reason for separating them into `GdsFlightSearchPort` and `GdsFlightCheckPort`.

---

## 2. OAuth2 Authentication — Double Base64

Sabre DEVCENTER uses an unusual encoding that is not clearly documented. The correct header signature is:

```
Authorization: Basic base64( base64(CLIENT_ID) + ":" + base64(CLIENT_SECRET) )
```

Implemented in `SabreOAuthProvider.buildDoubleBase64Credential()`:

```java
String b64Id  = Base64.getEncoder().encodeToString(clientId.getBytes(UTF_8));
String b64Sec = Base64.getEncoder().encodeToString(clientSecret.getBytes(UTF_8));
return Base64.getEncoder().encodeToString((b64Id + ":" + b64Sec).getBytes(UTF_8));
```

### Authentication Error Diagnosis

| Sabre Error | Cause | Action |
|---|---|---|
| `"Credentials are missing or the syntax is not correct"` | Incorrect encoding (simple Base64) | Verify that double Base64 is applied |
| `"Wrong clientID or clientSecret"` | Correct encoding, but expired credentials | Regenerate at developer.sabre.com |

### Token Lifecycle

The token lasts 7 days (604,800 s). `SabreOAuthProvider` caches it in memory with a 60-second margin:

```
token_expiry = now() + expires_in - 60s
```

Thread-safe via `synchronized` in `getValidToken()`. The `invalidate()` method allows forcing a refresh if a subsequent call returns 401.

---

## 3. Integration Layer Structure

```
com.jcarranza.minimidoffice.integration
├── port/                                  ← Contracts that services know
│   ├── GdsFlightSearchPort.java           — exploratory search interface
│   ├── GdsFlightCheckPort.java            — point revalidation interface
│   ├── FlightSearchCriteria.java          — search input
│   ├── FlightOption.java                  — search output
│   ├── FlightCheckRequest.java            — check input (with fromFlightKey())
│   └── FlightCheckResult.java             — check output
├── sabre/                                 ← Sabre adapters (implement the ports)
│   ├── SabreFlightSearchAdapter.java      — POST /v1/offers/flightSearch
│   ├── SabreFlightCheckAdapter.java       — POST /v1/offers/flightCheck
│   ├── auth/
│   │   ├── SabreOAuthProvider.java        — token cache + double Base64
│   │   └── SabreTokenResponse.java        — OAuth response DTO
│   ├── client/
│   │   └── SabreHttpClient.java           — Apache HttpClient 5, logs, IntegrationException
│   ├── dto/
│   │   ├── SabreFlightSearchRequest.java  — body POST flightSearch
│   │   ├── SabreFlightSearchResponse.java — flightSearch response
│   │   ├── SabreFlightCheckRequest.java   — body POST flightCheck
│   │   └── SabreFlightCheckResponse.java  — flightCheck response
│   └── mapper/
│       ├── SabreFlightSearchMapper.java   — Criteria→SabreReq, SabreResp→FlightOption[]
│       └── SabreFlightCheckMapper.java    — CheckReq→SabreReq, SabreResp→CheckResult
└── config/
    └── IntegrationConfig.java             — @Configuration: all Sabre beans
```

---

## 4. Port 1 — GdsFlightSearchPort (exploratory search)

```java
public interface GdsFlightSearchPort {
    List<FlightOption> search(FlightSearchCriteria criteria);
}
```

**Input (`FlightSearchCriteria`):**
- `originCode` — IATA code, e.g.: `"JFK"`
- `fromDate`, `toDate` — departure date range
- `lengthOfStay` — trip duration in days
- `passengerCount`, `currencyCode`

**Output (`FlightOption`):**
- `flightKey` — opaque key for later use in FlightCheck (see §6)
- `originCode`, `destinationCode`, `departureDate`
- `airlineCode`, `flightNumber`, `departureTime`, `arrivalTime`, `durationMinutes`
- `totalPrice`, `currencyCode`

**Sabre call:** `POST /v1/offers/flightSearch` with `returnFullOffers: true` to obtain the flight segment data needed to build the `flightKey`.

**Destination filtering:** The BFM is an *inspirational* (open-destination) search. Sabre does not accept a destination filter in the request — it always returns all available routes from the origin. The `destinationCode` field from `FlightSearchRequest` is applied **client-side** in `SearchServiceImpl` after receiving the complete Sabre response.

---

### Real `flightSearch` Response Structure (verified June 2026)

The response has three correlated arrays: `offers`, `journeys` and `flights`.
The resolution chain is:

```
Offer.journeyRefs[0]  →  Journey.id
Journey.flightRefs[]  →  Flight.id
Flight.(departureAirportCode, arrivalAirportCode, departureDate, …)
```

**The Journey does NOT contain its own route fields.** The initial DTO assumed fields
`originAirportCode`, `destinationAirportCode` and `departureDate` in the Journey — all null
because they do not exist in the real JSON. The route is obtained from the first and last `Flight` in
`flightRefs`.

Real JSON structure (fragment):
```json
{
  "journeys": [
    {
      "id": "c415709e-...",
      "requestedJourneyIndex": 0,
      "flightRefs": ["6734c9b1-...", "9c267624-..."]
    }
  ],
  "flights": [
    {
      "id": "6734c9b1-...",
      "departureAirportCode": "JFK",
      "arrivalAirportCode":   "MAD",
      "departureDate": "2026-07-03",
      "departureTime": "10:50",
      "arrivalDate":   "2026-07-04",
      "arrivalTime":   "11:45",
      "operatingAirlineCode":   "AT",
      "operatingFlightNumber":  203,
      "durationInMinutes":      600
    }
  ]
}
```

For itineraries with a stopover, `flightRefs` has N entries — `origin` = first Flight,
`destination` = last Flight, `durationMinutes` = sum of all segments.

---

## 5. Port 2 — GdsFlightCheckPort (revalidation before booking)

```java
public interface GdsFlightCheckPort {
    FlightCheckResult check(FlightCheckRequest request);
}
```

**Input (`FlightCheckRequest`):**
- Full flight identification: `airlineCode`, `flightNumber`, `originCode`, `destinationCode`, `departureDate`, `departureTime`, `arrivalDate`, `arrivalTime`, `bookingClass`
- `currencyCode`, `pseudoCityCode` (`PC18` in cert), `passengerCount`
- Static constructor `FlightCheckRequest.fromFlightKey(flightKey, currency, pcc)` to parse the key stored in `Booking`

**Output (`FlightCheckResult`):**
- `available` — whether the flight has availability
- `totalPrice`, `baseFare`, `taxes`, `currencyCode` — real-time price
- `additionalOffers` — up to 3 alternative fares returned by Sabre

**Sabre call:** `POST /v1/offers/flightCheck` with PCC `PC18`, `returnTaxBreakdown: true`, up to 3 additional offers.

---

## 6. flightKey Format

The `flightKey` is a string of 9 fields separated by `|` that uniquely identifies a specific flight:

```
airlineCode|flightNumber|origin|destination|departureDate|departureTime|arrivalDate|arrivalTime|bookingClass
```

Example (LX 2020 ZRH→MAD):
```
LX|2020|ZRH|MAD|2026-07-17|06:55|2026-07-17|09:20|Y
```

- Built in `SabreFlightSearchMapper.buildFlightKey()` from the segment data in the flightSearch response.
- Stored in `Booking.flightKey` when `status = SEARCHED`.
- Parsed back in `FlightCheckRequest.fromFlightKey()` when `BookingService` calls the FlightCheck.

This format allows `BookingService` to go from `Booking.flightKey` to a `flightCheck` call without needing to store flight details in additional DB fields.

---

## 7. Complete Flow: Search → Check → Reserve

```
[SearchService]
    │
    ├─► GdsFlightSearchPort.search(criteria)
    │       │
    │       └─ SabreFlightSearchAdapter
    │              → SabreOAuthProvider.getValidToken()
    │              → SabreHttpClient.post("/v1/offers/flightSearch", ...)
    │              → SabreFlightSearchMapper.toFlightOptions(response)
    │              → List<FlightOption> (with constructed flightKey)
    │
    └─► Booking saved (status=SEARCHED, flightKey="LX|2020|ZRH|MAD|2026-07-17|06:55|...|Y")

[BookingService — in separate REQUIRES_NEW transaction]
    │
    ├─► BookingDao.findSearchedByFlightKeyAndTraveller(flightKey, travellerId)
    │       → Booking(status=SEARCHED)
    │
    ├─► GdsFlightCheckPort.check(FlightCheckRequest.fromFlightKey(flightKey, "EUR", "PC18"))
    │       │
    │       └─ SabreFlightCheckAdapter
    │              → SabreOAuthProvider.getValidToken()
    │              → SabreHttpClient.post("/v1/offers/flightCheck", ...)
    │              → SabreFlightCheckMapper.toCheckResult(response)
    │              → FlightCheckResult(available=true, totalPrice=162.34, currency=EUR)
    │
    ├─► if (!result.isAvailable()) → throw BusinessException("Flight no longer available")
    ├─► if (price delta > 5%)     → throw BusinessException("Price changed: ...")
    │
    └─► booking.setStatus(RESERVED)
        booking.setConfirmedPrice(162.34)
        booking.setReservationDate(now())
        BookingDao.update(booking)
```

---

## 8. SabreHttpClient — Design and Diagnosis

`SabreHttpClient` wraps Apache HttpClient 5 with:
- Configurable timeouts via properties (`connectTimeoutSeconds`, `readTimeoutSeconds`)
- `DEBUG` log of each request (path + body truncated to 500 chars) and response (status + body)
- Every HTTP exception is re-thrown as `IntegrationException("SABRE", httpStatus, message)` — the service can catch it and decide whether to retry or fail

To activate integration logs in development:
```xml
<!-- logback.xml -->
<logger name="com.jcarranza.minimidoffice.integration.sabre" level="DEBUG"/>
```

---

## 9. External Configuration — sabre.properties

```properties
sabre.baseUrl=https://api.cert.platform.sabre.com
sabre.tokenUrl=https://api.cert.platform.sabre.com/v2/auth/token
sabre.clientId=V1:nfuoonfuxsvhti3y:DEVCENTER:EXT
sabre.clientSecret=Fv7oMTh1
sabre.pseudoCityCode=PC18
sabre.connectTimeoutSeconds=5
sabre.readTimeoutSeconds=30
```

`sabre.properties` is in **`.gitignore`** — credentials are never committed. Only `sabre.properties.template` (without real credentials) exists in the repository as a reference for new developers.

For production: pass properties via JVM args (`-Dsabre.clientId=...`) or Tomcat environment variables, never in the WAR.

---

## 10. Design Decisions

| Decision | Alternative | Reason |
|---|---|---|
| Two separate ports instead of one | One port with two methods | Search and Check have distinct contracts, caches and availability in Sabre (and any GDS). A single port hides that difference and forces implementations to cover operations they don't need. |
| `@JsonIgnoreProperties(ignoreUnknown = true)` on external DTOs | Strict mapping | Sabre's API may add fields in future versions without breaking the code. Defensive strategy. |
| `@JsonInclude(NON_NULL)` on request DTOs | Send all fields | Sabre rejects some null fields with an error; `NON_NULL` guarantees that only what is present is sent. |
| flightKey as `|`-delimited string | UUID + in-memory cache or table | Simple, no server-side state, directly serializable in `Booking.flightKey`. The `fromFlightKey()` parser reconstructs it without going to DB. |
| `synchronized` in `getValidToken()` | `AtomicReference` + double-checked locking | The token expires once a week — contention is practically zero. The simplest pattern is the correct one here. |
| `IntegrationException` extends `RuntimeException` | checked exception | Services should not be forced to declare `throws IntegrationException`. The `@ControllerAdvice` captures it globally. |
| Own `ObjectMapper` for Sabre (`sabreObjectMapper`) | Shared with Spring MVC | Avoids Sabre's date/null configuration affecting REST response serialization to the browser. |
| Credentials in `sabre.properties` + `.gitignore` | Hardcoded / System.getenv | External properties allow changing credentials without recompiling. `.gitignore` as a second barrier. |

---

## 11. Known cert Environment Limitations

- **Flight Search — origin**: only works for US origins (JFK confirmed: ~1,470 offers). ZRH as origin returns an empty array. Limitation of the cert dataset, not the code.
- **Flight Search — destination**: the BFM does not filter by destination in the request. Filtering is done client-side in `SearchServiceImpl` after receiving the complete response (~1,470 offers). With `destination=GVA` about 28 results are obtained, all real JFK→GVA.
- **Sandbox data**: the cert dataset is fixed (does not change in real time). Flights, dates and prices are representative but do not correspond to real operations.
- **LX 2020**: the reference flight for FlightCheck (ZRH→MAD) does not operate every day. Adjust the date if it returns `available: false`.
- **LX 2022**: was the reference flight in earlier sessions, no longer in the cert dataset.
- **PCC PC18**: the only PCC that returns fares in cert with DEVCENTER. Other PCCs (`DEVC`, `9999`) do not work.
- **`offerItemID` with `dummy-` prefix**: fare data is simulated — expected in cert.

---

*Next phase → docs/04-services.md: ProfileService, BookingService, SearchService with @Transactional and validations.*
