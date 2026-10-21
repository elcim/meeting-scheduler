# ---- build: compile and package the application ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first, so they are cached until the pom changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package \
    && java -Djarmode=tools -jar target/meeting-scheduler-*.jar extract --layers --destination /build/layers

# ---- runtime: JRE only, unprivileged user ----
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home scheduler
WORKDIR /app

# Layers ordered from least to most frequently changing.
COPY --from=build /build/layers/dependencies/ ./
COPY --from=build /build/layers/spring-boot-loader/ ./
COPY --from=build /build/layers/snapshot-dependencies/ ./
COPY --from=build /build/layers/application/ ./

# Default port inside the container when the image runs on its own. docker-compose.yml passes its own APP_PORT
# (also defaulting to 8088), which takes precedence over this value.
ENV APP_PORT=8088

USER scheduler
EXPOSE ${APP_PORT}
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fs http://localhost:${APP_PORT}/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "meeting-scheduler-0.0.1-SNAPSHOT.jar"]
