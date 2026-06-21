# JCarranza Mini-Midoffice — Frontend SPA

> Phase 6 of 9. jQuery + Handlebars + Bootstrap 3 served by Tomcat DefaultServlet.

---

## 1. Architecture Decisions

### Why Classic SPA (no React/Angular)

This stack — jQuery for AJAX and DOM manipulation, Handlebars as a template engine,
Bootstrap 3 for layout — is typical of mature mid-office and back-office Java applications
and deliberately avoids modern SPA frameworks.

### How Static Files Are Served

The `DispatcherServlet` is mapped exclusively to `/api/*`.
Tomcat `DefaultServlet` serves `index.html` and all resources from `static/` directly,
without going through Spring MVC.

```
GET /                     → Tomcat DefaultServlet → index.html
GET /static/css/app.css   → Tomcat DefaultServlet → file
GET /api/profiles         → DispatcherServlet → ProfileController
```

No `mvc:resources` or `WebMvcConfigurer.addResourceHandlers` is needed.
`configureDefaultServletHandling(DefaultServletHandlerConfigurer)` is already enabled in
`WebMvcConfig` so that the `DispatcherServlet` does not absorb non-API routes.

### File Structure

```
mini-midoffice-web/src/main/webapp/
├── index.html                    ← SPA entry point, contains all Handlebars templates
└── static/
    ├── css/
    │   └── app.css               ← navbar, spinner, table overrides
    └── js/
        └── app/
            ├── api.js            ← centralized AJAX wrapper
            ├── app.js            ← Handlebars helpers, section router, global alert
            ├── dashboard.js      ← Dashboard module
            ├── profiles.js       ← Profiles module
            ├── flights.js        ← Flights module
            └── bookings.js       ← Bookings module
```

Bootstrap 3, jQuery 3.7.1 and Handlebars 4.7.8 are loaded from a public CDN:

| Library    | CDN |
|-------------|-----|
| Bootstrap 3.3.7 CSS | `maxcdn.bootstrapcdn.com` |
| Bootstrap 3.3.7 JS  | `maxcdn.bootstrapcdn.com` |
| jQuery 3.7.1        | `code.jquery.com` |
| Handlebars 4.7.8    | `cdnjs.cloudflare.com` |

---

## 2. JS Module Pattern

Each screen is an IIFE module that exposes only `init()` (and optionally public methods
needed for inter-module communication):

```javascript
var Profiles = (function ($) {
    'use strict';
    // private state
    function init() { ... }
    return { init: init };
}(jQuery));
```

`app.js` acts as a router: clicking on the navbar calls `Module.init()`.
The module reloads its data every time the section is activated, guaranteeing fresh data.

### Inter-Module Communication

Two flows require one module to call another:
- **Flights → Bookings**: after booking a flight, `Bookings.loadForTraveller(tid)` and
  `$('[data-section="bookings"]').trigger('click')`.
- **Dashboard → Bookings**: the "Bookings" button in the recent profiles table
  does the same.

For `Bookings` to be accessible at the time of the call, it is sufficient for `bookings.js`
to load before `app.js` (the order in the HTML guarantees this).

---

## 3. Handlebars Templates

All templates are inline in `index.html` as
`<script type="text/x-handlebars-template">` blocks. Advantages:

- No additional AJAX to load templates.
- Works without special Tomcat configuration.
- Easy to review in a single file.

| Template ID              | Generates                                        |
|--------------------------|--------------------------------------------------|
| `#tpl-profiles`          | `<tr>` rows for `<tbody id="profiles-tbody">`    |
| `#tpl-flights`           | Complete table with search results               |
| `#tpl-bookings-list`     | `<tr>` rows for `<tbody id="bookings-tbody">`    |
| `#tpl-booking-detail`    | `<div class="panel">` panel below the table      |

### Registered Helpers

| Helper | Use |
|--------|-----|
| `{{{statusBadge status}}}` | Bootstrap `<span class="label label-*">` based on SEARCHED/RESERVED/CANCELLED |
| `{{statusPanel status}}` | Bootstrap panel class (`success`, `warning`, `default`) |
| `{{durationMins N}}` | formats minutes as `2h 15m` |
| `{{orDash value}}` | returns `—` if value is null/empty |

