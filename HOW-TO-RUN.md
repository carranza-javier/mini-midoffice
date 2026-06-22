# JCarranza Mini-Midoffice — How to Run & Test

> **Platform note:** Commands are written for PowerShell on Windows. On Linux/macOS: replace
> backtick line continuations (`` ` ``) with backslash (`\`), use `curl` instead of `curl.exe`,
> and use forward slashes in paths.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Docker Desktop | Any recent | Build + run — no local Maven or Tomcat needed |
| PostgreSQL | 16 (Docker) | Container `mini-midoffice-db` on port 5432 |

Make sure PostgreSQL is running before starting the app:
```powershell
docker ps --filter "name=mini-midoffice-db"
```
If it is not running:
```powershell
docker start mini-midoffice-db
```

---

## 0. First-time database setup

Do this once per machine. Skip if `mini-midoffice-db` already exists and the schema has been applied.

### Create the PostgreSQL container

```powershell
docker run -d --name mini-midoffice-db -p 5432:5432 `
  -e POSTGRES_PASSWORD=miniumbrella -e POSTGRES_DB=miniumbrella `
  postgres:16
```

### Apply the schema

Wait a few seconds for Postgres to start, then apply the two migration files in order. Run each
command from the repo root:

```powershell
Get-Content mini-midoffice-persistence\src\main\resources\db\V1__create_schema.sql `
  | docker exec -i mini-midoffice-db psql -U postgres -d miniumbrella

Get-Content mini-midoffice-persistence\src\main\resources\db\V2__remove_searched_status.sql `
  | docker exec -i mini-midoffice-db psql -U postgres -d miniumbrella
```

`V1` creates the sequences, `traveller_profile`, and `booking` tables with all indexes and
constraints. `V2` updates the `chk_booking_status` check constraint — the reserve flow goes
directly to `RESERVED`, so `SEARCHED` is no longer a valid stored status.

### Load seed data (optional)

Loads 11 traveller profiles and 51 bookings across all 12 months, 12 destinations, and 3 providers.
Useful for exercising the Reports screen out of the box.

```powershell
Get-Content mini-midoffice-persistence\src\main\resources\db\seed-data.sql `
  | docker exec -i mini-midoffice-db psql -U postgres -d miniumbrella
```

---

## 1. Build

Uses a Maven 3.9 + JDK 17 Docker image. The `.m2` cache is shared with the host so subsequent builds are fast.

```powershell
docker run --rm `
  -v "C:/workspace/mini-midoffice:/workspace" `
  -v "$env:USERPROFILE/.m2:/root/.m2" `
  -w /workspace `
  maven:3.9.6-eclipse-temurin-17 `
  mvn clean package -DskipTests --no-transfer-progress
```

Output WAR: `mini-midoffice-web/target/mini-midoffice-web-1.0.0-SNAPSHOT.war`

---

## 2. Run

```powershell
docker run -d --name mini-midoffice-app -p 8080:8080 `
  -e JAVA_OPTS="-Ddb.url=jdbc:postgresql://host.docker.internal:5432/miniumbrella -Ddb.username=postgres -Ddb.password=miniumbrella" `
  -v "C:/workspace/mini-midoffice/mini-midoffice-web/target/mini-midoffice-web-1.0.0-SNAPSHOT.war:/usr/local/tomcat/webapps/ROOT.war:ro" `
  tomcat:10.1-jdk17
```

`host.docker.internal` resolves to the host machine — this is how the Tomcat container reaches the PostgreSQL container exposed on `localhost:5432`.

**Wait ~10 seconds** for Spring context to initialize, then verify:
```powershell
docker logs mini-midoffice-app --tail 5
# Should end with: Server startup in [NNNN] milliseconds
```

---

## 3. Test the API

Use `curl.exe` (ships with Windows 10/11) — not `curl`, which is a PowerShell alias for `Invoke-WebRequest`.

Base URL: `http://localhost:8080`

---

### 3.1 Profiles

#### Create a profile
```powershell
curl.exe -s -X POST http://localhost:8080/api/profiles `
  -H "Content-Type: application/json" `
  -d '{\"firstName\":\"Ana\",\"lastName\":\"Garcia\",\"email\":\"ana@corp.com\",\"company\":\"Iberia\"}'
```
Expected: `201 Created` with the created profile JSON including `"id": 1`.

#### List all profiles
```powershell
curl.exe -s http://localhost:8080/api/profiles
```

#### Search profiles by name / email / company
```powershell
curl.exe -s "http://localhost:8080/api/profiles?q=garcia"
```

#### Get profile by ID
```powershell
curl.exe -s http://localhost:8080/api/profiles/1
```

#### Update a profile
```powershell
curl.exe -s -X PUT http://localhost:8080/api/profiles/1 `
  -H "Content-Type: application/json" `
  -d '{\"company\":\"Iberia Group\",\"frequentFlyerNumber\":\"IB12345\"}'
```

---

### 3.2 Flight Search

> **Sandbox limitation:** Flight Search only returns complete results (flight key, times, airline)
> for **US origins**. European origins (ZRH, MAD, BCN…) return prices but no flight segment details —
> this is a Sabre certification dataset restriction, not a code bug.
> For the reserve flow, use a `flightKey` obtained from Flight Check (see section 3.3).

```powershell
curl.exe -s "http://localhost:8080/api/flights/search?origin=JFK&fromDate=2026-07-17&toDate=2026-07-20&passengerCount=1&currencyCode=USD"
```

Optional parameters:
| Parameter | Default | Example |
|-----------|---------|---------|
| `origin` | required | `JFK` |
| `destination` | — | (not required for BFM) |
| `fromDate` | — | `2026-07-17` |
| `toDate` | — | `2026-07-20` |
| `lengthOfStay` | 7 | `3` |
| `passengerCount` | 1 | `2` |
| `currencyCode` | EUR | `USD` |

---

### 3.3 Reserve a flight (Flight Check + persist RESERVED)

`POST /api/bookings` triggers a real-time **Flight Check** against Sabre and, if the flight is
available and the price is within tolerance, persists the booking directly with `status=RESERVED`.
No prior search step needed — just pass the `flightKey` from any search result.

```powershell
# Step 1 — search to get a flightKey (no travellerId needed)
curl.exe -s "http://localhost:8080/api/flights/search?origin=JFK&fromDate=2026-07-17&toDate=2026-07-20&currencyCode=USD"

# Step 2 — reserve (FlightCheck → persist RESERVED in one call)
# searchedPrice is your pre-check estimate; confirmedPrice is returned by FlightCheck and will
# typically differ in the cert sandbox — BFM and FlightCheck draw from different pricing datasets.
$body = '{"flightKey":"LX|23|JFK|GVA|2026-07-18|19:25|2026-07-19|09:15|Y","travellerId":1,"searchedPrice":500.00,"currencyCode":"USD"}'
Set-Content -Path "$env:TEMP\reserve.json" -Value $body -Encoding utf8
curl.exe -s -X POST http://localhost:8080/api/bookings `
  -H "Content-Type: application/json" `
  --data-binary "@$env:TEMP\reserve.json"
```

Expected: `201 Created` with the booking JSON (`status: "RESERVED"`, `confirmedPrice` from FlightCheck).

> **Flights confirmed working in FlightCheck cert (DEVCENTER):**
> - `LX|23|JFK|GVA|2026-07-18|19:25|2026-07-19|09:15|Y` — LX 23 JFK→GVA
>
> Most BFM results return "No such flight" from FlightCheck cert — this is a Sabre dataset
> limitation. Flights that fail return `400 Flight is no longer available`, not 504.

---

### 3.4 Bookings

#### List bookings for a traveller
```powershell
curl.exe -s "http://localhost:8080/api/bookings?travellerId=1"
```

#### Get booking by ID
```powershell
curl.exe -s http://localhost:8080/api/bookings/1
```

#### Cancel a booking
```powershell
curl.exe -s -X PUT http://localhost:8080/api/bookings/1/cancel
```
Expected: `200 OK`, booking status changes to `CANCELLED`.
Cancelling again returns `409 Conflict`.

---

## 4. Error responses

All errors follow this JSON shape:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Traveller not found: 999",
  "timestamp": "2026-07-17T10:00:00",
  "path": "/api/profiles/999"
}
```

| HTTP | Cause |
|------|-------|
| 400 | Business rule violation (not found, invalid input, price tolerance exceeded) |
| 409 | Conflict (duplicate email, booking already cancelled) |
| 502 | Sabre returned an HTTP error |
| 504 | Sabre timeout or network error |
| 500 | Unexpected server error |

---

## 5. Logs

```powershell
# Follow live
docker logs -f mini-midoffice-app

# Request log format: [REQUEST] METHOD /path → STATUS (N ms)
docker logs mini-midoffice-app 2>&1 | Select-String "\[REQUEST\]"
```

---

## 6. Stopping and restarting

```powershell
docker stop mini-midoffice-app
docker rm mini-midoffice-app
```

To restart after a rebuild, re-run the `docker run` command from section 2. The PostgreSQL
container (`mini-midoffice-db`) keeps its data between restarts — only `mini-midoffice-app`
needs to be removed and recreated.
