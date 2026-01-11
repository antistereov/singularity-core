FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY ./gradle/ ./gradle/
COPY ./settings.gradle.kts /app/settings.gradle.kts
COPY ./build.gradle.kts /app/build.gradle.kts
COPY ./gradlew /app/gradlew
COPY ./apps/ /app/apps/
COPY ./libs/ /app/libs/

RUN chmod +x ./gradlew && \
    ./gradlew :apps:stereov-io:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jdk AS runtime

WORKDIR /app

COPY --from=build /app/apps/stereov-io/build/libs/*.jar app.jar

EXPOSE $BACKEND_PORT

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl --silent --fail http://localhost:$BACKEND_PORT/actuator/health || exit 1
