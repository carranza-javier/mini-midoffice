# JCarranza Mini-Midoffice — SQL Reporting

> Phase 7 of 9. Ad-hoc reporting on PostgreSQL: native queries, CSV and Excel export.

---

## 1. Context: The Real Use Case

In travel-agency mid-office systems, a common part of 2nd-level support work is exactly this flow:

> "The client asks for something very specific — how many bookings did they make to destination X last month, how much did each provider bill in Q3. We write a direct SQL query against the database and send them a CSV or an Excel."

This module implements that flow in a reusable way: three pre-configured reports, optional date filter, immediate export in CSV or Excel from the same REST endpoint.

---

## 2. Decision: Native SQL vs HQL/ORM

Reports use **native SQL directly on PostgreSQL** (not HQL, not JPQL).

| Criterion | Native SQL | HQL/ORM |
|---|---|---|
| Aggregation functions | Native PG `SUM`, `AVG`, `ROUND`, `TO_CHAR` | Partial support, no `TO_CHAR` |
| Free `GROUP BY` | Any expression | Limited to columns mapped in entities |
| Report clarity | The SQL query is the exact specification | Abstraction layers hide what is executed |
| Ad-hoc capability | Changing the query is trivial | Requires touching entities and mappings |
| Analytical performance | PG optimizer works directly on the SQL | PG never sees the original query until translation |

For ad-hoc reporting, **the ORM adds complexity without value**. An analyst who asks "please give me this" expects that what we execute in the DB is exactly what appears in the code — not an opaque translation.

### Why Not Spring's JdbcTemplate

`JdbcTemplate` requires `spring-jdbc`. Since we already have `DataSource` (HikariCP, configured in `PersistenceConfig`), using JDBC directly with `try-with-resources` is equally clean, without adding dependencies. Accessing `DataSource` from the DAO is still standard Spring dependency injection.

---

## 3. Decision: Apache POI for Excel

**Chosen:** `org.apache.poi:poi-ooxml:5.2.5`

| Library | Format | Advantages | Reason Discarded |
|---|---|---|---|
| **Apache POI** ✓ | `.xlsx` (OOXML) | Java enterprise industry standard, active, full style support | — |
| OpenCSV | CSV only | Very lightweight | Does not do Excel |
| JExcel (jxl) | `.xls` (BIFF8) | Small | Abandoned, obsolete format |
| FastExcel | `.xlsx` | Fast, lightweight | No advanced styles, small community |

**Apache POI** is the obvious choice for an enterprise mid-office system:
- Used in virtually all Java enterprise systems that generate Excel.
- `poi-ooxml` produces `.xlsx` (Office Open XML), the default format since Excel 2007.
- Supports styles (bold, colors, numeric format), column auto-sizing and multiple sheets — everything a corporate client expects.
- Java 11+ (compatible with our Java 17).

---

## 4. Module Architecture

```
mini-midoffice-domain/
└── com.jcarranza.minimidoffice.domain.report.ReportResult   ← tabular DTO (headers + rows)

mini-midoffice-persistence/
├── dao/ReportingDao.java                          ← contract interface
└── jdbc/JdbcReportingDao.java                     ← native JDBC + DataSource

mini-midoffice-service/
├── ReportingService.java                          ← service interface
└── ReportingServiceImpl.java                      ← direct delegation to DAO

mini-midoffice-web/
├── controller/ReportController.java               ← GET /api/reports/*
└── export/
    ├── CsvExporter.java                           ← RFC 4180, UTF-8 BOM
    └── ExcelExporter.java                         ← Apache POI XSSFWorkbook
```

### Request Flow

```
GET /api/reports/by-destination?format=excel&from=2026-01-01&to=2026-12-31
        │
        ▼
ReportController.byDestination(from, to, "excel", response)
        │
        ├─► ReportingService.byDestination(from, to)
        │       │
        │       └─► JdbcReportingDao.bookingsByDestination(from, to)
        │               │  DataSource.getConnection()          ← direct HikariCP connection
        │               │  PreparedStatement(dynamic SQL)      ← WHERE built based on params
        │               │  ResultSetMetaData → headers[]       ← headers from JDBC metadata
        │               │  ResultSet → List<List<Object>>      ← typed rows (Long, BigDecimal, String)
        │               └─► ReportResult("Bookings by Destination", headers, rows)
        │
        └─► ExcelExporter.write(report, response.getOutputStream())
                XSSFWorkbook  → bold header + blue background
                               → numeric cells with #,##0.00 format
                               → column auto-size
                               → write() to response stream
```

---

## 5. The Three Reports

### 5.1 Bookings by Destination

**Endpoint:** `GET /api/reports/by-destination`

**Use case:** "Which destinations did our clients fly to most? How much did we bill per destination?"

```sql
SELECT b.destination                                 AS "Destination",
       COUNT(*)                                      AS "Bookings",
       COALESCE(SUM(b.confirmed_price), 0)           AS "Revenue",
       COALESCE(ROUND(AVG(b.confirmed_price), 2), 0) AS "Avg Price"
FROM booking b
WHERE b.status = 'RESERVED'
  [AND b.departure_date >= ?]
  [AND b.departure_date <= ?]
GROUP BY b.destination
ORDER BY COUNT(*) DESC
```

