# Quarkus Template App

A **production-quality Quarkus reference template** demonstrating clean architecture, full CRUD REST API with PostgreSQL, external API integration, Swagger UI, validation, and observability.

---

## Table of Contents

- [Project Overview](#project-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [API Endpoints](#api-endpoints)
- [External API Integration](#external-api-integration)
- [Swagger UI](#swagger-ui)
- [Health & Metrics](#health--metrics)
- [Running Tests](#running-tests)
- [Building for Production](#building-for-production)

---

## Project Overview

This template demonstrates:

- **Clean architecture**: `Resource → Service → Repository` layering
- **Full CRUD REST API** with proper HTTP status codes and paginated responses
- **PostgreSQL** via Hibernate ORM with Panache
- **MapStruct** for type-safe DTO mapping
- **Bean Validation** with structured field-level error responses
- **Global exception handling** with consistent `ErrorResponse` contract
- **External REST Client** calling JSONPlaceholder and returning transformed summaries
- **OpenAPI / Swagger UI** with full endpoint documentation
- **SmallRye Health** and **Micrometer + Prometheus** metrics out of the box
- **Profile-based config** (dev, test, prod) via `application.properties`

---

## Technology Stack

| Component        | Technology                          |
|-----------------|--------------------------------------|
| Framework        | Quarkus 3.17.x                      |
| Language         | Java 21                             |
| ORM              | Hibernate ORM with Panache          |
| Database         | PostgreSQL (H2 for tests)           |
| REST             | Quarkus REST (RESTEasy Reactive)    |
| REST Client      | Quarkus REST Client                 |
| DTO Mapping      | MapStruct 1.6                       |
| Validation       | Hibernate Validator (Jakarta)       |
| API Docs         | SmallRye OpenAPI + Swagger UI       |
| Health           | SmallRye Health                     |
| Metrics          | Micrometer + Prometheus             |
| Build            | Maven 3.9+                         |
| Tests            | JUnit 5 + RestAssured + Mockito     |

---

## Project Structure

```
quarkus-template-app/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/template/quarkus/
    │   │   ├── config/
    │   │   │   └── OpenApiConfig.java          # OpenAPI metadata
    │   │   ├── client/
    │   │   │   └── JsonPlaceholderClient.java  # External REST client interface
    │   │   ├── dto/
    │   │   │   ├── ProductRequest.java         # Inbound payload (create/update)
    │   │   │   ├── ProductResponse.java        # Outbound product data
    │   │   │   ├── PagedResponse.java          # Generic pagination wrapper
    │   │   │   ├── ExternalPostDto.java        # Raw external API model
    │   │   │   └── ExternalPostSummary.java    # Transformed external post
    │   │   ├── entity/
    │   │   │   └── Product.java                # JPA entity (PanacheEntity)
    │   │   ├── exception/
    │   │   │   ├── ProductNotFoundException.java
    │   │   │   ├── DuplicateSkuException.java
    │   │   │   ├── ErrorResponse.java          # Standard error body
    │   │   │   └── GlobalExceptionHandler.java # JAX-RS ExceptionMapper
    │   │   ├── mapper/
    │   │   │   └── ProductMapper.java          # MapStruct mapper
    │   │   ├── repository/
    │   │   │   └── ProductRepository.java      # Panache repository
    │   │   ├── resource/
    │   │   │   ├── ProductResource.java        # /api/products endpoints
    │   │   │   └── ExternalApiResource.java    # /api/external/posts endpoints
    │   │   └── service/
    │   │       ├── ProductService.java
    │   │       └── ExternalApiService.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/template/quarkus/
            ├── resource/
            │   └── ProductResourceTest.java    # Integration tests (RestAssured)
            └── service/
                └── ProductServiceTest.java     # Unit tests (Mockito)
```

---

## Prerequisites

- **Java 21+** — `java -version`
- **Maven 3.9+** — `mvn -version`
- **Docker** (optional, for PostgreSQL via Docker)
- **PostgreSQL 14+** (if running without Docker)

---

## Configuration

All configuration lives in `src/main/resources/application.properties`.  
Override with **environment variables** in production:

| Environment Variable   | Default                                       | Description            |
|------------------------|-----------------------------------------------|------------------------|
| `DB_USERNAME`          | `postgres`                                    | Database username      |
| `DB_PASSWORD`          | `postgres`                                    | Database password      |
| `DB_URL`               | `jdbc:postgresql://localhost:5432/quarkus_template` | JDBC URL         |
| `JSONPLACEHOLDER_URL`  | `https://jsonplaceholder.typicode.com`        | External API base URL  |

### Start PostgreSQL with Docker

```bash
docker run --name quarkus-pg \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=quarkus_template \
  -p 5432:5432 \
  -d postgres:16-alpine
```

---

## How to Run

### Development mode (live reload)

```bash
./mvnw quarkus:dev
```

The app starts at **http://localhost:8080**  
Swagger UI: **http://localhost:8080/swagger-ui**  
Dev UI: **http://localhost:8080/q/dev**

### With custom DB config

```bash
DB_URL=jdbc:postgresql://myhost:5432/mydb \
DB_USERNAME=myuser \
DB_PASSWORD=mypassword \
./mvnw quarkus:dev
```

### Production JAR

```bash
./mvnw clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

---

## API Endpoints

### Products — `/api/products`

#### GET /api/products — List all (paginated)

```bash
curl -s "http://localhost:8080/api/products?page=0&size=10" | jq .
```

**Response 200:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Wireless Headphones",
      "description": "Noise-cancelling Bluetooth headphones",
      "sku": "WH-1000XM5",
      "price": 349.99,
      "stockQuantity": 150,
      "category": "Electronics",
      "active": true,
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

#### GET /api/products/{id} — Get by ID

```bash
curl -s http://localhost:8080/api/products/1 | jq .
```

**Response 200:**
```json
{
  "id": 1,
  "name": "Wireless Headphones",
  "sku": "WH-1000XM5",
  "price": 349.99,
  "stockQuantity": 150,
  "category": "Electronics",
  "active": true,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

**Response 404:**
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Product not found with ID: 99",
  "path": "/api/products/99",
  "timestamp": "2025-01-15T10:31:00"
}
```

---

#### GET /api/products/category/{category} — Get by category

```bash
curl -s http://localhost:8080/api/products/category/Electronics | jq .
```

---

#### GET /api/products/search?q=keyword — Search by name

```bash
curl -s "http://localhost:8080/api/products/search?q=headphone" | jq .
```

---

#### POST /api/products — Create a product

```bash
curl -s -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Headphones",
    "description": "Noise-cancelling Bluetooth headphones",
    "sku": "WH-1000XM5",
    "price": 349.99,
    "stockQuantity": 150,
    "category": "Electronics",
    "active": true
  }' | jq .
```

**Response 201:**
```json
{
  "id": 1,
  "name": "Wireless Headphones",
  "sku": "WH-1000XM5",
  "price": 349.99,
  "stockQuantity": 150,
  "category": "Electronics",
  "active": true,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

**Response 400 (validation error):**
```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/products",
  "timestamp": "2025-01-15T10:30:00",
  "fieldErrors": [
    {
      "field": "price",
      "rejectedValue": -5.0,
      "message": "Price must be greater than 0"
    }
  ]
}
```

**Response 409 (duplicate SKU):**
```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "A product with SKU 'WH-1000XM5' already exists",
  "path": "/api/products",
  "timestamp": "2025-01-15T10:30:00"
}
```

---

#### PUT /api/products/{id} — Update a product

```bash
curl -s -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Wireless Headphones Pro",
    "description": "Updated description",
    "sku": "WH-1000XM5",
    "price": 399.99,
    "stockQuantity": 120,
    "category": "Electronics",
    "active": true
  }' | jq .
```

---

#### DELETE /api/products/{id} — Soft-delete a product

```bash
curl -s -X DELETE http://localhost:8080/api/products/1 -v
# HTTP/1.1 204 No Content
```

> Products are **soft-deleted** (marked `active = false`). The record stays in the DB.

---

### External API — `/api/external/posts`

#### GET /api/external/posts — All posts (transformed)

```bash
curl -s http://localhost:8080/api/external/posts | jq '.[0]'
```

**Response 200:**
```json
{
  "id": 1,
  "authorId": 1,
  "title": "Sunt Aut Facere Repellat Provident Occaecati",
  "excerpt": "quia et suscipit suscipit recusandae consequuntur expedita et...",
  "estimatedReadSeconds": 18
}
```

---

#### GET /api/external/posts/{id} — Single post (transformed)

```bash
curl -s http://localhost:8080/api/external/posts/1 | jq .
```

---

#### GET /api/external/posts/user/{userId} — Posts by user

```bash
curl -s http://localhost:8080/api/external/posts/user/1 | jq .
```

---

## External API Integration

The app integrates with [jsonplaceholder.typicode.com](https://jsonplaceholder.typicode.com) using a **Quarkus REST Client**.

**How it works:**

1. `JsonPlaceholderClient` (interface annotated with `@RegisterRestClient`) declares the external API contract.
2. Quarkus generates a type-safe HTTP client at build time.
3. `ExternalApiService` injects the client via `@RestClient`, calls the external API, and transforms raw `ExternalPostDto` objects into `ExternalPostSummary` (title-cased, truncated excerpt, estimated read time).
4. `ExternalApiResource` exposes the results via REST endpoints.

**Transformation applied:**
- Title → Title Case
- Body → truncated to 100 characters (excerpt)
- Word count → estimated read time in seconds (at 180 wpm)

**Configure base URL:**
```properties
# application.properties
quarkus.rest-client."com.template.quarkus.client.JsonPlaceholderClient".url=https://jsonplaceholder.typicode.com
```
Or via environment variable:
```bash
JSONPLACEHOLDER_URL=https://jsonplaceholder.typicode.com ./mvnw quarkus:dev
```

---

## Swagger UI

Swagger UI is always enabled and available at:

```
http://localhost:8080/swagger-ui
```

OpenAPI JSON spec:
```
http://localhost:8080/q/openapi
```

All endpoints include:
- Summary and description
- Parameter documentation with examples
- Request/response schema via `@Schema`
- All possible HTTP response codes documented

---

## Health & Metrics

### Health checks

```bash
# Overall health
curl http://localhost:8080/q/health

# Liveness
curl http://localhost:8080/q/health/live

# Readiness (checks DB connectivity)
curl http://localhost:8080/q/health/ready
```

### Prometheus metrics

```bash
curl http://localhost:8080/q/metrics
```

Includes HTTP request duration, JVM metrics, datasource pool metrics, and more.

---

## Running Tests

Tests use **H2 in-memory database** (configured automatically via `%test` profile) — no PostgreSQL required.

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=ProductResourceTest

# Run with coverage report
./mvnw test jacoco:report
```

**Test types:**
- `ProductResourceTest` — full integration tests via RestAssured hitting real HTTP endpoints
- `ProductServiceTest` — unit tests with Mockito mocking the repository layer

---

## Building for Production

### JVM mode (standard JAR)

```bash
./mvnw clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

### Native binary (GraalVM)

```bash
# Requires GraalVM with native-image installed
./mvnw clean package -Pnative -DskipTests
./target/quarkus-template-app-1.0.0-SNAPSHOT-runner
```

### Docker

```dockerfile
# Dockerfile.jvm (auto-generated by Quarkus in src/main/docker/)
docker build -f src/main/docker/Dockerfile.jvm -t quarkus-template-app .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_template \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  quarkus-template-app
```

---

## Environment Variables Reference

| Variable              | Default                                          | Required |
|-----------------------|--------------------------------------------------|----------|
| `DB_URL`              | `jdbc:postgresql://localhost:5432/quarkus_template` | Yes (prod) |
| `DB_USERNAME`         | `postgres`                                       | Yes (prod) |
| `DB_PASSWORD`         | `postgres`                                       | Yes (prod) |
| `JSONPLACEHOLDER_URL` | `https://jsonplaceholder.typicode.com`           | No       |
| `QUARKUS_HTTP_PORT`   | `8080`                                           | No       |
