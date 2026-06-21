# JCarranza Mini-Midoffice — Project Brief
## Objective

Build a portfolio application modelling a travel-agency mid-office system: booking management, GDS integrations, and ad-hoc SQL reporting. The goal is to demonstrate senior Java backend skills on a realistic, production-representative stack.
## Business Context

A mid-office system of this kind is a monolithic Java application (NOT microservices, NOT cloud-native) that manages bookings for travel agencies. Characteristics we replicate:

- Java 17, Spring Framework 5 (WITHOUT Spring Boot), Hibernate 5, PostgreSQL

- Maven, WAR deployed on Tomcat

- Frontend with jQuery + Handlebars + Bootstrap 3 (classic SPA, no React/Angular)

- Integrations with external GDS systems: Amadeus, Galileo, Sabre (export to Amadeus/SAP, import from Galileo/Amadeus/Sabre)

- Small team where each developer does everything: features, bugfixes, 2nd-level support, ad-hoc SQL reporting

- Technical challenge mentioned: diagnosing integration errors with GDS systems, sometimes hard to trace
## Required Stack

- Backend: Java 17, Spring 5 (classic XML or JavaConfig, NO Spring Boot), Hibernate 5, Maven, WAR/Tomcat

- DB: PostgreSQL

- Frontend: jQuery, Handlebars, Bootstrap 3, AJAX

- External integration: Sabre Sandbox (REST, OAuth2)
## Functional Domain

### 1. Profiles (traveller profiles)

Fields: id, first name, last name, email, company, passport, frequent flyer number, creation date.

Operations: create, edit, query, search.
### 2. Mid Office (booking management)

Fields: bookingId, travellerId, origin, destination, search date, booking date, price, provider, status.

Initial provider: Sabre Sandbox.

Statuses: SEARCHED, RESERVED, CANCELLED.
## Phase Plan (sequential, approximately one Code session per phase)

1. Architecture (design only, no code) → docs/01-architecture.md

2. Data model: Hibernate entities, DAOs, PostgreSQL scripts → docs/02-data-model.md

3. Decoupled Sabre integration (HTTP client, OAuth, DTOs, mapper, easily replaceable by Amadeus/Galileo) → docs/03-sabre-integration.md

4. Services (ProfileService, BookingService, SearchService) with transactions and validations → docs/04-services.md

5. REST Controllers (Profiles, Flights, Bookings) → docs/05-rest-endpoints.md

6. Classic frontend SPA (jQuery + Handlebars + Bootstrap 3) → docs/06-frontend.md

7. SQL Reporting (bookings by destination/provider/month, CSV/Excel export) → docs/07-reporting.md

8. Observability (Logback, business/integration/error logs) → docs/08-observability.md

9. Support simulation (common Sabre/DB/validation errors with stack traces and troubleshooting exercises) → docs/09-support-simulation.md
## Validated Sabre Credentials (certification environment)

- Correct endpoint: https://api.cert.platform.sabre.com (NOT api.platform.sabre.com, which is production)

- Auth: OAuth2 client_credentials, double Base64 (Base64 of user, Base64 of password, then Base64 of b64user:b64pass)

- There is a working reference script at C:\workspace\sabre-flights-demo\
## Work Rule

Each phase ends by saving its design/decisions in the corresponding docs/0X-name.md file before moving to the next. Do not advance to the next phase without the current phase's file saved.
