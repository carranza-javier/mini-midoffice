# JCarranza Mini-Midoffice — Architecture

> Phase 1 of 9. Design only, no code.

---

## 1. Overview

Monolithic Java application deployed as a WAR on Tomcat. A single deployable artifact containing all layers. Maven multi-module to enforce layer boundaries at compile time, without becoming microservices.

```
Browser (SPA: jQuery + Handlebars + Bootstrap 3)
        │  AJAX / JSON
        ▼
   Tomcat (WAR)
   ┌─────────────────────────────────────────┐
   │  Spring MVC 5  (DispatcherServlet)      │  ← Presentation Layer
   │  REST Controllers  /api/**              │
   ├─────────────────────────────────────────┤
   │  Spring Services  @Transactional        │  ← Service Layer
   │  ProfileService · BookingService        │
   │  SearchService · ReportingService       │
   ├─────────────────────────────────────────┤
   │  DAO (Hibernate 5 SessionFactory)       │  ← Persistence Layer
   │  TravellerProfileDao · BookingDao       │
   │                  PostgreSQL             │
   ├─────────────────────────────────────────┤
   │  Integration                            │  ← Integration Layer
   │  GdsFlightSearchPort (search)           │
   │  GdsFlightCheckPort  (revalidation)     │
   │  SabreFlightSearchAdapter               │
   │  SabreFlightCheckAdapter                │
   │  SabreOAuthProvider · SabreHttpClient   │
   └─────────────────────────────────────────┘
        │  REST / OAuth2
        ▼
   Sabre Sandbox (api.cert.platform.sabre.com)
```

---

## 2. Maven Module Structure

```
mini-midoffice/                    (parent, packaging: pom)
├── pom.xml                       — dependencyManagement, pluginManagement
├── mini-midoffice-domain/         (packaging: jar)
├── mini-midoffice-integration/    (packaging: jar)
├── mini-midoffice-persistence/    (packaging: jar)
├── mini-midoffice-service/        (packaging: jar)
└── mini-midoffice-web/            (packaging: war)  ← final artifact
```

### Module Dependencies

```
web        → service, integration (interfaces)
service    → persistence, domain, integration (via port interface)
persistence→ domain
integration→ domain
domain     → (nothing — pure POJOs + enums + domain exceptions)
```

The rule is that no lower module imports from a higher one. `domain` knows nothing about Hibernate or Spring.

### Key External Dependencies (versions fixed in parent BOM)

| Library | Version | Justification |
|---|---|---|
| Spring Framework | 5.3.x | Required stack of the real project |
| Hibernate ORM | 5.6.x | Required stack |
| Spring ORM | 5.3.x | `LocalSessionFactoryBean` integration |
| Jackson Databind | 2.15.x | JSON in Spring MVC |
| HikariCP | 4.x | Connection pool (compatible with Hibernate 5 without Boot autoconfig) |
| PostgreSQL JDBC | 42.x | Driver |
| Apache HttpClient | 5.x | HTTP client for Sabre |
| Logback Classic | 1.4.x | Logging |
| Servlet API | 4.0.1 | Tomcat 9 (scope: provided) |
| JUnit 5 + Mockito | — | Tests |

---

## 3. Package Structure

Root: `com.jcarranza.minimidoffice`

### `mini-midoffice-domain`
```
com.jcarranza.minimidoffice.domain
├── model/
│   ├── TravellerProfile.java     — Hibernate entity
│   └── Booking.java              — Hibernate entity
├── enums/
│   ├── BookingStatus.java        — SEARCHED, RESERVED, CANCELLED
│   └── GdsProvider.java         — SABRE, AMADEUS, GALILEO
└── exception/
    ├── BusinessException.java    — business validations
    └── IntegrationException.java — failures communicating with GDS
```

