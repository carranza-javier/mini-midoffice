# JCarranza Mini-Midoffice — 2nd-Level Support Simulation

> Phase 9 of 9. Collection of common errors with real logs, root cause and troubleshooting exercises.
> Reproduces the diagnostic flow typical of 2nd-level support in a travel-agency mid-office system.

---

## How to Use This Document

Each scenario has:
1. **Symptom** — what the operator or client sees before opening any log.
2. **Logs** — the exact lines that would appear in `business.log`, `sabre-integration.log` or `errors.log`.
3. **Root cause** — what is really failing and why.
4. **Exercise** — four questions in order (observe → suspect → confirm → resolve). Answers are at the end of each block. Read them only after thinking through the answers.

The format of all log lines is from Phase 8:
```
{timestamp} [{thread}] {level} [{requestId}] {logger} - {message}
```

---

## Category S — Sabre Integration Errors

---

### S1 · Real Bug: `JsonMappingException` Masked as I/O Error → 504

**Scenario:** The agent tries to book a flight that exists in the BFM but not in the flightCheck dataset of the cert environment.

**Symptom:** The client receives a `504 Gateway Timeout`. The agent reports it as "Sabre not responding". The Sabre service actually responded in less than 700ms.

#### Logs BEFORE the fix (original bug state)

In this state the DTO had `List<String> errors` instead of `List<SabreError>`.

```
# sabre-integration.log
2026-06-15 11:42:17.088 [http-nio-8080-exec-3] INFO  [a3f7b241] c.m.i.sabre.SabreFlightCheckAdapter - Sabre flightCheck LX2020  ZRH/MAD on 2026-07-15
2026-06-15 11:42:17.091 [http-nio-8080-exec-3] INFO  [a3f7b241] c.m.i.sabre.client.SabreHttpClient - SABRE_REQUEST → POST /v1/offers/flightCheck
2026-06-15 11:42:17.734 [http-nio-8080-exec-3] ERROR [a3f7b241] c.m.i.sabre.client.SabreHttpClient - SABRE_IO_ERROR ← POST /v1/offers/flightCheck (643ms): Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)
com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)
 at [Source: (org.apache.hc.core5.http.impl.io.SocketInputStream); line: 1, column: 312]
 at com.fasterxml.jackson.databind.exc.MismatchedInputException.from(MismatchedInputException.java:59)
 at com.fasterxml.jackson.databind.DeserializationContext.reportInputMismatch(DeserializationContext.java:1741)
 at com.fasterxml.jackson.databind.deser.std.StringDeserializer.deserialize(StringDeserializer.java:64)
 at com.fasterxml.jackson.databind.deser.std.CollectionDeserializer._deserializeFromArray(CollectionDeserializer.java:355)
 at com.fasterxml.jackson.databind.deser.std.CollectionDeserializer.deserialize(CollectionDeserializer.java:244)
 at com.fasterxml.jackson.databind.deser.DefaultDeserializationContext.readRootValue(DefaultDeserializationContext.java:323)
 at com.fasterxml.jackson.databind.ObjectMapper._readValue(ObjectMapper.java:4736)
 at com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient.execute(SabreHttpClient.java:118)
 at com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient.post(SabreHttpClient.java:98)
 at com.jcarranza.minimidoffice.integration.sabre.SabreFlightCheckAdapter.check(SabreFlightCheckAdapter.java:51)
 at com.jcarranza.minimidoffice.service.BookingServiceImpl.reserve(BookingServiceImpl.java:97)
```

The `MismatchedInputException` is a subclass of `JsonMappingException`, which in turn extends `IOException`. The `catch (Exception e)` block in `SabreHttpClient.execute()` catches it and re-throws it as `IntegrationException("SABRE", "I/O error calling ...", e)` with `httpStatus == -1`. The `GlobalExceptionHandler` maps `httpStatus == -1` to HTTP 504.

#### Logs AFTER the fix

Sabre returned HTTP 200 correctly. The error is a business error (flight without data in cert), not a network failure.

```
# sabre-integration.log
2026-06-18 14:22:17.088 [http-nio-8080-exec-3] INFO  [b5c6d7e8] c.m.i.sabre.SabreFlightCheckAdapter - Sabre flightCheck LX2020  ZRH/MAD on 2026-07-15
2026-06-18 14:22:17.091 [http-nio-8080-exec-3] INFO  [b5c6d7e8] c.m.i.sabre.client.SabreHttpClient - SABRE_REQUEST → POST /v1/offers/flightCheck
2026-06-18 14:22:17.734 [http-nio-8080-exec-3] INFO  [b5c6d7e8] c.m.i.sabre.client.SabreHttpClient - SABRE_RESPONSE ← POST /v1/offers/flightCheck HTTP 200 (643ms)
2026-06-18 14:22:17.741 [http-nio-8080-exec-3] WARN  [b5c6d7e8] c.m.i.s.m.SabreFlightCheckMapper - SABRE_BUSINESS_ERROR category=Revalidation error type=Itinerary could not be priced. description=No such flight (LX 2020)
```

Result for the client: HTTP 400, `"Flight is no longer available: LX|2020|..."`. Correct — it is a business error, not a timeout.

**Root cause:** The `SabreFlightCheckResponse` DTO had `List<String> errors`. The Sabre API returns errors as structured JSON objects `{"category":"...", "type":"...", "description":"..."}`, not as strings. Jackson threw `MismatchedInputException` when trying to deserialize a JSON object into a String. That exception was caught in the `catch (Exception e)` block of `SabreHttpClient.execute()` and re-thrown as an I/O error, hiding the real origin.

