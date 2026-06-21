# Sabre REST API — Integration Guide

Everything learned and verified as of June 2026 through live API calls against the cert environment.

---

## Credentials

Sabre provides two sets of credentials for the Developer Center (DEVCENTER) sandbox.
Both are active simultaneously and work interchangeably against the cert environment.

| Label | Client ID | Secret |
|---|---|---|
| guide-creds | `V1:p30i3v2zaxukjwfd:DEVCENTER:EXT` | `NGF9ry6j` |
| demo-creds | `V1:nfuoonfuxsvhti3y:DEVCENTER:EXT` | `Fv7oMTh1` |

> **Important:** DEVCENTER credentials reset periodically (weeks/months).
> If you get `"Wrong clientID or clientSecret"`, regenerate them at
> https://developer.sabre.com/my-account/applications

---

## Environments

| Environment | Base URL | Works with DEVCENTER? |
|---|---|---|
| **Certification (cert)** | `https://api.cert.platform.sabre.com` | Yes |
| Production | `https://api.platform.sabre.com` | **No** — returns 401 |

**Always use the cert URL with DEVCENTER credentials.**

---

## Authentication — Double Base64

Sabre uses an unusual double-Base64 encoding for the `Authorization` header.
The official docs confirm this (decoding their own example proves it).

```
Authorization: Basic base64( base64(CLIENT_ID) + ":" + base64(SECRET) )
```

### Python

```python
import base64

client_id = "V1:nfuoonfuxsvhti3y:DEVCENTER:EXT"
secret    = "Fv7oMTh1"

b64_id     = base64.b64encode(client_id.encode()).decode()
b64_secret = base64.b64encode(secret.encode()).decode()
credential = base64.b64encode(f"{b64_id}:{b64_secret}".encode()).decode()

headers = {
    "Authorization": f"Basic {credential}",
    "Content-Type": "application/x-www-form-urlencoded",
}
```

### Bash

```bash
B64_USER=$(printf '%s' "V1:nfuoonfuxsvhti3y:DEVCENTER:EXT" | base64 -w 0)
B64_PASS=$(printf '%s' "Fv7oMTh1" | base64 -w 0)
CREDENTIAL=$(printf '%s:%s' "$B64_USER" "$B64_PASS" | base64 -w 0)
```

### Error diagnosis

| Error message | Cause |
|---|---|
| `"Credentials are missing or the syntax is not correct"` | Wrong encoding (single Base64) |
| `"Wrong clientID or clientSecret"` | Correct encoding, but credentials expired or invalid |

---

## Step 1 — Get an OAuth Token

**Endpoint:** `POST /v2/auth/token`

```bash
curl -X POST "https://api.cert.platform.sabre.com/v2/auth/token" \
  -H "Authorization: Basic $CREDENTIAL" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials"
```

**Response (HTTP 200):**

```json
{
  "access_token": "T1RLAQ...",
  "token_type": "bearer",
  "expires_in": 604800
}
```

Token lifetime: **7 days**. Reuse it — no need to re-authenticate on every request.

---

## API Comparison — Which works for which route

Verified in the cert environment with DEVCENTER credentials, June 2026:

| API | Endpoint | ZRH→MAD | US routes | Notes |
|---|---|---|---|---|
| InstaFlights | `GET /v1/shop/flights` | ❌ 404 | ✅ JFK/ORD/MIA | European origins not in cert cache |
| Flight Search | `POST /v1/offers/flightSearch` | ❌ empty | ✅ JFK (1 430 offers, 73 destinations) | ZRH as origin returns `{}` |
| Flight Check | `POST /v1/offers/flightCheck` | ✅ LX 2020 | n/a | Not a search — revalidates known flight |

**Bottom line for ZRH→MAD in cert:** only `flightCheck` works.
For US routes, both InstaFlights and Flight Search work from JFK.

---

## Step 2a — Flight Search API (inspirational, open-destination)

**Endpoint:** `POST /v1/offers/flightSearch`

The most powerful search API. Supports open origin/destination, date ranges, length-of-stay,
themes, regions, and budget filters. Returns cache-based results.

Works in cert with DEVCENTER **only from US origins** (JFK confirmed: 1 430 offers across
73 destinations). European origins like ZRH return an empty response with just `{"timestamp":"..."}`.

### Request

```bash
curl -X POST "https://api.cert.platform.sabre.com/v1/offers/flightSearch" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "departureLocation": {
      "locationType": "Airport",
      "locationCode": "JFK"
    },
    "departureDateRange": {
      "fromDate": "2026-07-01",
      "toDate": "2026-07-31"
    },
    "lengthsOfStay": [7],
    "processingOptions": {
      "publicContentPointOfSaleCountry": "US",
      "returnLowestNonStopFare": true,
      "returnFullOffers": false,
      "returnMode": "Per Day"
    },
    "sources": {
      "providers": ["Sabre"],
      "distributionModels": ["ATPCO"]
    }
  }'
```