### `mini-midoffice-integration`
```
com.jcarranza.minimidoffice.integration
├── port/
│   ├── GdsFlightSearchPort.java  — interface: exploratory search (called by SearchService)
│   └── GdsFlightCheckPort.java   — interface: point revalidation (called by BookingService)
├── sabre/
│   ├── client/
│   │   └── SabreHttpClient.java
│   ├── auth/
│   │   └── SabreOAuthProvider.java
│   ├── dto/
│   │   ├── SabreFlightSearchRequest.java
│   │   ├── SabreFlightSearchResponse.java
│   │   ├── SabreFlightCheckRequest.java
│   │   ├── SabreFlightCheckResponse.java
│   │   └── SabreTokenResponse.java
│   ├── mapper/
│   │   ├── SabreFlightSearchMapper.java
│   │   └── SabreFlightCheckMapper.java
│   ├── SabreFlightSearchAdapter.java  — implements GdsFlightSearchPort
│   └── SabreFlightCheckAdapter.java   — implements GdsFlightCheckPort
└── config/
    └── IntegrationConfig.java    — @Configuration: Sabre beans (both adapters)
```

### `mini-midoffice-persistence`
```
com.jcarranza.minimidoffice.persistence
├── dao/
│   ├── TravellerProfileDao.java  — interface
│   └── BookingDao.java           — interface
├── hibernate/
│   ├── HibernateTravellerProfileDao.java
│   └── HibernateBookingDao.java
└── config/
    └── PersistenceConfig.java    — @Configuration: DataSource, SessionFactory, TxManager
```

### `mini-midoffice-service`
```
com.jcarranza.minimidoffice.service
├── ProfileService.java           — interface
├── ProfileServiceImpl.java
├── BookingService.java
├── BookingServiceImpl.java
├── SearchService.java
├── SearchServiceImpl.java
├── ReportingService.java
├── ReportingServiceImpl.java
└── dto/
    ├── ProfileDTO.java
    ├── BookingDTO.java
    ├── FlightOptionDTO.java
    ├── FlightSearchCriteria.java
    └── BookingRequestDTO.java
```

### `mini-midoffice-web`
```
com.jcarranza.minimidoffice.web
├── controller/
│   ├── ProfileController.java    — /api/profiles/**
│   ├── FlightController.java     — /api/flights/**
│   └── BookingController.java    — /api/bookings/**
├── config/
│   ├── WebAppInitializer.java    — starts both Spring contexts
│   ├── AppConfig.java            — root context: scan service + persistence + integration
│   └── WebMvcConfig.java         — web context: Jackson, CORS, static resources
└── filter/
    └── RequestLoggingFilter.java — log per request (method, URL, duration)

src/main/webapp/
├── WEB-INF/
│   └── web.xml
├── static/
│   ├── css/
│   │   └── bootstrap.min.css     (Bootstrap 3)
│   ├── js/
│   │   ├── lib/
│   │   │   ├── jquery.min.js
│   │   │   └── handlebars.min.js
│   │   └── app/
│   │       ├── profiles.js
│   │       ├── flights.js
│   │       └── bookings.js
│   └── templates/                (Handlebars templates, preloaded or fetched)
└── index.html                    — SPA entry point
```

---

## 4. Layers in Detail

### 4.1 Presentation Layer (Spring MVC)

- `@RestController` + `@RequestMapping`
- Jackson automatically converts POJO ↔ JSON (`MappingJackson2HttpMessageConverter`)
- Input validation with `@Valid` + Bean Validation (Hibernate Validator)
- Global error handling with `@ControllerAdvice` + `@ExceptionHandler`
- Controllers only orchestrate: receive request DTO, call the service, return response DTO. No business logic.
- `index.html` is served from `src/main/webapp/` directly by Tomcat (does not go through DispatcherServlet)

### 4.2 Service Layer

- `@Service` + Spring `@Transactional`
- The transaction opens when entering the service method and closes when exiting
- Rule: DAOs do not manage transactions — only the service layer does
- Converts between domain entities (Hibernate) and service DTOs
- `SearchServiceImpl` coordinates: calls the GDS port, saves the search, returns options
- `BookingServiceImpl` coordinates: validates profile, reserves, persists state

### 4.3 Persistence Layer

- Hibernate 5 `Session` (not JPA `EntityManager`, although Hibernate implements it)
- DAOs inject `SessionFactory` via `@Autowired`
- Typical methods: `findById`, `findAll`, `save`, `update`, `delete`, `findByEmail`, `findByStatus`
- No Spring Data JPA (does not exist in this stack)
- `hbm2ddl.auto = validate` in production; `create-drop` only in tests