Sample result:
```
Destination | Bookings | Revenue   | Avg Price
GVA         |        5 | 22197.50  |   4439.50
FCO         |        3 | 11850.00  |   3950.00
```

---

### 5.2 Bookings by Provider

**Endpoint:** `GET /api/reports/by-provider`

**Use case:** "What portion of business goes through Sabre vs Amadeus? Is the contract with each GDS justified?"

```sql
SELECT b.provider                                    AS "Provider",
       COUNT(*)                                      AS "Bookings",
       COALESCE(SUM(b.confirmed_price), 0)           AS "Revenue",
       COALESCE(ROUND(AVG(b.confirmed_price), 2), 0) AS "Avg Price"
FROM booking b
WHERE b.status = 'RESERVED'
  [AND b.departure_date >= ?]
  [AND b.departure_date <= ?]
GROUP BY b.provider
ORDER BY COUNT(*) DESC
```

---

### 5.3 Bookings by Travel Month

**Endpoint:** `GET /api/reports/by-month`

**Use case:** "What was the volume of travel in July? Which months are the peaks?"

```sql
SELECT TO_CHAR(b.departure_date, 'YYYY-MM')          AS "Month",
       COUNT(*)                                       AS "Bookings",
       COALESCE(SUM(b.confirmed_price), 0)            AS "Revenue",
       COALESCE(ROUND(AVG(b.confirmed_price), 2), 0)  AS "Avg Price"
FROM booking b
WHERE b.status = 'RESERVED'
  AND b.departure_date IS NOT NULL
  [AND b.departure_date >= ?]
  [AND b.departure_date <= ?]
GROUP BY TO_CHAR(b.departure_date, 'YYYY-MM')
ORDER BY 1
```

**Why `departure_date` and not `reservation_date`:** the real travel month (the one the client perceives as "when I travelled") is the flight departure date. `reservation_date` is when the booking was processed in the system — it can be weeks or months earlier. A report of "bookings in July" based on `reservation_date` would count bookings made in July to fly in August, which is not what the client is asking.

---

## 6. Endpoint Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `from` | `yyyy-MM-dd` | No | Start of the `departure_date` range |
| `to` | `yyyy-MM-dd` | No | End of the `departure_date` range |
| `format` | `csv` \| `excel` | No (default: `csv`) | Download format |

Without `from`/`to`: the report covers all historical data.

---

## 7. CSV Export

- Format: RFC 4180 (CRLF, optional quotes, double-quote escape).
- Encoding: UTF-8 with BOM (`EF BB BF`) so Excel on Windows detects it correctly.
- Headers in the first row.
- File name: `bookings-by-destination.csv`, etc.

### Sample Output

```
Destination,Bookings,Revenue,Avg Price
GVA,5,22197.50,4439.50
```

---

## 8. Excel Export

- Format: `.xlsx` (Office Open XML, compatible with Excel 2007+, LibreOffice, Google Sheets).
- Single sheet with the report title as the tab name.
- Row 0: bold headers with light blue background (`IndexedColors.PALE_BLUE`).
- Numeric columns:
  - `Long`/`Integer` → `#,##0` format (no decimals)
  - `BigDecimal`/`Double` → `#,##0.00` format
- Column auto-sizing to content.
- Generated in memory with `XSSFWorkbook` — acceptable for typical report volumes (hundreds/thousands of rows). For millions of rows, `SXSSFWorkbook` (streaming) would be used.

---

## 9. Headers Extracted from JDBC Metadata

An important implementation decision: report headers are extracted from `ResultSetMetaData.getColumnLabel()`, not hardcoded in Java code.

```java
for (int c = 1; c <= cols; c++) {
    headers.add(meta.getColumnLabel(c));  // uses the AS "..." alias from SQL
}
```

This means that if the SQL query is changed (`AS "New Header"`), the CSV and Excel reflect the change automatically without touching `JdbcReportingDao`. The SQL query is the single source of truth about what appears in the report.

---

## 10. Frontend

The `Reports` module in the SPA (Bootstrap 3) has:
- A shared date filter (applies to all three reports).
- Three panels, one per report, each with a CSV button (grey) and an Excel button (green).
- The buttons navigate directly to `/api/reports/...?format=...&from=...&to=...` — the browser initiates the download without needing AJAX.

---

## 11. Design Decisions

| Decision | Alternative | Reason |
|---|---|---|
| Native JDBC in DAO | `session.createNativeQuery()` from Hibernate | Avoids Hibernate session overhead; more explicit for reporting; the DAO does not depend on an active Hibernate TX |
| Generic `ReportResult` (headers + Object rows) | Typed POJOs per report | A single exporter (CSV/Excel) serves all reports; SQL defines the schema, not Java code |
| `ResultSetMetaData` for headers | Hardcoded headers | The SQL query is the source of truth; changing the `AS` alias in SQL updates the export without changing Java |
| CSV with UTF-8 BOM | Without BOM | Excel on Windows correctly opens UTF-8 files with BOM; without BOM it interprets as Windows-1252 |
| `XSSFWorkbook` (in-memory) | `SXSSFWorkbook` (streaming) | Typical report volumes (< 10,000 rows). Streaming would be added if the dataset grows |
| Download via `window.location.href` | Fetch + Blob | Simpler, no blob management in JS; the browser handles the file name from `Content-Disposition` |

---

*Next phase → docs/08-observability.md: Logback, business/integration/error logs, MDC.*
