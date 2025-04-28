FROM gradle:8.5-jdk21 AS build

WORKDIR /app

COPY ./gradle/ ./gradle/
COPY ./settings.gradle.kts /app/settings.gradle.kts
COPY ./gradlew /app/gradlew
COPY ./apps/stereov-io/ ./apps/stereov-io/
COPY ./libs/core/ ./libs/core/

ARG GPR_USER=""
ARG GPR_KEY=""

RUN chmod +x ./gradlew && \
    ./gradlew :apps:stereov-io:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=build /app/apps/stereov-io/build/libs/*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