### 4.4 Integration Layer

The layer exposes **two ports** with distinct contracts and guarantees. Services know only the interfaces; the adapters know how to talk to Sabre.

**`GdsFlightSearchPort`** — exploratory search
```
List<FlightOptionDTO> search(FlightSearchCriteria criteria)
```
- Called by `SearchService` when the user searches for flights by origin/destination/date range.
- The underlying Sabre operation is `POST /v1/offers/flightSearch`.
- Works with Sabre's cache: two identical calls in a short time may return cached results. Acceptable because it is exploratory, does not commit price or availability.
- Results are saved to DB with `status=SEARCHED` for traceability; they are not binding.

**`GdsFlightCheckPort`** — point revalidation before booking
```
FlightCheckResultDTO check(FlightCheckRequest request)
```
- Called by `BookingService` just before transitioning a `Booking` from `SEARCHED` to `RESERVED`.
- The underlying Sabre operation is `POST /v1/offers/flightCheck`.
- Receives a specific known flight (airline + flight number + exact date + cabin) and returns current availability and price **in real time**, without cache.
- If the check returns that the flight is not available or the price changed, `BookingService` throws `BusinessException` and the transition to `RESERVED` does not occur.
- This step protects against the mismatch between what the user saw in the search and what is actually available at booking time.

**Shared Infrastructure**
- `SabreOAuthProvider` manages the OAuth2 token: obtains it, caches it in memory, refreshes it when expired. Both adapters use it.
- `SabreHttpClient` wraps Apache HttpClient 5: manages timeouts, request/response logs, and re-throws HTTP errors as `IntegrationException`.
- If tomorrow we need to connect Amadeus, create `AmadeusFlightSearchAdapter` and `AmadeusFlightCheckAdapter` implementing the same ports — services don't change.

---

## 5. Data Flow

### 5.1 Flight Search

```
1. Browser → GET /api/flights/search?origin=JFK&fromDate=2026-07-15&toDate=2026-07-20&travellerId=42
2. FlightController.search(SearchRequest) → calls SearchService
3. SearchService:
   a. Validates SearchCriteria
   b. Calls GdsFlightSearchPort.search(criteria)
      i.  SabreOAuthProvider.getValidToken()   → POST /v2/auth/token (if expired)
      ii. SabreHttpClient.post(/v5.2/shop/flights, requestBody)
      iii.SabreFlightMapper.toFlightOptions(sabreResponse) → List<FlightOptionDTO>
   c. Records search in DB: Booking(status=SEARCHED, origin, destination, searchDate)
   d. Returns List<FlightOptionDTO>
4. FlightController → 200 OK + JSON array
5. Browser → Handlebars renders flight table
```

### 5.2 Create Booking (with Flight Check)

The transition from `SEARCHED` to `RESERVED` has a mandatory revalidation step via `GdsFlightCheckPort`. The price and availability the user saw in the search may have changed.

```
1. Browser → POST /api/bookings
              { flightKey: "IB3456|2026-07-15|Y", travellerId: 42, searchedPrice: 234.50 }
2. BookingController.create(BookingRequestDTO) → calls BookingService

3. BookingService (@Transactional):
   a. TravellerProfileDao.findById(42) → TravellerProfile or BusinessException
   b. BookingDao.findByFlightKeyAndTraveller(...) → existing Booking (status=SEARCHED)
      or BusinessException("No prior search found for this flight")

   c. *** FLIGHT CHECK (outside DB transaction, external call) ***
      GdsFlightCheckPort.check(FlightCheckRequest { flightKey, cabin, passengerCount })
        i.  SabreOAuthProvider.getValidToken()
        ii. SabreHttpClient.post(/v1/offers/flightCheck, requestBody)
        iii.SabreFlightCheckMapper.toCheckResult(sabreResponse) → FlightCheckResultDTO
      If FlightCheckResultDTO.available == false:
        → BusinessException("Flight no longer available")
      If FlightCheckResultDTO.currentPrice > searchedPrice * 1.05:  (5% tolerance)
        → BusinessException("Price changed: was 234.50, now " + currentPrice)

   d. booking.setStatus(RESERVED)
      booking.setReservationDate(now())
      booking.setConfirmedPrice(checkResult.currentPrice)
      booking.setProvider(SABRE)
      BookingDao.update(booking)
   e. Returns BookingDTO

4. BookingController → 201 Created + BookingDTO
5. Browser → Handlebars shows confirmation with confirmed price
```

