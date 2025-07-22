FROM gradle:8.5-jdk21 AS build

ENV APP_NAME=stereov-io
ENV SUPPORT_EMAIL=contact@stereov.io
ENV BACKEND_BASE_URL=http://localhost:8000
ENV BACKEND_PORT=8000
ENV SECURE=true
ENV DEV_MODE=false
ENV CREATE_ROOT_USER=false
ENV ROOT_EMAIL=root@email.com
ENV ROOT_PASSWORD=root-password
ENV UI_BASE_URL=http://localhost:4200
ENV MONGO_DB_HOST=localhost
ENV MONGO_DB_PORT=27017
ENV MONGO_DB_DATABASE_NAME=stereov-io
ENV MONGO_DB_USERNAME=root
ENV MONGO_DB_PASSWORD=password
ENV MONGO_DB_SSL_ENABLED=false
ENV REDIS_HOST=localhost
ENV REDIS_PORT=6479
ENV REDIS_PASSWORD=password
ENV REDIS_SSL_ENABLED=true
ENV REDIS_DATABASE=0
ENV REDIS_TIMEOUT=5000ms
ENV S3_ACCESS_KEY=access-key
ENV S3_SECRET_KEY=secret-key
ENV S3_DOMAIN=domain
ENV S3_SCHEME=https
ENV S3_BUCKET=bucket
ENV SECRET_STORE_TYPE=local
ENV SECRET_STORE_DIRECTORY=./.data/secrets
ENV ENABLE_EMAIL_VERIFICATION=true
ENV MAIL_HOST=mail.host.de
ENV MAIL_PORT=587
ENV MAIL_EMAIL=noreply@mail.de
ENV MAIL_USERNAME=noreply@mail.de
ENV MAIL_PASSWORD=mail-password
ENV MAIL_TRANSPORT_PROTOCOL=smtp
ENV MAIL_SMTP_AUTH=true
ENV MAIL_SMTP_STARTTLS=true
ENV MAIL_DEBUG=false
ENV EMAIL_VERIFICATION_PATH=/auth/verify-email
ENV PASSWORD_RESET_PATH=/auth/password-reset
ENV LOG_LEVEL=DEBUG
ENV LOG_PATH=logs

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

FROM eclipse-temurin:21-jdk AS runtime

WORKDIR /app

COPY --from=build /app/apps/stereov-io/build/libs/*.jar app.jar

EXPOSE $BACKEND_PORT

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl --silent --fail http://localhost:$BACKEND_PORT/actuator/health || exit 1