**Fix applied:** Add inner class `SabreError` with the three fields and change the type of the `errors` field from `List<String>` to `List<SabreError>`.

---

**Troubleshooting Exercise:**

> **Q1.** The agent says "Sabre is not responding" and the client sees 504. What is the first thing you look at in the logs?

> **Q2.** In `sabre-integration.log` you see a `SABRE_IO_ERROR` with the message "Cannot deserialize value of type String from Object value". What does that message tell you? Is it a network problem?

> **Q3.** The duration in the log says `(643ms)`. Does that change your diagnosis? What does it mean that the call only took 643ms but the client receives 504?

> **Q4.** How do you confirm that Sabre responded HTTP 200 and there was no network problem?

---

<details>
<summary>Answers</summary>

**A1.** Search for the `requestId` of the problematic request in `sabre-integration.log`. If there is no `SABRE_REQUEST` entry, the error occurred before calling Sabre. If there is `SABRE_REQUEST` without `SABRE_RESPONSE`, it was indeed a network problem. If both are present, Sabre responded correctly and the error is downstream.

**A2.** "Cannot deserialize value of type String from Object value" is a Jackson deserialization error, **not a network error**. It means the JSON in Sabre's response contains a field that the DTO expects as `String` but in the actual response is a JSON object `{...}`. Problem in the DTO data model, not in connectivity.

**A3.** 643ms confirms that Sabre responded quickly. A real timeout would have taken ≥30,000ms (the configured `readTimeoutSeconds`). If it had been a network failure, we would see `SocketTimeoutException` or `ConnectException`, not `MismatchedInputException`. The 504 is a false positive — the I/O exception error code masks the real type of the problem.

**A4.** Before the fix: only indirectly, seeing that the duration is ~600ms (too fast for a timeout). After the fix, the log explicitly shows `SABRE_RESPONSE ← POST /v1/offers/flightCheck HTTP 200 (643ms)` before the `SABRE_BUSINESS_ERROR`. If you do not see that line, Sabre did not return 200.

</details>

---

### S2 · Network Timeout — Sabre Does Not Respond Within the Configured Limit

**Scenario:** The Sabre cert environment has high latency or is under maintenance. The client sees that the flight search "freezes" for 30 seconds before receiving 504.

**Symptom:** Multiple search requests start returning 504. Tomcat threads are blocked waiting for a response.

```
# business.log
2026-06-18 22:15:33.000 [http-nio-8080-exec-2] INFO  [7f3a9c12] com.jcarranza.minimidoffice.business - FLIGHT_SEARCH origin=JFK dest=GVA from=2026-07-15 to=2026-07-20 los=3d pax=1
# (FLIGHT_SEARCH_RESULT does not appear — the exception aborted execution before)

# sabre-integration.log
2026-06-18 22:15:33.001 [http-nio-8080-exec-2] INFO  [7f3a9c12] c.m.i.sabre.SabreFlightSearchAdapter - Sabre flightSearch origin=JFK from=2026-07-15 to=2026-07-20 los=3d
2026-06-18 22:15:33.012 [http-nio-8080-exec-2] INFO  [7f3a9c12] c.m.i.sabre.client.SabreHttpClient - SABRE_REQUEST → POST /v1/offers/flightSearch
2026-06-18 22:16:03.023 [http-nio-8080-exec-2] ERROR [7f3a9c12] c.m.i.sabre.client.SabreHttpClient - SABRE_IO_ERROR ← POST /v1/offers/flightSearch (30011ms): Read timed out
java.net.SocketTimeoutException: Read timed out
    at java.base/sun.nio.ch.NioSocketImpl.timedRead(NioSocketImpl.java:283)
    at java.base/sun.nio.ch.NioSocketImpl.implRead(NioSocketImpl.java:309)
    at org.apache.hc.core5.http.impl.io.SessionInputBufferImpl.fillBuffer(SessionInputBufferImpl.java:150)
    at org.apache.hc.core5.http.impl.io.DefaultBHttpClientConnection.receiveResponseHeader(...)
    at com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient.execute(SabreHttpClient.java:124)
    at com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient.post(SabreHttpClient.java:98)
    at com.jcarranza.minimidoffice.integration.sabre.SabreFlightSearchAdapter.search(SabreFlightSearchAdapter.java:49)
    at com.jcarranza.minimidoffice.service.SearchServiceImpl.search(SearchServiceImpl.java:38)
```

**Root cause:** `SabreHttpClient` configures `readTimeoutSeconds=30` via `sabre.properties`. Apache HttpClient 5 throws `SocketTimeoutException` when no byte arrives within that time. The `catch (Exception e)` block wraps it in `IntegrationException(httpStatus=-1)` → GlobalExceptionHandler → 504.

The diagnostic key is the duration: **exactly 30,011ms** — the configured timeout, not Sabre's actual response time.

**Troubleshooting Exercise:**

> **Q1.** In 5 minutes you receive 12 504 alerts on the same route `/api/flights/search`. What do you look for first in the logs?

> **Q2.** In `sabre-integration.log` you see the line `SABRE_IO_ERROR ← POST /v1/offers/flightSearch (30011ms): Read timed out`. Is this a code bug or an infrastructure problem? How do you distinguish it from scenario S1?

> **Q3.** In `business.log` you see `FLIGHT_SEARCH` but no `FLIGHT_SEARCH_RESULT` for the same `requestId`. What conclusion do you draw?

> **Q4.** What would you do to confirm that the problem is in Sabre and not in the server's local network?

---

<details>
<summary>Answers</summary>

