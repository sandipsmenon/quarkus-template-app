# =============================================================================
# Stage 1 — Build
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copy dependency descriptors first — layer-cached until pom.xml changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q -B 2>/dev/null || true

# Copy source and build (skip tests — run them in CI before image build)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -q -B

# =============================================================================
# Stage 2 — Runtime
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL org.opencontainers.image.title="quarkus-template-app" \
      org.opencontainers.image.description="Production-quality Quarkus reference template" \
      org.opencontainers.image.version="1.0.0"

# Install Maven in the build stage — runtime image stays lean
# Non-root user for security compliance (Kubernetes / OpenShift)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the Quarkus fast-jar layout from the build stage
COPY --from=build --chown=appuser:appgroup /build/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=appuser:appgroup /build/target/quarkus-app/*.jar ./
COPY --from=build --chown=appuser:appgroup /build/target/quarkus-app/app/ ./app/
COPY --from=build --chown=appuser:appgroup /build/target/quarkus-app/quarkus/ ./quarkus/

USER appuser

EXPOSE 8080

# =============================================================================
# Runtime environment — all secrets injected at container start time
# =============================================================================
ENV DB_URL=jdbc:postgresql://postgres:5432/quarkus_template \
    DB_USERNAME=postgres \
    DB_PASSWORD=postgres \
    JSONPLACEHOLDER_URL=https://jsonplaceholder.typicode.com \
    JAVA_OPTS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/q/health/live || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]
