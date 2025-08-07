FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY ./gradle/ ./gradle/
COPY ./settings.gradle.kts /app/settings.gradle.kts
COPY ./gradlew /app/gradlew
COPY ./apps/demo/ ./apps/demo/
COPY ./libs/core/ ./libs/core/

RUN chmod +x ./gradlew && \
    ./gradlew :apps:demo:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jdk AS runtime

WORKDIR /app

COPY --from=build /app/apps/demo/build/libs/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl --silent --fail http://localhost:8000/actuator/health || exit 1