**A1.** Search in `sabre-integration.log` for all `SABRE_IO_ERROR` in the last hour: `grep "SABRE_IO_ERROR" sabre-integration.log | tail -20`. If all durations are ~30,000ms, it is a systematic timeout problem, not isolated errors.

**A2.** External infrastructure problem (Sabre), not a code bug. The distinction with S1: here the duration is ~30,000ms (the configured timeout) and the error is `SocketTimeoutException`. In S1, the duration was ~640ms and the error was `MismatchedInputException`. In S1 Sabre responded; here it did not respond.

**A3.** That the exception thrown by `SabreFlightSearchAdapter.search()` interrupted execution before reaching `businessLog.info("FLIGHT_SEARCH_RESULT ...")`. If a `requestId` has `FLIGHT_SEARCH` without `FLIGHT_SEARCH_RESULT`, the search never completed successfully.

**A4.** Do a direct `curl` to the Sabre endpoint from the server (or from outside the WAR). `curl -v --max-time 10 https://api.cert.platform.sabre.com` — if it also times out, the problem is Sabre or the network toward Sabre, not the code. Alternatively, check the Sabre sandbox status page at developer.sabre.com.

</details>

---

### S3 · Expired OAuth2 Token or Regenerated Credentials → 401 → 502

**Scenario:** Someone regenerated the Sabre credentials at developer.sabre.com. The token cached in `SabreOAuthProvider` is still valid in memory, but Sabre rejects it with 401. Calls start returning 502.

```
# sabre-integration.log
2026-06-18 23:45:11.100 [http-nio-8080-exec-4] INFO  [a8b2d1f4] c.m.i.sabre.SabreFlightCheckAdapter - Sabre flightCheck LX23  JFK/GVA on 2026-07-18
2026-06-18 23:45:11.101 [http-nio-8080-exec-4] INFO  [a8b2d1f4] c.m.i.sabre.client.SabreHttpClient - SABRE_REQUEST → POST /v1/offers/flightCheck
2026-06-18 23:45:11.213 [http-nio-8080-exec-4] ERROR [a8b2d1f4] c.m.i.sabre.client.SabreHttpClient - SABRE_ERROR ← POST /v1/offers/flightCheck HTTP 401 (112ms) body={"message":"Full authentication is required to access this resource"}
```

Result: `IntegrationException(httpStatus=401)`. The GlobalExceptionHandler maps any `httpStatus != -1` → 502 Bad Gateway.

**The cached token is not invalidated automatically** — `SabreOAuthProvider.invalidate()` exists but is only called manually. The problem persists until Tomcat is restarted or `invalidate()` is called.

**Root cause:** The OAuth2 token has a 7-day TTL but can become invalid before that if credentials are rotated. `SabreOAuthProvider` caches the token until `now() > expiryTime`. Since the server did not receive the new token, the old token continues to be sent and Sabre rejects it.

This also occurs if the credentials in `sabre.properties` are incorrect from the start: `SabreOAuthProvider` logs `Sabre token expired or missing — refreshing`, makes the auth POST, Sabre returns 400, `IntegrationException(httpStatus=400)` → 502 on all calls.

**Troubleshooting Exercise:**

> **Q1.** The client reports 502 error on all searches and bookings simultaneously. What first grep would you do?

> **Q2.** You see `SABRE_ERROR ← POST /v1/offers/flightCheck HTTP 401`. What is the difference between a 401 and a 403 in this context? What causes each?

> **Q3.** How do you fix the problem without restarting Tomcat?

> **Q4.** How would you know if the auth problem started with the token call (auth/token returns an error) or with an API call (flightCheck returns 401)?

---

<details>
<summary>Answers</summary>

**A1.** `grep "SABRE_ERROR" sabre-integration.log | tail -20`. If all errors are HTTP 401 or HTTP 400 on the token endpoint, it is a global authentication problem, not an isolated flight failure.

**A2.** 401 = not authenticated (invalid or missing token). 403 = authenticated but no permission for that resource (incorrect PCC, expired subscription). In Sabre cert, 401 usually indicates an invalid token or incorrect credentials. 403 may indicate that PCC `PC18` does not have access to that specific endpoint.

**A3.** Call `SabreOAuthProvider.invalidate()`. This resets the cached token, forcing a refresh on the next call. Without restarting Tomcat: if the WAR is deployed, a private admin endpoint could be exposed, or simply wait for the cached token to expire (`expiryTime`). The correct fix is to update `sabre.properties` with the new credentials and restart Tomcat to load them.

**A4.** In `sabre-integration.log`: if the problem starts with `SABRE_ERROR ← POST https://...api.cert.../v2/auth/token HTTP 400`, auth fails from the start — incorrect credentials. If auth has `SABRE_RESPONSE HTTP 200` but then API calls give 401, the token was obtained but is being rejected — credentials rotated after obtaining the token.

</details>

---

### S4 · European Origin Without Data in cert Cache — Empty Results Without Error

**Scenario:** The team configures the application in cert to demo searches from Zurich. Results are always zero. There are no errors in the logs.