**Why the check is outside the DB transaction**: the HTTP call to Sabre can take 2-5 seconds. Keeping a DB connection open (and the lock on the Booking) for that time exhausts the pool. The correct pattern is: read → close transaction → external call → open new transaction → write. In the implementation, `BookingService.reserve()` will have `@Transactional(propagation = REQUIRES_NEW)` for the final write step, separate from the initial read.

### 5.3 Cancel Booking

```
1. Browser → DELETE /api/bookings/{bookingId}
2. BookingController.cancel(bookingId) → BookingService
3. BookingService (@Transactional):
   a. BookingDao.findById(bookingId) → Booking or BusinessException
   b. Validates: if status=CANCELLED → BusinessException("Already cancelled")
   c. booking.setStatus(CANCELLED)
   d. BookingDao.update(booking)
4. 204 No Content
```

---

## 6. Spring 5 Configuration

### 6.1 Two Application Contexts (classic Spring MVC pattern)

```
Root Application Context  (AppConfig + PersistenceConfig + IntegrationConfig)
│   Contains: Services, DAOs, SessionFactory, DataSource, Sabre beans
│   Loaded by: ContextLoaderListener at Tomcat startup
│
└── Web Application Context  (WebMvcConfig)
        Contains: Controllers, MessageConverters, HandlerMappings
        Loaded by: DispatcherServlet on first request
        Inherits: all beans from the Root Context (parent → child visibility)
```

This is a critical Spring MVC subtlety: if services were in the Web Context, Controllers would see them, but `@Transactional` would not work correctly with the `HibernateTransactionManager` from the Root Context. The separation is mandatory.

### 6.2 WebAppInitializer.java (Servlet 3.0, optional without web.xml)

```java
// Extends AbstractAnnotationConfigDispatcherServletInitializer
// Method getRootConfigClasses() → AppConfig.class, PersistenceConfig.class, IntegrationConfig.class
// Method getServletConfigClasses() → WebMvcConfig.class
// Method getServletMappings() → "/api/*"
```

Used together with `web.xml` for static resources (see §8).

### 6.3 AppConfig.java (Root Context)

```
@Configuration
@ComponentScan(basePackages = {
    "com.jcarranza.minimidoffice.service",
    "com.jcarranza.minimidoffice.persistence.hibernate",
    "com.jcarranza.minimidoffice.integration.sabre"
})
@EnableTransactionManagement   ← activates @Transactional
@Import({PersistenceConfig.class, IntegrationConfig.class})
```

### 6.4 PersistenceConfig.java

```
@Configuration
@Bean DataSource → HikariDataSource
    url: jdbc:postgresql://localhost:5432/miniumbrella
    username / password from external properties
    maximumPoolSize: 10
    connectionTimeout: 30000

@Bean LocalSessionFactoryBean → configures Hibernate
    setDataSource(dataSource)
    setPackagesToScan("com.jcarranza.minimidoffice.domain.model")
    hibernateProperties:
        hibernate.dialect = PostgreSQL95Dialect
        hibernate.hbm2ddl.auto = validate
        hibernate.show_sql = false
        hibernate.format_sql = false
        hibernate.default_schema = public

@Bean HibernateTransactionManager
    setSessionFactory(sessionFactory)
```

### 6.5 IntegrationConfig.java

```
@Configuration
@PropertySource("classpath:sabre.properties")
@Bean SabreOAuthProvider   (sabre.clientId, sabre.clientSecret, sabre.tokenUrl)
@Bean SabreHttpClient      (sabre.baseUrl, connectionTimeout, readTimeout)
@Bean GdsFlightSearchPort  → SabreFlightSearchAdapter(sabreHttpClient, sabreOAuthProvider)
@Bean GdsFlightCheckPort   → SabreFlightCheckAdapter(sabreHttpClient, sabreOAuthProvider)
```