---

## 4. Screens

### 4.1 Dashboard

Shows:
- Card with total profile count (`GET /api/profiles`).
- Table with a note about the Sabre cert environment (what works and what doesn't).
- Table with the 10 most recent profiles; each row has a
  "Bookings" button that navigates to the Bookings tab with that travellerId pre-loaded.

There is no dedicated `/api/stats` endpoint; the count is done client-side over the profile list.
The booking count by status requires selecting a traveller, so it is delegated to the Bookings module.

### 4.2 Profiles

Flow:
1. Initial load → `GET /api/profiles` (up to 50 entries).
2. Search input → `GET /api/profiles?q=text` on Enter or button click.
3. "New Profile" button → Bootstrap modal with creation form.
4. "Edit" button on each row → same modal with fields pre-populated from
   `GET /api/profiles/{id}`.
5. Modal submit → `POST /api/profiles` (create) or `PUT /api/profiles/{id}` (update).
6. After saving: closes modal, reloads the list, shows success alert.

Form fields: `firstName`, `lastName`, `email` (required),
`company`, `passportNumber`, `frequentFlyerNumber` (optional).

### 4.3 Flight Search

Flow:
1. Form with: `origin` (required), `destination`, `fromDate`, `toDate`,
   `passengerCount`, `currencyCode`, `travellerId`.
2. Submit → `GET /api/flights/search` with the parameters.
3. Results rendered with Handlebars in `#flights-results`.
4. If `travellerId` is set and the result has a `flightKey`:
   a "Reserve" button appears → `POST /api/bookings`.
5. After booking: success alert with confirmed price, navigates to Bookings.

**Sabre cert note:** The form shows an `alert-info` explaining that only US origins
return `flightKey`. If the search returns no results, an `alert-warning` is shown
suggesting using JFK.

Default dates are calculated dynamically (+14 days and +28 days from today).

### 4.4 Bookings

Flow:
1. `travellerId` input + Load button → `GET /api/bookings?travellerId={id}`.
2. Results in Handlebars table. Each row has:
   - Status badge (colored).
   - Confirmed price / searched price.
   - ⊕ detail button → `GET /api/bookings/{id}` → Handlebars panel below the table.
   - "Cancel" button (only if status ≠ CANCELLED) → `PUT /api/bookings/{id}/cancel`.
3. The module exposes `loadForTraveller(id)` so other modules can
   pre-load bookings before navigating to this section.

---

## 5. api.js — AJAX Wrapper

All modules call the API through `API.*` (defined in `api.js`).
The wrapper centralizes:
- The base URL `/api`.
- The error handler: parses the JSON error body and calls `window.showAlert`.
- `contentType: 'application/json'` and `JSON.stringify` in POST/PUT.

Returns jQuery Deferred (`.done()`, `.fail()`).

---

## 6. HOW-TO: Add the SPA to the WAR and Test in the Browser

The SPA requires no additional build steps. Static files are automatically included
in the WAR by Maven (everything under `src/main/webapp/`).

After a rebuild:

```powershell
docker stop mini-midoffice-app; docker rm mini-midoffice-app

docker run -d --name mini-midoffice-app -p 8080:8080 `
  -e JAVA_OPTS="-Ddb.url=jdbc:postgresql://host.docker.internal:5432/miniumbrella -Ddb.username=postgres -Ddb.password=miniumbrella" `
  -v "C:/workspace/mini-midoffice/mini-midoffice-web/target/mini-midoffice-web-1.0.0-SNAPSHOT.war:/usr/local/tomcat/webapps/ROOT.war:ro" `
  tomcat:9.0-jdk17
```

Open `http://localhost:8080/` in the browser.

### Complete Test Flow

1. **Profiles** → New Profile → fill in name/email → Save → profile appears in the table.
2. **Flight Search** → origin=`JFK`, fromDate=`2026-08-01`, toDate=`2026-08-15`,
   Traveller ID=`1` → Search Sabre → results table → Reserve any flight.
3. **Bookings** → automatically loads for traveller 1 → booking in RESERVED status → Cancel.
4. **Dashboard** → shows total profiles and recent table.

---

*Next phase → docs/07-reporting.md: SQL reporting with direct PostgreSQL queries.*