```
# business.log
2026-06-18 10:30:00.000 [http-nio-8080-exec-1] INFO  [3b9f1e77] com.jcarranza.minimidoffice.business - FLIGHT_SEARCH origin=ZRH dest=MAD from=2026-07-15 to=2026-07-20 los=3d pax=1
2026-06-18 10:30:02.123 [http-nio-8080-exec-1] INFO  [3b9f1e77] com.jcarranza.minimidoffice.business - FLIGHT_SEARCH_RESULT origin=ZRH dest=MAD results=0

# sabre-integration.log
2026-06-18 10:30:00.001 [http-nio-8080-exec-1] INFO  [3b9f1e77] c.m.i.sabre.SabreFlightSearchAdapter - Sabre flightSearch origin=ZRH from=2026-07-15 to=2026-07-20 los=3d
2026-06-18 10:30:00.012 [http-nio-8080-exec-1] INFO  [7f3a9c12] c.m.i.sabre.client.SabreHttpClient - SABRE_REQUEST → POST /v1/offers/flightSearch
2026-06-18 10:30:02.113 [http-nio-8080-exec-1] INFO  [3b9f1e77] c.m.i.sabre.client.SabreHttpClient - SABRE_RESPONSE ← POST /v1/offers/flightSearch HTTP 200 (2112ms)
2026-06-18 10:30:02.121 [http-nio-8080-exec-1] INFO  [3b9f1e77] c.m.i.sabre.SabreFlightSearchAdapter - Sabre flightSearch → 0 options returned
```

No errors. Sabre returns HTTP 200 with an empty `offers: []` array. The code works correctly.

**Root cause:** Limitation of the Sabre certification environment with DEVCENTER credentials. The BFM dataset (`/v1/offers/flightSearch`) only has cached data for US origins in cert (JFK confirmed: ~1,470 offers). European origins like ZRH return an empty array. It is not a code bug.

**Troubleshooting Exercise:**

> **Q1.** The business team escalates an incident: "ZRH search has not been working for weeks." What do you do before reviewing the code?

> **Q2.** The logs show `results=0` with no errors. What grep command would you use to see if ZRH ever returned results in the last 7 days?

> **Q3.** How do you distinguish "code bug" from "cert environment limitation" in this case?

> **Q4.** What would you look for to verify if JFK as origin works?

---

<details>
<summary>Answers</summary>

**A1.** Verify whether the source of the problem is the cert environment vs production. Sabre DEVCENTER cert has limited datasets — this is documented in `docs/03-sabre-integration.md`. If the real system in production works with ZRH, the problem is in the test environment, not the code.

**A2.** `grep "FLIGHT_SEARCH_RESULT.*origin=ZRH" business.log` to see all searches from ZRH. If all have `results=0` for weeks and JFK has `results>0`, it confirms it is an environment limitation, not a regression.

**A3.** If the code has a bug, JFK would also give 0. If ZRH gives 0 but JFK gives 616+, the code works — it is the environment dataset. Also: the `SABRE_RESPONSE HTTP 200` log indicates that Sabre responded correctly. If there were a mapping bug, we would see exceptions in `SabreFlightSearchMapper`.

**A4.** `grep "FLIGHT_SEARCH_RESULT.*origin=JFK" business.log | tail -5`. If you see `results=616` (or similar), JFK works and the problem is specific to ZRH in cert.

</details>

---

## Category D — Database Errors

---

### D1 · Constraint Violation — Duplicate Email → 409

**Scenario:** The agent tries to create a profile with an email that already exists in the DB.

**Symptom:** HTTP 409 Conflict. The client sees "The operation conflicts with existing data".

```
# errors.log — EMPTY (this is an expected error, not a bug)

# Console / root logger (GlobalExceptionHandler, WARN):
2026-06-18 14:22:33.456 [http-nio-8080-exec-8] WARN  [e2b4a891] c.m.w.advice.GlobalExceptionHandler - Data integrity violation on POST /api/profiles: ERROR: duplicate key value violates unique constraint "uk_traveller_profile_email"
  Detail: Key (email)=(john@acme.com) already exists.
```

The `GlobalExceptionHandler.handleDataIntegrity()` catches `DataIntegrityViolationException` (from Spring) and maps it to 409. It does not go to `errors.log` because the handler treats it as a known and expected case.

**Root cause:** The `email` column in `traveller_profile` has a `UNIQUE` constraint. If you try to insert an email that already exists, PostgreSQL throws `PSQLException`, Hibernate wraps it in `ConstraintViolationException`, Spring ORM translates it to `DataIntegrityViolationException`.

There are two lines of defense: `ProfileServiceImpl.create()` calls `profileDao.existsByEmail()` before the insert (first validation, at application level). The DB constraint is the second and definitive one: even if two threads simultaneously pass the application validation (race condition), only one can insert. The 409 response in that case comes from the constraint, not from the application validation.

**Troubleshooting Exercise:**

> **Q1.** The agent says "the system gives an error when creating a client." How do you determine if it is a 409 for a duplicate email or a 500 without more information?

> **Q2.** If the application validation in `ProfileServiceImpl` already checks whether the email exists, why also keep the UNIQUE constraint in DB? What scenario does it cover that the application validation does not?

> **Q3.** Where would you see the `PSQLException` stack trace if you need it? Why is it not in `errors.log`?

---

<details>
<summary>Answers</summary>

**A1.** If the response body says `"status":409,"error":"Conflict"`, it is a known conflict (duplicate email or similar). If it says `"status":500`, search in `errors.log` for the `requestId` of that request. 409s do not go to `errors.log` — they are expected errors, not bugs.