Both adapters receive the same infrastructure dependencies (`SabreHttpClient`, `SabreOAuthProvider`). If in the future Amadeus is connected, it is enough to change the two `@Bean` methods to return `AmadeusFlightSearchAdapter` and `AmadeusFlightCheckAdapter` — the services that consume `GdsFlightSearchPort` and `GdsFlightCheckPort` are not touched.

### 6.6 WebMvcConfig.java (Web Context)

```
@Configuration
@EnableWebMvc
implements WebMvcConfigurer

configureMessageConverters: MappingJackson2HttpMessageConverter (ObjectMapper with ISO dates)
addResourceHandlers: /static/** → classpath:/static/
addCorsMappings: /api/** allows localhost:8080 (dev)
configureDefaultServletHandling: enables DefaultServletHandler for index.html
@Bean ExceptionTranslationHandlerAdvice (@ControllerAdvice)
```

---

## 7. Hibernate 5 Configuration

### 7.1 Mapping Strategy

- JPA annotations on domain entities (`@Entity`, `@Table`, `@Column`, etc.)
- Hibernate 5 reads them as JPA provider, but access is via `Session` (native Hibernate API), not `EntityManager`
- Naming: table `traveller_profile`, columns `snake_case`
- ID generation strategy: `@GeneratedValue(strategy = GenerationType.SEQUENCE)` with native PostgreSQL sequences (more efficient than IDENTITY for batch inserts)

### 7.2 Key Hibernate Properties

| Property | Value | Reason |
|---|---|---|
| `hibernate.dialect` | `PostgreSQL95Dialect` | Enables PG-specific features (JSON, arrays, `returning`) |
| `hibernate.hbm2ddl.auto` | `validate` | Does not touch the schema in production; fails if there is inconsistency between entities and DB |
| `hibernate.show_sql` | `false` | Activate only in debug with Logback |
| `hibernate.jdbc.batch_size` | `25` | Grouping of inserts/updates |
| `hibernate.connection.provider_class` | (delegated to HikariCP via DataSource) | HikariCP manages the pool, not Hibernate |
| `hibernate.current_session_context_class` | `org.springframework.orm.hibernate5.SpringSessionContext` | Spring manages the Session lifecycle |

### 7.3 Session Management

Spring opens one Hibernate `Session` per transaction. DAOs obtain the session with `sessionFactory.getCurrentSession()` — works because `current_session_context_class` points to Spring. There is no `beginTransaction()`/`commit()` code in the DAOs; that is done by `HibernateTransactionManager` via AOP (`@Transactional`).

---

