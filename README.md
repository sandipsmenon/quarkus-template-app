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
- [Docker](#docker)
- [Docker Compose](#docker-compose)
- [Kubernetes / OpenShift](#kubernetes--openshift)

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

---

## Environment Variables Reference

| Variable              | Default                                              | Required in prod |
|-----------------------|------------------------------------------------------|------------------|
| `DB_URL`              | `jdbc:postgresql://localhost:5432/quarkus_template`  | Yes              |
| `DB_USERNAME`         | `postgres`                                           | Yes              |
| `DB_PASSWORD`         | `postgres`                                           | Yes              |
| `JSONPLACEHOLDER_URL` | `https://jsonplaceholder.typicode.com`               | No               |
| `APP_PORT`            | `8080`                                               | No               |

---

## Docker

The project ships a **multi-stage `Dockerfile`**:

| Stage     | Base image                    | Purpose                              |
|-----------|-------------------------------|--------------------------------------|
| `build`   | `eclipse-temurin:21-jdk-alpine` | Compiles sources, produces fast-jar |
| `runtime` | `eclipse-temurin:21-jre-alpine` | Lean image (~200 MB), non-root user  |

The Maven dependency layer is cached separately from the source layer, so rebuilds after a code-only change are fast.

### Build the JAR

```bash
./mvnw clean package -DskipTests
```

### Build the Docker image

```bash
docker build -t quarkus-template-app:latest .
```

Tag for a registry:

```bash
docker build -t ghcr.io/your-org/quarkus-template-app:1.0.0 .
docker push ghcr.io/your-org/quarkus-template-app:1.0.0
```

### Run the container (standalone)

Requires a reachable PostgreSQL instance. Use `host.docker.internal` to reach one running on the host machine.

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_template \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  quarkus-template-app:latest
```

Override the external API base URL if needed:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_template \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e JSONPLACEHOLDER_URL=https://jsonplaceholder.typicode.com \
  quarkus-template-app:latest
```

App is available at **http://localhost:8080** — Swagger UI at **http://localhost:8080/swagger-ui**.

### Runtime JVM tuning

Pass extra JVM flags via `JAVA_OPTS`:

```bash
docker run --rm -p 8080:8080 \
  -e JAVA_OPTS="-Xms128m -Xmx512m -Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_template \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  quarkus-template-app:latest
```

---

## Docker Compose

`docker-compose.yml` starts **PostgreSQL + the Quarkus app** in one command. The app waits for Postgres to pass its healthcheck before starting.

### Start the full stack

```bash
docker compose up --build
```

### Start in the background

```bash
docker compose up --build -d
docker compose logs -f app       # tail app logs
docker compose logs -f postgres  # tail DB logs
```

### Verify health

```bash
# App readiness
curl http://localhost:8080/q/health/ready

# App liveness
curl http://localhost:8080/q/health/live

# Postgres connectivity (inside the postgres container)
docker exec quarkus-postgres pg_isready -U postgres
```

### Use a `.env` file for secrets (recommended locally)

Create a `.env` file in the project root (it is git-ignored):

```dotenv
DB_USERNAME=postgres
DB_PASSWORD=supersecret
APP_PORT=8080
```

Docker Compose picks it up automatically:

```bash
docker compose up --build
```

### Tear down

```bash
docker compose down          # stop containers, keep the data volume
docker compose down -v       # stop containers AND delete the postgres_data volume
```

---

## Kubernetes / OpenShift

The container image is Kubernetes-ready:

- Runs as a **non-root user** (`appuser` / UID auto-assigned) — compatible with restricted SCCs on OpenShift
- All config injected via **environment variables** — map to `ConfigMap` / `Secret`
- Exposes `/q/health/live` and `/q/health/ready` for liveness and readiness probes
- Exposes `/q/metrics` for Prometheus scraping

### Minimal Deployment manifest

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: quarkus-template-app
spec:
  replicas: 2
  selector:
    matchLabels:
      app: quarkus-template-app
  template:
    metadata:
      labels:
        app: quarkus-template-app
    spec:
      containers:
        - name: app
          image: ghcr.io/your-org/quarkus-template-app:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: url
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secret
                  key: password
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /q/health/ready
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 10
            failureThreshold: 3
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: quarkus-template-app
spec:
  selector:
    app: quarkus-template-app
  ports:
    - port: 80
      targetPort: 8080
```

### Create the DB secret

```bash
kubectl create secret generic db-secret \
  --from-literal=url=jdbc:postgresql://postgres-svc:5432/quarkus_template \
  --from-literal=username=postgres \
  --from-literal=password=supersecret
```

### Prometheus scraping annotation

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/q/metrics"
    prometheus.io/port: "8080"
```