**A2.** Application validation (`existsByEmail`) does a SELECT before the INSERT in the same transaction. In a concurrency scenario, two threads can read "does not exist" simultaneously, both pass the validation, and both attempt the INSERT — only one succeeds. The UNIQUE constraint in DB guarantees integrity even under concurrency. The application validation only improves the experience (clearer error message than PostgreSQL's).

**A3.** The full `PSQLException` stack trace is in the root logger logs (console, WARN level) or can be activated in Hibernate with `logging.level.org.hibernate=DEBUG`. It is not in `errors.log` because that file only receives messages from the `com.jcarranza.minimidoffice.errors` logger, which `GlobalExceptionHandler` only writes for unhandled `Exception` exceptions (500), not for `DataIntegrityViolationException` (409).

</details>

---

### D2 · Connection Pool Exhausted — HikariCP Out of Connections → 500

**Scenario:** Under high load (or due to a connection leak), all pool connections are occupied and a new request cannot obtain one.

**Symptom:** Requests that normally take <100ms start taking exactly 30,000ms and then return 500.

```
# errors.log
2026-06-18 15:47:22.001 [http-nio-8080-exec-1] ERROR [b3c5d7e9] com.jcarranza.minimidoffice.errors - UNHANDLED_EXCEPTION on POST /api/bookings: Unable to acquire JDBC Connection
org.springframework.dao.DataAccessResourceFailureException: Unable to acquire JDBC Connection; nested exception is org.hibernate.exception.JDBCConnectionException: Unable to acquire JDBC Connection
    at org.springframework.orm.hibernate5.SessionFactoryUtils.convertHibernateAccessException(SessionFactoryUtils.java:235)
    at org.springframework.orm.hibernate5.HibernateTransactionManager.convertHibernateAccessException(HibernateTransactionManager.java:733)
    at org.springframework.orm.hibernate5.HibernateTransactionManager.doBegin(HibernateTransactionManager.java:561)
    at org.springframework.transaction.support.AbstractPlatformTransactionManager.startTransaction(AbstractPlatformTransactionManager.java:400)
    at com.jcarranza.minimidoffice.service.BookingServiceImpl.lambda$reserve$0(BookingServiceImpl.java:90)
    at org.springframework.transaction.support.TransactionTemplate.execute(TransactionTemplate.java:140)
    at com.jcarranza.minimidoffice.service.BookingServiceImpl.reserve(BookingServiceImpl.java:88)
Caused by: org.hibernate.exception.JDBCConnectionException: Unable to acquire JDBC Connection
    at org.hibernate.exception.internal.SQLExceptionTypeDelegate.convert(SQLExceptionTypeDelegate.java:65)
    at org.hibernate.exception.internal.StandardSQLExceptionConverter.convert(StandardSQLExceptionConverter.java:37)
Caused by: java.sql.SQLTimeoutException: Timeout of 30000ms exceeded waiting for connection from pool 'HikariPool-1'
    at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:213)
    at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:162)
    at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:128)
    at org.springframework.orm.hibernate5.LocalSessionFactoryBean$HibernateTransactionInterceptor...
```

```
# business.log — The request started but never completed
2026-06-18 15:47:22.000 [http-nio-8080-exec-1] INFO  [b3c5d7e9] com.jcarranza.minimidoffice.business - FLIGHT_CHECK_START travellerId=7 flightKey=LX|23|JFK|GVA|2026-07-18|19:25|2026-07-19|09:15|Y
# (BOOKING_RESERVED never appears)
```

**Root cause:** `PersistenceConfig` configures HikariCP with `maximumPoolSize=10`. If there are 10 active simultaneous transactions and an eleventh request arrives, it waits up to 30,000ms (`connectionTimeout=30000`). If in that time no connection is released, it throws `SQLTimeoutException`.

Common causes: (a) real high load, (b) very slow FlightCheck HTTP leaving transactions open longer than expected, (c) connection leak (a transaction never commits/rolls back).

In this system, `BookingServiceImpl.reserve()` uses `TransactionTemplate` precisely to release the connection before the HTTP call to Sabre. If that pattern had been implemented incorrectly (keeping the TX open during the FlightCheck), 10 concurrent requests with a slow Sabre would exhaust the pool.

**Troubleshooting Exercise:**

> **Q1.** You see 500s starting at 15:47 with `Timeout of 30000ms exceeded waiting for connection`. At 16:00 they resolve on their own. What hypotheses do you have?

> **Q2.** How do you determine if it is a connection leak or simply high load?

> **Q3.** The pool has 10 connections. How many concurrent FlightCheck requests can cause exhaustion without any bug?

> **Q4.** What HikariCP metrics would you check to confirm the exhaustion?

---

<details>
<summary>Answers</summary>

**A1.** Transient high load that resolved itself. If it had been a leak, the problem would not have resolved without restarting. The spontaneous resolution suggests that connections are being released (no leak), but the request rate momentarily exceeded the pool capacity.

**A2.** With leak: the pool exhausts progressively and does not recover. The number of available connections decreases monotonically. Without leak / high load: connections are exhausted and recovered in cycles. In the logs, a leak would show as errors that start subtle and become permanent; high load produces bursts of errors.

**A3.** With `readTimeoutSeconds=30` for Sabre and without the pattern of releasing TX before the HTTP call: a slow FlightCheck call holds a pool connection for ~30s. With 10 connections = 10 concurrent requests in FlightCheck = pool exhausted. The `readTemplate → FlightCheck (no TX) → writeTemplate` pattern in `BookingServiceImpl` resolves exactly this — the connection is released before calling Sabre.

**A4.** HikariCP exposes JMX metrics: `HikariPool-1.ActiveConnections`, `HikariPool-1.PendingConnections`, `HikariPool-1.IdleConnections`. At the moment of exhaustion: `ActiveConnections=10`, `PendingConnections>0`, `IdleConnections=0`. If available via JMX/Actuator, they confirm the diagnosis without touching the code.

</details>

---

### D3 · Deadlock Between Concurrent Transactions → 500

**Scenario:** Two concurrent requests try to cancel bookings that reference each other (or access the same rows in reverse order). PostgreSQL detects the cycle and aborts one of the transactions.

**Symptom:** Cancellation requests occasionally return 500. The error is not reproducible deterministically — it occurs under concurrency.

```
# errors.log
2026-06-18 16:33:55.789 [http-nio-8080-exec-6] ERROR [d1e2f3a4] com.jcarranza.minimidoffice.errors - UNHANDLED_EXCEPTION on DELETE /api/bookings/42: could not execute statement
org.springframework.dao.CannotAcquireLockException: could not execute statement; SQL [n/a]; nested exception is org.hibernate.exception.LockAcquisitionException: could not execute statement
    at org.springframework.orm.hibernate5.SessionFactoryUtils.convertHibernateAccessException(SessionFactoryUtils.java:219)
    at org.springframework.orm.hibernate5.HibernateTransactionManager.convertHibernateAccessException(...)
    at com.jcarranza.minimidoffice.service.BookingServiceImpl.cancel(BookingServiceImpl.java:141)
    at com.jcarranza.minimidoffice.web.controller.BookingController.cancel(BookingController.java:57)
Caused by: org.hibernate.exception.LockAcquisitionException: could not execute statement
    at org.hibernate.exception.internal.SQLExceptionTypeDelegate.convert(SQLExceptionTypeDelegate.java:59)
Caused by: org.postgresql.util.PSQLException: ERROR: deadlock detected
  Detail: Process 23451 waits for ShareLock on transaction 1784; blocked by process 23452.
          Process 23452 waits for ShareLock on transaction 1783; blocked by process 23451.
  Hint: See server log for query details.
    at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2553)
    at org.postgresql.jdbc.PgStatement.executeInternal(PgStatement.java:462)
    at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:384)
```

```
# business.log — Only one of the two concurrent requests succeeded
2026-06-18 16:33:55.500 [http-nio-8080-exec-5] INFO  [c5d6e7f8] com.jcarranza.minimidoffice.business - BOOKING_CANCELLED bookingId=42 travellerId=3
# The request with requestId=d1e2f3a4 has no BOOKING_CANCELLED — it aborted
```

**Root cause:** PostgreSQL detects a wait cycle between two transactions (A waits for B, B waits for A). It aborts the transaction with less accumulated work and throws `PSQLException: deadlock detected`. Hibernate translates it to `LockAcquisitionException`, Spring ORM to `CannotAcquireLockException`. There is no specific handler in `GlobalExceptionHandler` — it falls through to the `Exception` catch → 500 → `errors.log`.

In this system, a deadlock can occur if two concurrent requests try to update the same rows in different order (e.g., update booking 42 then 43 in one thread, 43 then 42 in the other).

**Troubleshooting Exercise:**

> **Q1.** A 500 from a deadlock is hard to reproduce. What pattern in the logs would confirm that it is really a deadlock and not another cause of 500?

> **Q2.** What is the difference between `CannotAcquireLockException` (deadlock) and `QueryTimeoutException` (lock timeout)? How do you distinguish them in the stack trace?

> **Q3.** To mitigate deadlocks in `BookingServiceImpl.cancel()`, what strategies would you apply?

---

<details>
<summary>Answers</summary>

**A1.** The message `PSQLException: ERROR: deadlock detected` together with `Detail: Process X waits for ShareLock on transaction Y; blocked by process Z. Process Z waits for ShareLock on transaction Y; blocked by process X.` is unambiguous. The `Detail` describes the exact cycle. If the 500 is from another cause, the stack trace will be different.

**A2.** `CannotAcquireLockException` with `deadlock detected` = PostgreSQL detected a cycle and aborted the TX immediately. `QueryTimeoutException` = the TX waited the configured lock timeout without success. In the stack: deadlock has `PSQLException: ERROR: deadlock detected`; timeout has `PSQLException: ERROR: canceling statement due to statement timeout` or `LockAcquisitionException: could not obtain lock`.

**A3.** (a) Ensure all transactions access rows in the same order (e.g., always by ascending `bookingId`). (b) Use `SELECT ... FOR UPDATE SKIP LOCKED` to avoid waiting. (c) Add automatic retry: catch `CannotAcquireLockException` in the service and retry N times with backoff — Spring has `@Retryable` from `spring-retry`. (d) Reduce transaction time to reduce the collision window.

</details>

---

## Category V — Validation Errors

---

### V1 · Required Field Missing — `flightKey` Null → 400

**Scenario:** The frontend sends the booking request without the `flightKey` field (bug in the JS that builds the payload).

**Symptom:** HTTP 400 Bad Request with a clear message.

```
# Console / root logger (GlobalExceptionHandler, WARN):
2026-06-18 17:05:11.234 [http-nio-8080-exec-9] WARN  [f9e8d7c6] c.m.w.advice.GlobalExceptionHandler - Business rule violation [400] on POST /api/bookings: flightKey is required

# business.log — EMPTY (validation fails before any business event)
# sabre-integration.log — EMPTY (Sabre is never called)
# errors.log — EMPTY (400 is an expected error, not a bug)
```

Response body to the client:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "flightKey is required",
  "path": "/api/bookings"
}
```

**Root cause:** `BookingServiceImpl.validateReserveRequest()` checks that `flightKey` is not null or blank. If missing, it throws `BusinessException("flightKey is required")`. The `GlobalExceptionHandler.handleBusiness()` maps it to 400 and logs it as WARN (not ERROR).

**Troubleshooting Exercise:**

> **Q1.** The agent reports "I cannot book, it gives me error 400." How do you know if it is a server bug or a problem with the data the client is sending?

> **Q2.** In the log you see `WARN: Business rule violation [400] on POST /api/bookings: flightKey is required`. There is nothing in `business.log` or `errors.log`. What conclusion do you draw about the system state?

> **Q3.** If the problem were that `FlightCheckRequest.fromFlightKey()` fails to parse the `flightKey` format (some parts of the pipe-separated string are missing), what error would you see?

---

<details>
<summary>Answers</summary>

**A1.** Looking at the 400 body: if it says "flightKey is required", the client is not sending that field. If it says "invalid format", the field arrives but is malformed. To confirm, the agent can reproduce with `curl -X POST /api/bookings -d '{"travellerId":1}'` — if it also gives 400 with the same message, it is the client's payload. To see the exact payload, activate `log.debug` in the request logging filter.

**A2.** The system is in perfect state. The validation worked as designed — it detected the error before touching DB or calling Sabre. The fact that there is nothing in `errors.log` confirms there is no unhandled exception. It is a client error, not a server error.

**A3.** `BusinessException: Invalid flightKey format: expected 9 fields separated by '|'` (thrown in `FlightCheckRequest.fromFlightKey()`). It would appear in the GlobalExceptionHandler WARN as `Business rule violation [400]`, not in `errors.log`. If the parsing threw an unhandled exception (e.g., `ArrayIndexOutOfBoundsException`), that would go to `errors.log` as 500 — but the code has explicit validation of the number of parts.

</details>

---

### V2 · Malformed JSON in Body — `HttpMessageNotReadableException` → 400

**Scenario:** An API client sends a body that is not valid JSON (unclosed quotes, trailing comma, etc.).

**Symptom:** HTTP 400. The error message includes the Jackson parsing error detail.

```
# Console / root logger (GlobalExceptionHandler, DEBUG):
2026-06-18 17:12:45.678 [http-nio-8080-exec-2] DEBUG [a1b2c3d4] c.m.w.advice.GlobalExceptionHandler - Bad input on POST /api/bookings: JSON parse error: Unexpected character ('}' (code 125)): was expecting double-quote to start field name; nested exception is com.fasterxml.jackson.core.JsonParseException: Unexpected character ('}' (code 125)): was expecting double-quote to start field name
 at [Source: (org.springframework.util.StreamUtils$NonClosingInputStream); line: 1, column: 47]