### Key parameters

| Parameter | Options | Notes |
|---|---|---|
| `locationType` | `Airport`, `City`, `Country`, `Region`, `Theme` | For `arrivalLocations` too |
| `returnMode` | `Per Day`, `Per Month` | Per Day returns one best offer per day |
| `returnFullOffers` | `true` / `false` | `false` = lead fares only (much lighter response); `true` = full itinerary + fare breakdown. Use `false` first to explore |
| `returnLowestNonStopFare` | `true` / `false` | Returns a separate nonstop lead fare per destination |
| `lengthsOfStay` | array of ints | Multiple values allowed — compare trip lengths |

### Response structure (`returnFullOffers: false`)

```json
{
  "timestamp": "...",
  "journeys": [
    {
      "id": "d7e30dc7-...",
      "originAirportCode": "JFK",
      "destinationAirportCode": "MAD",
      "departureDate": "2026-07-07",
      "requestedJourneyIndex": 0
    }
  ],
  "offers": [
    {
      "type": "FlightOffer",
      "id": "1d758810-...",
      "totalPrice": { "amount": "633.93", "currencyCode": "USD" },
      "journeyRefs": ["d7e30dc7-..."]
    }
  ]
}
```

### Response structure (`returnFullOffers: true`)

Same as above plus `flights` array and full fare breakdown inside each offer's `fares[]`:

```json
{
  "flights": [{ "departureAirportCode": "JFK", "arrivalAirportCode": "MAD", ... }],
  "offers": [{
    "totalPrice": { "amount": "633.93", "currencyCode": "USD" },
    "items": [{
      "fares": [{
        "fareTotal": {
          "equivalentFare": "555.00",
          "taxAmount":      "78.93",
          "amount":         "633.93",
          "currencyCode":   "USD"
        },
        "fareComponents": [{ "fareBasisCode": "...", "segmentDetails": [...] }]
      }]
    }]
  }]
}
```

### Destinations available from JFK in cert (73 total, verified July 2026)

AMS, AUA, BCN, BDA, BGI, BOG, BOS, BRU, BUF, CAI, CDG, CLO, CUN, DCA, DEL, DUB,
DXB, EZE, FCO, FLL, FRA, GIG, GRU, GVA, HKG, ICN, IST, LAX, LHR, LIM, **MAD**, MIA,
MNL, MXP, NRT, ORD, PTY, SAN, SFO, SJU, TLV, TPE, YYZ, ZRH and more.

---

## Step 2b — InstaFlights (point-to-point, live cache)

**Endpoint:** `GET /v1/shop/flights`

Simpler than Flight Search — fixed origin and destination, single date, live pricing.
Works in cert for US domestic and some international routes from US airports.

```bash
curl "https://api.cert.platform.sabre.com/v1/shop/flights\
?origin=JFK&destination=LAX&departuredate=2026-07-01&mode=live&limit=10&offset=1" \
  -H "Authorization: Bearer $TOKEN"
```

### Parameters

| Parameter | Required | Notes |
|---|---|---|
| `origin` | Yes | IATA airport code |
| `destination` | Yes | IATA airport code |
| `departuredate` | Yes | `YYYY-MM-DD` |
| `mode` | Yes | Must be `live` — `cache` returns 403 with DEVCENTER |
| `limit` | No | Results per page |
| `offset` | No | **Minimum 1** — 0 returns 400 |

### Response structure

```json
{
  "PricedItineraries": [{
    "AirItinerary": {
      "OriginDestinationOptions": {
        "OriginDestinationOption": [{
          "FlightSegment": [{
            "DepartureAirport":  { "LocationCode": "JFK" },
            "ArrivalAirport":    { "LocationCode": "LAX" },
            "MarketingAirline":  { "Code": "B6" },
            "FlightNumber":      323,
            "DepartureDateTime": "2026-07-01T08:00:00",
            "ArrivalDateTime":   "2026-07-01T11:00:00",
            "StopQuantity":      0,
            "ElapsedTime":       360
          }]
        }]
      }
    },
    "AirItineraryPricingInfo": {
      "ItinTotalFare": {
        "BaseFare":  { "CurrencyCode": "USD", "Amount": 242.79 },
        "TotalFare": { "CurrencyCode": "USD", "Amount": 276.40 }
      },
      "FareInfos": {
        "FareInfo": [{
          "TPA_Extensions": {
            "Cabin":          { "Cabin": "Y" },
            "SeatsRemaining": { "Number": 7 }
          }
        }]
      }
    }
  }],
  "Page": { "Size": 10, "Offset": 1 }
}
```

### Routes confirmed in cert

| Route | Works? |
|---|---|
| JFK → LAX | ✅ |
| ORD → LAX | ✅ |
| MIA → NYC | ✅ |
| ZRH → MAD (any date/direction/variant) | ❌ 404 |

