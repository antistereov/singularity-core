# **Singularity Core**

[![Maven Central](https://img.shields.io/maven-central/v/io.stereov.singularity/core.svg)](https://central.sonatype.com/artifact/io.stereov.singularity/core)

A production-ready Spring Web baseline with JWT authentication, 2FA, MongoDB, Redis, rate limiting & async processing. 🚀
Save time, ensure consistency, and focus on features — whether you're building an API, microservice, or full-stack app.

Need a built-in content management backend? Check out the [content library](https://github.com/antistereov/singularity-content) for a seamless extension.

## ⚡ Why Use This?

- ✅ **Batteries Included** – JWT auth, 2FA, email verification, file storage, and key rotation already set up.
- ✅ **Code Reuse Made Easy** – Shared libraries mean no more copying boilerplate between projects.
- ✅ **Production-Ready by Default** – All components are built with real-world usage and scalability in mind.
- ✅ **Fast Start, Every Time** – Create a new backend app in minutes using the existing architecture and libs.
- ✅ **Open & Extensible** – Contributions welcome! Let’s refine this into a toolkit others can benefit from too.

## 🔐 Features at a Glance

### **Security & Key Management**
- 🔑 Secret manager integration with **automated key rotation** for JWT & encryption keys.

### **Authentication & User Management**
- 🔒 JWT auth with refresh tokens, 2FA, secure HTTP-only cookies.
- 📧 Email verification with expiration and resend control.
- 🧑‍💻 Role-based user access with custom exceptions for better error handling.

### **Data & Caching**
- 💾 MongoDB for persistence, Redis for caching and session storage.
- 🗂️ S3-based object storage abstraction with local fallback.

### **Content Management**
- 🧩 Abstract base for content types with **built-in access control** (users, groups, roles).
- 🌍 **Multi-language support** out of the box — store and serve content in multiple locales.
- 🏷️ Configurable tagging system for flexible content organization.
- 📝 Prebuilt `Article` class for instant publishing workflows.

### **Performance**
- ⚙️ Kotlin Coroutines for async flows.
- 🚦 Configurable rate limiting (IP & user-based).

## 🔗 Related

Need a built-in content management backend? Check out the content library:
👉 [singularity-content](https://github.com/antistereov/singularity-content)

## Development Setup

### Prerequisites:
- JDK 21 or higher.
- MongoDB and Redis running locally or remotely.
- SMTP server for email verification.
- S3 instance for file storage.
- [Bitwarden Secret Manager](https://bitwarden.com/products/secrets-manager/) account or instance.

### Dependency

Add the dependency to your `build.gradle.kts`, `build.gradle` or `pom.xml` if using Maven:

**For Kotlin DSL:**
```kotlin
implementation("io.stereov.singularity:core:<version>") // Check the maven status batch for the latest version
```

**For Gradle:**
```groovy
dependencies {
   implementation 'io.stereov.singularity:core:<version>' // Check the maven status batch for the latest version
}
```

**For Maven:**
```xml
<dependency>
   <groupId>io.stereov.singularity</groupId>
   <artifactId>core</artifactId>
   <version>_version_</version> <!-- Check the maven status batch for the latest version -->
</dependency>
```

**Note:** If you want to use the Bitwarden Secret Manager, you need to [set up the GitHub Package Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#installing-a-package):

```kotlin
repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.pkg.github.com/bitwarden/sdk-sm")
        credentials {
            username = properties["gpr.user"] as String? // Set your GPR user in your gradle.properties
                ?: System.getenv("GPR_USER")             // or as an environment variable
            password = properties["gpr.key"] as String?  // Do the same with your GPR token
                ?: System.getenv("GPR_KEY")
        }
    }
}
```

### Configuration

Just as any other Spring Boot Starter, you can configure your application using a `values.yaml` file in your resources.
Here are the options:

```yaml
# Server configuration
server:
  # The port on which the server will listen for incoming requests.
  port: ${BACKEND_PORT}

# Spring Framework specific configurations
spring:
  data:
    mongodb:
      # MongoDB connection URI. Includes credentials, host, port, database, authentication source, and SSL options.
      uri: mongodb://${MONGO_DB_USERNAME}:${MONGO_DB_PASSWORD}@${MONGO_DB_HOST}:${MONGO_DB_PORT}/${MONGO_DB_DATABASE_NAME}?authSource=admin&tls=${MONGO_DB_SSL_ENABLED:false}
    redis:
      # Redis server host.
      host: ${REDIS_HOST}
      # Redis server port.
      port: ${REDIS_PORT}
      # Redis server password.
      password: ${REDIS_PASSWORD}
      # Redis database index to use (default is 0).
      database: ${REDIS_DATABASE:0}
      # Redis connection timeout in milliseconds (default is 5000ms).
      timeout: ${REDIS_TIMEOUT:5000ms}
      ssl:
        # Enable or disable SSL for Redis connection (default is false).
        enabled: ${REDIS_SSL_ENABLED:false}
  devtools:
    restart:
      # Enable or disable Spring DevTools restart functionality (default is false).
      enabled: ${DEV_MODE:false}
      # Additional paths to monitor for changes that trigger a restart.
      additional-paths: src/main

# Logging configuration
logging:
  level:
    # Set the logging level for the 'io.stereov' package (default is DEBUG).
    io.stereov: ${LOG_LEVEL:DEBUG}

# Singularity application specific configurations
singularity:
  app:
    # The name of the application.
    name: ${APP_NAME}
    # The base URL of the backend application.
    base-url: ${BACKEND_BASE_URL}
    # Support email address.
    support-mail: ${SUPPORT_EMAIL}
    # Enable or disable secure mode (e.g., HTTPS enforcement).
    secure: ${SECURE:false}
    # Flag to determine if a root user should be created on startup.
    create-root-user: ${CREATE_ROOT_USER}
    # Email for the root user if created.
    root-email: ${ROOT_EMAIL}
    # Password for the root user if created.
    root-password: ${ROOT_PASSWORD}
  file:
    storage:
      # Type of file storage to use (e.g., s3).
      type: s3
      s3:
        # S3 domain.
        domain: ${S3_DOMAIN}
        # S3 bucket name.
        bucket: ${S3_BUCKET}
        # S3 access key.
        access-key: ${S3_ACCESS_KEY}
        # S3 secret key.
        secret-key: ${S3_SECRET_KEY}
        # S3 scheme (e.g., http, https).
        scheme: ${S3_SCHEME}
        # Enable or disable S3 path style access.
        path-style-access-enabled: ${S3_PATH_STYLE_ACCESS_ENABLED}
  secrets:
    # Key manager type for secrets (e.g., bitwarden).
    store: bitwarden
    # Cron expression for key rotation.
    key-rotation-cron: 0 0 4 1 1,4,7,10 *
    # Cache expiration for secrets in milliseconds.
    cache-expiration: 900000
    bitwarden:
      # Bitwarden access token.
      access-token: ${BITWARDEN_ACCESS_TOKEN}
      # Bitwarden API URL.
      api-url: ${BITWARDEN_API_URL}
      # Bitwarden Identity URL.
      identity-url: ${BITWARDEN_IDENTITY_URL}
      # Bitwarden project ID.
      project-id: ${BITWARDEN_PROJECT_ID}
      # Bitwarden organization ID.
      organization-id: ${BITWARDEN_ORGANIZATION_ID}
      # Path to the Bitwarden state file.
      state-file: ${BITWARDEN_STATE_FILE}
  mail:
    # Mail server host.
    host: ${MAIL_HOST}
    # Mail server port.
    port: ${MAIL_PORT}
    # Email address used for sending mail.
    email: ${MAIL_EMAIL}
    # Username for mail server authentication.
    username: ${MAIL_USERNAME}
    # Password for mail server authentication.
    password: ${MAIL_PASSWORD}
    # Mail transport protocol (default is smtp).
    transport-protocol: ${MAIL_TRANSPORT_PROTOCOL:smtp}
    # Enable or disable SMTP authentication (default is true).
    smtp-auth: ${MAIL_SMTP_AUTH:true}
    # Enable or disable SMTP STARTTLS (default is true).
    smtp-starttls: ${MAIL_SMTP_STARTTLS:true}
    # Enable or disable mail debug logging (default is false).
    debug: ${MAIL_DEBUG:false}
    # Expiration time for email verification tokens in seconds (default is 900).
    verification-expiration: ${MAIL_VERIFICATION_EXPIRATION:900}
    # Cooldown period for sending email verification in seconds (default is 60).
    verification-send-cooldown: ${MAIL_VERIFICATION_SEND_COOLDOWN:60}
    # Expiration time for password reset tokens in seconds.
    password-reset-expiration: 900
    # Cooldown period for sending password reset emails in seconds.
    password-reset-send-cooldown: 60
  security:
    jwt:
      # JWT token expiration time in seconds.
      expires-in: 900
    rate-limit:
      # Maximum number of requests per IP address within the time window.
      ip-limit: 2000
      # Time window for IP rate limiting in minutes.
      ip-time-window: 1
      # Maximum number of requests per user within the time window.
      user-limit: 2000
      # Time window for user rate limiting in minutes.
      user-time-window: 1
    login-attempt-limit:
      # Maximum number of login attempts per IP address.
      ip-limit: 1000
      # Time window for login attempt limiting in minutes.
      ip-time-window: 1
    two-factor:
      # Length of two-factor recovery codes.
      recovery-code-length: 10
      # Number of two-factor recovery codes generated.
      recovery-code-count: 6
  ui:
    # Base URL of the user interface.
    base-url: ${UI_BASE_URL}
    # URL for the application icon.
    icon-url: ${ICON_URL}
    # Primary color for the user interface (hex code).
    primary-color: ${UI_PRIMARY_COLOR:#6366f1}
    # Path to the contact page.
    contact-path: ${CONTACT_PATH:/contact}
    # Path to the legal notice page.
    legal-notice-path: ${LEGAL_NOTICE_PATH:/legal-notice}
    # Path to the privacy policy page.
    privacy-policy-path: ${PRIVACY_POLICY_PATH:/privacy-policy}
    # Path for email verification in the UI.
    email-verification-path: ${EMAIL_VERIFICATION_PATH:/auth/verify-email}
    # Path for password reset in the UI.
    password-reset-path: ${PASSWORD_RESET_PATH:/auth/reset-password}
```

### Getting Started

Create a Spring Boot Application in Kotlin:

```kotlin
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class App

fun main() {
  runApplication<App>()
}
```

Or in Java:

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
```

## Service Overview

> **Note**:  
> A detailed overview of the available methods and usage can be found inside the code.
> Please refer to the codebase for full documentation and examples.

### **User Management**
- **UserService**:  
  Access and manage user information using the [`UserService`](./src/main/kotlin/io/stereov/singularity/user/service/UserService.kt).

### **Authentication**
- **AuthenticationService**:  
  The application automatically handles authentication per request using a filter.
  You can access the current authenticated user via the [`AuthenticationService`](./src/main/kotlin/io/stereov/singularity/auth/service/AuthenticationService.kt).

### Cache Management
- **RedisService**:  
  Interact with the cache using the [`RedisService`](./src/main/kotlin/io/stereov/singularity/cache/service/RedisService.kt) or the `redisCoroutinesCommands` bean for efficient data retrieval and storage.

### Encryption & Decryption
- **EncryptionService**:  
  Use the [`EncryptionService`](./src/main/kotlin/io/stereov/singularity/encryption/service/EncryptionService.kt) to securely encrypt and decrypt values before storing them in the database.

### Hashing & Validation
- **HashService**:  
  The [`HashService`](./src/main/kotlin/io/stereov/singularity/hash/service/HashService.kt) allows you to hash sensitive data and validate hashed values for secure comparisons.

### JWT Encoding & Decoding
- **JwtService**:  
  The [`JwtService`](./src/main/kotlin/io/stereov/singularity/jwt/service/JwtService.kt) handles the encoding and decoding of JSON Web Tokens (JWT) for authentication and authorization.

### Two-Factor Authentication
- **TwoFactorAuthService**:  
  The [`TwoFactorAuthService`](./src/main/kotlin/io/stereov/singularity/twofactorauth/service/TwoFactorAuthService.kt) manages the setup,
  verification, and recovery of two-factor authentication (2FA) for user accounts.

## Endpoints

### `/user`

These endpoints are used for user management and authentication.
They are designed to be secure and efficient, leveraging JWT for stateless authentication.
The related class is [`UserSessionController`](./src/main/kotlin/io/stereov/singularity/user/controller/UserSessionController.kt).

- **POST /user/login**: Logs in the user and issues JWT access and refresh tokens.
- **POST /user/register**: Registers a new user and issues JWT tokens.
- **POST /user/logout**: Logs the user out and clears authentication cookies.
- **POST /user/logout-all**: Logs out the user from all devices.
- **POST /user/refresh**: Refreshes the user's JWT tokens.

### `/user/me`

- **GET /user/me**: Retrieves information about the currently authenticated user.
- **GET /user/me/app**: Retrieves application-specific information about the authenticated user.
- **PUT /user/me/email**: Updates the user's email address.
- **PUT /user/me/password**: Updates the user's password.
- **PUT /user/me**: Updates the user's profile.

### `/user/mail`

These endpoints are used for email verification and password reset functionalities.
They are designed to enhance security and user experience.
The related class is [`UserMailController`](./src/main/kotlin/io/stereov/singularity/user/controller/UserMailController.kt).

- **POST /user/mail/verify**: Verifies the user's email address using a token.
- **GET /user/mail/verify/cooldown**: Retrieves the remaining cooldown time before the user can request another email verification.
- **POST /user/mail/verify/send**: Send the email verification token.
- **POST /user/mail/reset-password**: Resets the user's password.
- **GET /user/mail/reset-password/cooldown**: Retrieves the remaining cooldown time before the user can request another password reset email.
- **POST /user/mail/reset-password/send**: Sends a password reset email to the user.

### `/user/devices`

These endpoints are used for managing user devices and sessions.
They allow users to view and manage their active sessions and devices.
The related class is [`UserDeviceController`](./src/main/kotlin/io/stereov/singularity/user/controller/UserDeviceController.kt).

- **GET /user/devices**: Retrieves a list of devices associated with the authenticated user.
- **DELETE /user/devices/{deviceId}**: Removes a specific device from the user's account.
- **DELETE /user/devices**: Clears all devices associated with the user.

### `/user/2fa`

These endpoints are used for managing two-factor authentication (2FA) for user accounts.
They provide a secure way to enhance user account security.
The related class is [`UserTwoFactorAuthController`](./src/main/kotlin/io/stereov/singularity/user/controller/UserTwoFactorAuthController.kt).

- **GET /user/2fa/start-setup**: Sets a token that enables the 2FA setup.
- **GET /user/2fa/setup**: Retrieves the setup information for two-factor authentication.
- **POST /user/2fa/setup**: Set up two-factor authentication for the user.
- **POST /user/2fa/verify-login**: Verifies the user's 2FA code and logs in the user.
- **GET /user/2fa/login-status**: Checks whether two-factor authentication is pending for the user.
- **POST /user/2fa/recovery**: Recovers the user's account using a recovery code.
- **POST /user/2fa/verify-step-up**: If 2FA is enabled, this endpoint is used to set a new step-up token that enables critical changes to the user account, e.g., changing the password.
- **GET /user/2fa/step-up-status**: Gets the status of the step-up needed for critical changes to the user account. Is true if 2FA is disabled.
- **POST /user/2fa/disable**: Disables two-factor authentication for the user.


## Contribution Guidelines

## 🤝 Contribution Guidelines

We welcome contributions! To get started, please follow these steps:

1. **Fork** the repository.
2. **Create** a new branch:
   ```bash
   git checkout -b your-feature-name
   ```
3. **Commit** your changes:
   ```bash
   git commit -am "Describe your feature or fix"
   ```
4. **Push** the branch to your fork:
   ```bash
   git push origin your-feature-name
   ```
5. **Open a pull request** with a clear and detailed description of your changes.

> ⚠️ **Note:**  
> This repository is a mirror of the monorepo where all libraries and apps are developed.  
> If your pull request is accepted, the changes will be manually merged into the monorepo and then reflected here.

## License

This project is licensed under the GPLv3 License—see the [LICENSE](../../LICENSE) file for details.
If you intend for commercial use, please contact me at [contact@stereov.io](mailto:contact@stereov.io).  