# business.log — EMPTY
# sabre-integration.log — EMPTY
# errors.log — EMPTY
```

**Root cause:** The `DispatcherServlet` tries to deserialize the body with Jackson before reaching the controller. `MappingJackson2HttpMessageConverter` throws `HttpMessageNotReadableException`. The specific handler `handleBadInput()` catches it and maps it to 400. DEBUG level because these are very frequent client errors that do not require server attention.

**Troubleshooting Exercise:**

> **Q1.** Why is this error at DEBUG and not at WARN like case V1?

> **Q2.** An external client integrates with the API and starts receiving 400s with "JSON parse error". The client claims their JSON is valid. How do you investigate without access to their system?

---

<details>
<summary>Answers</summary>

**A1.** JSON parsing errors are extremely frequent in public APIs (bots, misconfigured clients, incorrect manual tests). If they were at WARN, `sabre-integration.log` or the root logger would fill with noise that would obscure real problems. DEBUG allows activating them only when actively investigating a problem.

**A2.** Ask the client to send the exact body with `curl -v -X POST /api/bookings -H "Content-Type: application/json" -d '@payload.json'` and capture the complete response. Jackson's error usually indicates which line and column the problem is on (`line: 1, column: 47`). Also verify the `Content-Type` header — if the client sends `text/plain` instead of `application/json`, Spring will not even attempt JSON deserialization and will return 415.

</details>

---

### V3 · Incorrect Date Format in Query Param → 400

**Scenario:** The agent uses the API directly with curl and sends `fromDate=15-07-2026` instead of the expected format `2026-07-15`.

**Symptom:** HTTP 400 with a message about type conversion.

```
# Console / root logger (GlobalExceptionHandler, DEBUG):
2026-06-18 17:20:33.111 [http-nio-8080-exec-3] DEBUG [b2c3d4e5] c.m.w.advice.GlobalExceptionHandler - Bad input on GET /api/flights/search: Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate' for value: '15-07-2026'; nested exception is org.springframework.core.convert.ConversionFailedException: Failed to convert from type [java.lang.String] to type [@org.springframework.format.annotation.DateTimeFormat java.time.LocalDate] for value '15-07-2026'