## 8. Tomcat / web.xml Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0">

  <!-- Root Application Context: Services, DAOs, Hibernate, Sabre -->
  <context-param>
    <param-name>contextClass</param-name>
    <param-value>
      org.springframework.web.context.AnnotationConfigWebApplicationContext
    </param-value>
  </context-param>
  <context-param>
    <param-name>contextConfigLocation</param-name>
    <param-value>com.jcarranza.minimidoffice.web.config.AppConfig</param-value>
  </context-param>
  <listener>
    <listener-class>
      org.springframework.web.context.ContextLoaderListener
    </listener-class>
  </listener>

  <!-- Encoding filter: always before Spring MVC -->
  <filter>
    <filter-name>encodingFilter</filter-name>
    <filter-class>
      org.springframework.web.filter.CharacterEncodingFilter
    </filter-class>
    <init-param><param-name>encoding</param-name><param-value>UTF-8</param-value></init-param>
    <init-param><param-name>forceEncoding</param-name><param-value>true</param-value></init-param>
  </filter>
  <filter-mapping>
    <filter-name>encodingFilter</filter-name>
    <url-pattern>/*</url-pattern>
  </filter-mapping>

  <!-- Spring MVC DispatcherServlet: only /api/** -->
  <servlet>
    <servlet-name>dispatcher</servlet-name>
    <servlet-class>
      org.springframework.web.servlet.DispatcherServlet
    </servlet-class>
    <init-param>
      <param-name>contextClass</param-name>
      <param-value>
        org.springframework.web.context.AnnotationConfigWebApplicationContext
      </param-value>
    </init-param>
    <init-param>
      <param-name>contextConfigLocation</param-name>
      <param-value>com.jcarranza.minimidoffice.web.config.WebMvcConfig</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
  </servlet>
  <servlet-mapping>
    <servlet-name>dispatcher</servlet-name>
    <url-pattern>/api/*</url-pattern>
  </servlet-mapping>

  <!-- Static resources and SPA: Tomcat DefaultServlet serves everything else -->
  <!-- index.html → GET / or GET /index.html, served directly by Tomcat -->
  <welcome-file-list>
    <welcome-file>index.html</welcome-file>
  </welcome-file-list>

</web-app>
```

**Why `/api/*` and not `/*`**: By mapping the DispatcherServlet only to `/api/*`, Tomcat serves `index.html` and static resources from `/static/**` directly without going through Spring. This avoids configuring a complex `ResourceHttpRequestHandler` and is closer to the behavior of the real system.

---

## 9. External Properties

Two configuration files outside the WAR (in `$CATALINA_HOME/conf/` or passed as `-D` JVM args):

```
# db.properties
db.url=jdbc:postgresql://localhost:5432/miniumbrella
db.username=miniuser
db.password=...
db.pool.maxSize=10

# sabre.properties
sabre.baseUrl=https://api.cert.platform.sabre.com
sabre.tokenUrl=https://api.cert.platform.sabre.com/v2/auth/token
sabre.clientId=...
sabre.clientSecret=...
sabre.token.ttlSeconds=3600
```

Loaded in Spring with `@PropertySource` + `Environment` or `@Value`. Never hardcoded in the WAR.

---

## 10. Architectural Decisions and Their Justifications

| Decision | Discarded Alternative | Reason |
|---|---|---|
| Maven multi-module with 5 modules | Single project (flat) | Layer boundaries enforced at compile-time; you cannot call the DAO from the Controller |
| DAO pattern with native `Session` | Spring Data JPA | Aligned with the real stack; demonstrates Hibernate knowledge without JPA magic |
| `GdsFlightSearchPort` interface | Call Sabre directly from Service | Allows replacing Sabre with Amadeus/Galileo without touching the services; anti-corruption |
| Two separate ports: `GdsFlightSearchPort` + `GdsFlightCheckPort` | A single port with two methods | Search and Check have distinct contracts: Search is exploratory and accepts cache; Check is point-in-time and demands real-time data. Merging them in one port hides that semantic difference and forces the Amadeus adapter to implement operations it may not use |
| Flight Check before `SEARCHED→RESERVED` with `REQUIRES_NEW` propagation | Keep DB transaction open during the check | An HTTP call to Sabre can take seconds; blocking the DB connection for that time exhausts the pool under moderate load |
| Root Context + Web Context separated | A single context | Classic Spring MVC pattern; `@Transactional` works correctly with `HibernateTransactionManager` |
| `hbm2ddl.auto=validate` | `update` | Does not modify the schema in production; inconsistency errors visible at startup |
| DispatcherServlet at `/api/*` | At `/*` | Tomcat serves statics directly; SPA without additional Spring configuration |
| JavaConfig + hybrid web.xml | XML only / JavaConfig only | Demonstrates mastery of both worlds; legacy teams that migrate use exactly this |
| HikariCP for connection pool | C3P0 / DBCP2 | HikariCP is the current standard; C3P0 was the historical choice in Hibernate 3/4 |

---

## 11. What is NOT in this Architecture (and Why)

- **No Spring Boot**: no `@SpringBootApplication`, no autoconfiguration. Every bean is declared explicitly.
- **No microservices**: one WAR, one Tomcat, one database. The business does not require it.
- **No ORM abstraction**: DAOs know `Session`, not `EntityManager`. This demonstrates direct Hibernate knowledge: `session.get()` vs `session.load()`, L1/L2 caching, and so on.
- **No React/Angular/Vue**: only jQuery + Handlebars — a classic stack common in mid-office and back-office Java applications.
- **No Docker/Kubernetes**: classic WAR deployment on Tomcat, like the real system.
- **No Flyway/Liquibase (for now)**: SQL scripts are managed manually (Phase 2). Flyway could be added in a later iteration.

---

*Next phase → docs/02-data-model.md: Hibernate entities, DAOs, PostgreSQL scripts.*
