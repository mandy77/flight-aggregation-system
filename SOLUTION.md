# Flight Aggregation & Cache Optimization System — SOLUTION

## 1. Executive Summary

This project implements a production-oriented **Flight Aggregation & Cache Optimization System** for an Online Travel Agency (OTA). It concurrently ingests itineraries from three heterogeneous mock GDS providers (JSON, nested XML, CSV streams), normalises them into a unified domain model, deduplicates identical journeys, applies a 5 % OTA markup, filters invalid connections, persists the result set, and exposes a high-performance search API with **keyset pagination** together with a React dashboard.

**Backend stack: Java 21 + Spring Boot 3.4** (Clean Architecture / DDD).

The complete stack starts with a single command:

```bash
docker compose up --build
```

- Frontend: http://localhost:3000  
- Backend API: http://localhost:8000  
- Health: http://localhost:8000/health  

---

## 2. Architectural Decisions

### 2.1 Clean Architecture / Domain-Driven Design

| Layer              | Responsibility                                      | Location                                      |
|--------------------|-----------------------------------------------------|-----------------------------------------------|
| **Domain**         | Pure entities, value objects, domain services       | `com.ota.flights.domain`                      |
| **Application**    | Use-cases that orchestrate domain + ports           | `com.ota.flights.application`                 |
| **Infrastructure** | Adapters (providers, JPA/Postgres, Redis)           | `com.ota.flights.infrastructure`              |
| **Interfaces**     | Spring MVC REST controllers & DTOs                  | `com.ota.flights.interfaces.rest`             |

- Domain models (`Itinerary`, `FlightSegment`, `PricedItinerary`, `SearchCriteria`) contain **zero** Spring/JPA annotations.
- Ports are plain Java interfaces (`FlightProviderPort`, `ItineraryRepositoryPort`, `CachePort`).
- Spring beans wire adapters to ports in `AppConfig`.

### 2.2 Multi-Provider Ingestion & Resiliency

- Three independent provider adapters:
  - **Mock-Alpha** – raw JSON lists (100–800 ms latency, ~7 % failures)
  - **Mock-Beta** – deeply nested XML (300–2500 ms, ~10 % failures)
  - **Mock-Gamma** – CSV string streams (200–5000 ms, ~9 % failures)
- `IngestFlightsUseCase` launches all three with `CompletableFuture` + **virtual threads** (`Executors.newVirtualThreadPerTaskExecutor()`). A failure in one provider is captured; the others continue.

### 2.3 Aggregation & Deduplication Pipeline

1. **Validation** – discard dead legs and connections with < 45 min layover.
2. **Deduplication** – group by canonical `identityKey` (airline + flight number + origin/destination + departure of every segment). Keep the cheapest base price.
3. **Pricing** – apply a fixed 5 % OTA operational markup (`BigDecimal`, scale 2).
4. **Sort** – ascending final price.

### 2.4 Data Modelling for High Volume (100 k – 1 M rows)

- JPA entity `ItineraryEntity` mapped to table `itineraries` with denormalised filter columns + JSONB `segments_json`.
- Indexes on `(origin, destination, final_price, id)`, GIN-style array on `carriers`, and `(final_price, id)`.
- Full-refresh strategy on each `/ingest` (TRUNCATE + bulk save).

### 2.5 Pagination Choice: Keyset vs Offset

**Keyset (seek) pagination** was chosen.

| Criterion                    | Offset                     | Keyset                                      |
|-----------------------------|----------------------------|---------------------------------------------|
| Deep-page cost              | O(n)                       | O(log n) – index seek                       |
| Consistency under writes    | Duplicates / gaps          | Stable                                      |
| Suitability for 1 M rows    | Poor beyond ~10 k          | Excellent                                   |
| Client complexity           | Page number                | Opaque cursor (Base64 of `price\|id`)       |

Native SQL predicate:

```sql
WHERE … AND (final_price > :cursorPrice OR (final_price = :cursorPrice AND id > :cursorId))
ORDER BY final_price ASC, id ASC
LIMIT :limit+1
```

### 2.6 Caching

- Redis (`StringRedisTemplate`) invalidates `search:*` keys after a successful ingest.

### 2.7 Frontend

- React 18 + Vite dashboard, served by Nginx, proxying `/api/*` to the Spring Boot backend.

---

## 3. AI Tools & Engineering Process

AI Agents Used **Grok 4.5** (xAI) and CoPilot

1. Set Up Backend Layer Java 21 + Spring Boot 3.4
2. Virtual threads for concurrent provider calls.
3. Native SQL keyset queries via Spring Data JPA.
4. Create Unit Tests

---

## 4. System Limitations & Trade-offs

| Limitation / Trade-off                       | Rationale / Future work                                      |
|----------------------------------------------|--------------------------------------------------------------|
| Full-refresh on every ingest                 | Simple correctness; replace with CDC / upsert later          |
| Synthetic data only                          | Real GDS credentials out of scope                            |
| ~400–500 rows per provider per call          | Demonstrates format diversity; scale generators for load tests |
| No authentication / rate-limiting            | Demo focus                                                   |
| Approximate counts only                      | Exact COUNT is expensive at 1 M filtered rows                |
| Markup is a fixed 5 %                        | Real OTAs use dynamic commission rules                       |
| Hibernate `ddl-auto: update`                 | Fine for demo; use Flyway/Liquibase in production            |

---

## 5. How to Run & Verify

```bash
cd flight-aggregation-system
docker compose up --build
```

1. Open http://localhost:3000  
2. Click **Ingest from GDS**.  
3. Search / filter / page.  

Unit tests (domain):

```bash
cd backend
mvn test
```

---

## 6. File Map

```
flight-aggregation-system/
├── docker-compose.yml
├── SOLUTION.md
├── README.md
├── backend/                    # Java 21 + Spring Boot 3.4
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/ota/flights/
│       ├── domain/             # pure business rules
│       ├── application/        # use-cases
│       ├── infrastructure/     # providers, JPA, Redis
│       ├── interfaces/rest/    # controllers + DTOs
│       └── config/
└── frontend/                   # React
```

---

*Built with Clean Architecture, keyset pagination, virtual-thread concurrent ingestion, and resilient multi-provider aggregation.*