# business.log — EMPTY (conversion fails in Spring MVC layer, before the service)
```

**Root cause:** `FlightController.search()` declares `@RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate fromDate`. Spring tries to convert the String `"15-07-2026"` to `LocalDate` with ISO 8601 format (`yyyy-MM-dd`). The received format is `dd-MM-yyyy` — incompatible. Spring throws `MethodArgumentTypeMismatchException`, which the handler `handleBadInput()` catches and maps to 400.

**Troubleshooting Exercise:**

> **Q1.** How do you distinguish in the log between "missing parameter" and "parameter with incorrect format"?

> **Q2.** If you wanted the API to accept both `2026-07-15` and `15-07-2026`, where would you make the change and what considerations would you have?

---

<details>
<summary>Answers</summary>

**A1.** Missing parameter: `MissingServletRequestParameterException` with message "Required request parameter 'fromDate' for method parameter type LocalDate is not present". Incorrect format: `MethodArgumentTypeMismatchException` with "Failed to convert value of type 'String'... for value '15-07-2026'". In both cases they appear in DEBUG from the GlobalExceptionHandler.

**A2.** The change would go in `FlightController`: change `@DateTimeFormat(iso = ISO.DATE)` to a `@DateTimeFormat` with a custom pattern, or change the param type to `String` and parse manually with fallback. The main consideration: accepting multiple formats introduces ambiguity (is `06-07-2026` July 6th or June 7th?). The golden rule of REST APIs is to enforce a canonical format (ISO 8601) and document it, not try to guess the client's format.

</details>

---

### V4 · Confirmed Price Exceeds Tolerance → 400

**Scenario:** The price confirmed by Sabre in the FlightCheck is much higher than the price the user saw in the search. The tolerance configured in `booking.priceTolerance` is not sufficient.

**Symptom:** HTTP 400. The booking is not created. The FlightCheck was done correctly.

```
# business.log
2026-06-18 17:35:22.456 [http-nio-8080-exec-4] INFO  [c3d4e5f6] com.jcarranza.minimidoffice.business - FLIGHT_CHECK_START travellerId=5 flightKey=LX|23|JFK|GVA|2026-07-18|19:25|2026-07-19|09:15|Y
# (BOOKING_RESERVED never appears)