A `404` means no cache data for that route — not an auth error. Treat it as empty results.

---

## Step 2c — Flight Check (revalidation of a known itinerary)

**Endpoint:** `POST /v1/offers/flightCheck`

This API does **not search** for flights. It revalidates a specific itinerary you already know
(carrier, flight number, departure time, date, cabin class) and returns current pricing.
It is the **only way to get ZRH→MAD pricing in cert with DEVCENTER**.

### Working test flight in cert

**LX 2020 ZRH→MAD** — verified June 2026. LX 2022 (used in earlier sessions) is no longer
in the cert dataset.

| Field | Value |
|---|---|
| Flight | LX 2020 |
| Route | ZRH → MAD |
| Departure | 06:55 |
| Arrival | 09:20 |
| Duration | 145 min |
| Aircraft | E90 (Embraer 190) |
| PCC required | `PC18` |

**LX 2020 does not operate every day.** The cert reflects the real Swiss schedule.
Do not hardcode a date — scan forward to find the next operating date.
Verified operating pattern (June/July 2026): most Mon/Tue/Thu have data; other days vary.

### Sample fares returned (4 options)

| Base fare | Taxes | Total |
|---|---|---|
| EUR 95.00 | EUR 67.34 | **EUR 162.34** |
| EUR 157.00 | EUR 67.34 | **EUR 224.34** |
| EUR 303.00 | EUR 114.86 | **EUR 417.86** |
| EUR 690.00 | EUR 110.78 | **EUR 800.78** |

### Request

```bash
DATE="2026-07-17"  # must be a date LX 2020 operates — scan if unsure

curl -X POST "https://api.cert.platform.sabre.com/v1/offers/flightCheck" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "journeys": [{"flights": [{
      "departureAirportCode": "ZRH",
      "departureDate": "'"$DATE"'",
      "departureTime": "06:55",
      "arrivalAirportCode": "MAD",
      "arrivalDate": "'"$DATE"'",
      "arrivalTime": "09:20",
      "operatingAirlineCode": "LX",
      "operatingFlightNumber": 2020,
      "marketingAirlineCode": "LX",
      "marketingFlightNumber": 2020,
      "segmentDetails": { "bookingClassCode": "Y" }
    }]}],
    "fare": {
      "currencyCode": "EUR",
      "cabin": { "logic": "Jump Cabin", "name": "Economy" },
      "returnTaxBreakdown": true
    },
    "retailing": {
      "returnOfferAttributes": ["Baggage"],
      "returnAdditionalOffers": { "numberOfAdditionalOffers": 3 }
    },
    "travelers": [{ "passengerTypeCode": "ADT" }],
    "processingOptions": { "pseudoCityCode": "PC18" }
  }'
```

### Response structure

```json
{
  "flights": [{
    "departureAirportCode": "ZRH", "departureTime": "06:55",
    "arrivalAirportCode":   "MAD", "arrivalTime":   "09:20",
    "operatingAirlineCode": "LX",  "operatingFlightNumber": 2020,
    "aircraftTypeCode": "E90",     "durationInMinutes": 145
  }],
  "offers": [{
    "totalPrice": { "amount": "162.34", "currencyCode": "EUR" },
    "items": [{
      "fares": [{
        "fareTotal": {
          "equivalentFare": "95.00",
          "taxAmount":      "67.34",
          "amount":         "162.34",
          "currencyCode":   "EUR"
        }
      }]
    }]
  }],
  "errors": []
}
```

Error cases:

| Error | Meaning |
|---|---|
| `"No such flight (LX 2020)"` | Flight doesn't operate on that date in cert |
| `"Itinerary could not be priced."` | Wrong times, wrong PCC, or past date |

---

## Other APIs

### Travel Theme Lookup — good for connectivity tests

```bash
curl "https://api.cert.platform.sabre.com/v1/lists/supported/shop/themes" \
  -H "Authorization: Bearer $TOKEN"
```

Always returns 12 themes with no content restrictions. Use to verify token and connectivity.

### APIs that do NOT work with DEVCENTER

| API | Error | Reason |
|---|---|---|
| Bargain Finder Max v4 (`POST /v1/offers/shop`) | 404 / IF2 PROCESSING ERROR | Requires provisioned agency with content |
| InstaFlights cache mode (`mode=cache`) | 403 Not Authorized | No cache privileges |

---

## Python demo script

`sabre_flights_demo.py` uses Flight Check to demonstrate ZRH→MAD. It automatically scans
forward day by day to find the next date LX 2020 operates, then prints all fare options.

```bash
python sabre_flights_demo.py
```

---

## To get production access

Everything above uses DEVCENTER sandbox credentials. For production you need:

- A Sabre agency account with a real PCC (PseudoCityCode)
- An EPR (Electronic Pseudo Record) assigned to your agency
- A production Client ID and Secret (not DEVCENTER)
- A commercial contract with Sabre

Production endpoint: `https://api.platform.sabre.com`
