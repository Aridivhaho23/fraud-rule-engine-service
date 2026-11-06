# syntax=docker/dockerfile:1
# Multi-stage build: compile with Maven + JDK, then ship only a small JRE and the jar.

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Copy the pom first so Docker caches the dependency download until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Optional stage: run the tests ----------
#   docker build --target test -t fraud-rule-engine-tests .
FROM build AS test
RUN mvn -B test

# ---------- Stage 2: runtime (default) ----------
FROM eclipse-temurin:21-jre AS runtime
# Never run as root inside the container.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/target/app.jar app.jar
USER app
EXPOSE 8080
# Default = in-memory H2 so "docker run" works with nothing else. Compose switches to PostgreSQL.
ENV SPRING_PROFILES_ACTIVE=h2 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
# "exec" makes Java PID 1 so it receives SIGTERM and shuts down gracefully.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