# sabre-integration.log
2026-06-18 17:35:22.460 [http-nio-8080-exec-4] INFO  [c3d4e5f6] c.m.i.sabre.SabreFlightCheckAdapter - Sabre flightCheck LX23  JFK/GVA on 2026-07-18
2026-06-18 17:35:22.461 [http-nio-8080-exec-4] INFO  [c3d4e5f6] c.m.i.sabre.client.SabreHttpClient - SABRE_REQUEST → POST /v1/offers/flightCheck
2026-06-18 17:35:23.098 [http-nio-8080-exec-4] INFO  [c3d4e5f6] c.m.i.sabre.client.SabreHttpClient - SABRE_RESPONSE ← POST /v1/offers/flightCheck HTTP 200 (637ms)
2026-06-18 17:35:23.105 [http-nio-8080-exec-4] INFO  [c3d4e5f6] c.m.i.sabre.SabreFlightCheckAdapter - Sabre flightCheck available=true price=3834.22 EUR

# Console / root logger (GlobalExceptionHandler, WARN):
2026-06-18 17:35:23.108 [http-nio-8080-exec-4] WARN  [c3d4e5f6] c.m.w.advice.GlobalExceptionHandler - Business rule violation [400] on POST /api/bookings: Price exceeds tolerance. Searched: 100.00 EUR, confirmed: 3834.22 EUR
```

**Root cause:** `BookingServiceImpl.reserve()` validates that the price confirmed by Sabre does not exceed `searchedPrice * PRICE_TOLERANCE`. With tolerance `1.05` (5%), if the user saw 100 EUR in the search but FlightCheck confirms 3834 EUR, the difference exceeds the threshold and throws `BusinessException`. This protects the user from surprise price confirmations.

In the cert environment, the BFM and FlightCheck datasets are independent — prices usually differ significantly. That is why `booking.priceTolerance=100.0` (10,000% tolerance) is used in cert to absorb this artificial sandbox difference.

The diagnostic trick: `BOOKING_RESERVED` does not appear in `business.log` even though `FlightCheck` was successful (`available=true`). The exception occurred in the price validation, after the check and before the INSERT.

**Troubleshooting Exercise:**

> **Q1.** An agent reports: "Sabre says the flight is available but the system won't let me book." The logs show `flightCheck available=true` but no `BOOKING_RESERVED`. What do you suspect?

> **Q2.** How do you confirm that the problem is the price tolerance and not another type of validation?

> **Q3.** The team wants to temporarily raise the tolerance in production to avoid rejections. What file do you touch? Does it require a redeploy?

---

<details>
<summary>Answers</summary>

**A1.** The price validation. The FlightCheck passed correctly (available=true, price obtained), but the subsequent verification in `BookingServiceImpl` detected that the confirmed price exceeds what the user saw. Between `FLIGHT_CHECK_START` and the absence of `BOOKING_RESERVED`, something failed: could be price tolerance or (less likely) that the traveller ceased to exist in DB between phase 1 and phase 4.

**A2.** The 400 message confirms it directly: "Price exceeds tolerance. Searched: X, confirmed: Y". If there is no access to the console log, it is enough to repeat the request and read the 400 response body — it includes the `BusinessException` message.

**A3.** The value is in `sabre.properties` (or in the JVM arg `-Dbooking.priceTolerance=1.10`). If passed as a JVM property, it is enough to restart Tomcat with the new value — no recompile or WAR redeploy needed. If it is in `sabre.properties`, edit the file and restart. This system has no hot-reload of properties.

</details>

---

## Quick Reference — Diagnostic Commands

```bash
# Trace a complete request by requestId
grep "c3d4e5f6" /usr/local/tomcat/logs/mini-midoffice/*.log

# See all Sabre errors from the last few hours
grep "SABRE_ERROR\|SABRE_IO_ERROR\|SABRE_BUSINESS_ERROR" sabre-integration.log | tail -30

# Search specifically for timeouts (duration ≥ 30s)
grep "SABRE_IO_ERROR.*30[0-9][0-9][0-9][0-9]ms" sabre-integration.log

# Failed bookings (have FLIGHT_CHECK_START but no BOOKING_RESERVED with the same requestId)
grep "FLIGHT_CHECK_START" business.log | awk '{print $4}' | while read rid; do
  grep -q "BOOKING_RESERVED" business.log <<< "$(grep "$rid" business.log)" || echo "FAILED: $rid"
done

# See all 500s today
grep "$(date +%Y-%m-%d)" errors.log | grep "UNHANDLED_EXCEPTION"

# Booking history for a traveller
grep "travellerId=42" business.log | grep -E "BOOKING_RESERVED|BOOKING_CANCELLED"

# Detect deadlock pattern
grep "deadlock detected" errors.log | wc -l
```

---

*Phase 9 completed — JCarranza Mini-Midoffice build finished.*
