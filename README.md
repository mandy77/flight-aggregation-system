# Flight Aggregation & Cache Optimization System

OTA multi-GDS flight aggregator with **Clean Architecture**, resilient concurrent ingestion, 5 % markup, deduplication, and keyset-paginated search.

**Backend: Java 21 + Spring Boot 3.4** · Frontend: React + Vite

## Quick Start

```bash
docker compose up --build
```

| Service   | URL                          |
|-----------|------------------------------|
| Dashboard | http://localhost:3000        |
| API       | http://localhost:8000        |
| Health    | http://localhost:8000/health |

1. Click **Ingest from GDS** on the dashboard (or `POST /ingest`).
2. Search / filter / page through results.

## Architecture

See [SOLUTION.md](./SOLUTION.md) for architectural decisions, pagination justification, AI process notes, limitations and trade-offs.

## Tests

```bash
cd backend
mvn test
```
