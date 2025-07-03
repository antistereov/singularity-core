# **Singularity Core**

[![Maven Central](https://img.shields.io/maven-central/v/io.stereov.singularity/core.svg)](https://central.sonatype.com/artifact/io.stereov.singularity/core)

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

### **Performance**
- ⚙️ Kotlin Coroutines for async flows.
- 🚦 Configurable rate limiting (IP & user-based).

## 🔗 Related

Check out the matching Angular frontend baseline:  
👉 [web-angular-baseline](https://github.com/antistereov/web-angular-baseline)

## 🚀 Getting Started

Want to bootstrap a new web backend project? Just add a new app to the `apps/` folder and hook into the existing libraries in `libs/` — all the groundwork is already done.

## Development Setup

### Prerequisites:
- JDK 21 or higher.
- MongoDB and Redis running locally or remotely.
- SMTP server for email verification.

### Running the Application:

**Note:** You can always look at the demo module for inspiration.

#### Dependency

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

#### Configuration

Here are the key properties you need to set in your `application.yaml`:
- **Spring Configuration:**
    There are some things you need to set up to make things work:

    ```yaml
    server:
      # The port the server should run on
      port: 8000
    spring:
      devtools:
        restart:
          # Do you want your application to restart if changes occur?
          enabled: false
          additional-paths: src/main
        logging:
          level:
          # Set log levels for your packages
          io.stereov.singularity: DEBUG
    ```
  
- **MongoDB:**
   
    You need to connect your backend to a MongoDB instance. This instance is used to save information about the users.
   
    ```yaml
    spring:
      data:
        mongodb:
         uri: mongodb://<username>:<password>@<host>:<port>/<database>?authSource=admin&tls=<enable-tls>
    ```

- **Redis:**
           
    You need a running redis instance for rate-limiting and caching.
    ```yaml
    spring:
      data:
        redis:
          host: <redis_host>
          port: <redis_port>
          password: <redis_password>
          database: 0
          timeout: 5000ms
          ssl:
            enabled: false
    ```

- **Application Settings:**

    This sets the main configurations for the application.
    ```yaml
    baseline:
      app:
        # Optional: The name of your application. Default is "Spring Boot Application"
        name: Your Application
        # Optional: The base URL of your backend. Default is http://localhost:8000
        base-url: http://localhost:8000
        # Optional: Enable HTTPS and secure cookies for the application. Default is false.
        # Important: Never set this to false in production!
        secure: false
        # Should a root user be created on startup?
        # It can be useful if you don't want to manually set up a root user.
        # Please set it to false as soon as you created one and change the password.
        create-root-user: false
        root-email: admin@email.com
        root-password: <root-password>
    ```
  
- **Secrets Settings:**

    Your application secrets such as encryption keys and JWT secrets will be stored in a secret manager.
    Currently, only Bitwarden Secret Manager is available.
    ```yaml
    key-manager: bitwarden
    # The CRON string that defines automatic key rotation
    key-rotation-cron: 0 0 4 1 1,4,7,10 *
    # Secrets are cached locally to improve performance and to limit calls to the secret manager
    # You can define the expiration of these cached secrets here in milliseconds.
    cache-expiration: 900000
    bitwarden:
      access-token: <your-bitwarden-access-token>
      api-url: <your-bitwarden-api-url>
      identity-url: <your-bitwarden-identity-url>
      project-id: <your-bitwarden-project-id>
      organization-id: <your-bitwarden-organization-id>
      # Bitwarden needs access to a state file where it can save necessary information
      state-file: ~/.bitwarden/secrets-manager/demo-application/state
    ```
    
- **UI Settings:**
    
    Most probably, you are using this application to control your UI.
    You have to set some properties to make it run as expected.
    ```yaml
    baseline:
      ui:
        # Optional: The base URL of your frontend. Default is http://localhost:4200
        base-url: http://localhost:4200
    ```
      
- **Security Settings:**
   
    You need to set up some measures to secure your web application:
    ```yaml
    baseline:
      app:
        security:
          encryption:
            # Generate an encryption key used to encrypt entries in the database
            # Note: You can create one here: https://generate-random.org/encryption-key-generator and use the Base64 value
            secret-key: <your-encryption-key>
          jwt:
            # Your JWT secret
            # Note: You can generate a secret here: https://jwtsecret.com/generate
            secret-key: <your-jwt-secret>
            # Optional: Expiration time for access tokens in seconds
            expires-in: 900
          rate-limit:
            # Optional: IP-based limit on requests in a given time window
            ip-limit: 200
            # Optional: Time window for IP-based rate limiting in minutes
            ip-time-window: 1
            # Optional: User-based limit on requests in a given time window
            user-limit: 200
            # Optional: Time window for user-based rate limiting in minutes
            user-time-window: 1
          login-attempt-limit:
            # Optional: IP-based limit on login attempts in a given time window
            ip-limit: 10
            # Optional: Time window for IP-based login attempts in minutes
            ip-time-window: 15
          two-factor:
            # Optional: Length of the 2FA recovery codes. Default is 10 characters.
            recovery-code-length: 10
            # Optional: Count of 2FA recovery codes. Default is 6.
            recovery-code-count: 6
    ```

- **Email Settings:**
   
    Emails are needed to allow recovery when the user forgets their password. 
    To activate, you need to configure your own SMTP server.
    The server will send the user an email with a link to verify their email address on register
    and can email the user to recover the user account it the password is lost.
    ```yaml
    baseline:
      mail:
        # Credentials for your smtp server
        host: <smtp_host>
        port: <smtp_port>
        username: <smtp_username>
        password: <smtp_password>
        # The transport protocol - it is SMTP most of the time
        transport-protocol: smtp 
        # Enable SMTP authentication
        smtp-auth: true 
        # Enable STARTTLS encryption
        smtp-starttls: true
        # Enable debugging
        debug: false
        # How long should a token for email verification be valid?
        verification-expiration: 900
        # How much time should pass until a new verification email can be sent?
        verification-send-cooldown: 60 
        # How long a token for a password reset should be valid?
        password-reset-expiration: 900
        # How much time should pass until a new password reset email can be sent?
        password-reset-cooldown: 60
        # The path to the email verification page in the UI
        ui-verification-path: /auth/mail/verify
        # The path to the password reset page in the UI
        ui-password-reset-path: /auth/password-reset
    ```
    
    **Note:** Password resets are not possible without email verification.

- **File Settings:**

    Configure how and where files should be saved. You need an S3-compatible instance.
  
    ```yaml
    baseline:
      file:
        storage:
          s3:
            domain: your.s3.domain.com
            bucket: your-bucket
            access-key: <your-access-key>
            secret-key: <your-secret-key>
            scheme: https
    ```

## Managing Sensitive Data
The abstract class [`SenstiveCrudService`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/database/service/SensitiveCrudService.kt) and interface [`SensitiveCrudRepository`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/database/repository/SensitiveCrudRepository.kt)
define a way to handle documents that contain sensitive information.

There is a class for
[`EncryptedSensitiveDocument`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/database/model/EncryptedSensitiveDocument.kt)
and [`SensitiveDocument`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/database/model/SensitiveDocument.kt)
share a common type parameter which defines the class of the encrypted information.
You can take a look at the implementation of
[`UserDocument`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/model/UserDocument.kt) and
[`EncryptedUserDocument`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/model/EncryptedUserDocument.kt)
which share [`SensitiveUserData`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/model/SensitiveUserData.kt)
in decrypted and encrypted form respectively.

Encryption and decryption is handled by the `SensitiveCrudService` automatically.
You will only work with the decrypted document.

The only thing you need to set up is the `encrypt()` and `decrypt()` method and to override the `serializer` value.

Here`s an example:

```kotlin
/**
 * The class defining sensitive properties.
 */
@Serializable
data class SensitiveExampleData(
    var favoriteColor: String,
)

/**
 * The document containing the sensitive data in decrypted form.
 * This is the document you will actually work with.
 */
@Serializable
data class SensitiveDocument(
    var id: String?,
    override var sensitive: SensitiveExampleData
) : SensitiveDocument<SensitiveExampleData>()

/**
 * The document containing the sensitive data in encrypted form.
 * This will be stored to the database.
 */
@Serializable
data class EncryptionSensitiveDocument(
    var id: String,
    override var sensitive: Encrypted<SensitiveExampleData>
) : EncryptionSensitiveDocument<SensitiveExampleData>()


/**
 * The repository uses the encrypted document.
 */
interface ExampleRepository : SensitiveCrudRepository<EncryptedSensitiveDocument>

/**
 * This is the service. You need to override the `serializer` and the `encrypt()` and `decrypt()` methods.
 */
class ExampleService(
    private val encryptionService: EncryptionService,
    exampleRepository: ExampleRepository,
    encryptionSecretService: EncryptionSecretService,
    json: Json
) : SensitiveCrudService<SensitiveExampleData, ExampleDocument, EncryptedExampleDocument>(exampleRepository, encryptionSecretService) {

    /**
     * If you make sure to add the @Serializable annotation to all your documents, you can import the serializer like this.
     */
    override val serializer = json.serializersModule.serializer<SensitiveExampleData>()
  
    override suspend fun encrypt(document: ExampleDocument, otherValues: List<any>): EncryptedExampleDocument {
        return encryptionService.encrypt(document, serializer) as EncryptedExampleDocument
    }

    override suspend fun decrypt(encrypted: EncryptedExampleDocument, otherValues: List<any>): ExampleDocument {
        return encryptionService.decrypt(encrypted, serializer) as EncryptedExampleDocument
    }
}
```

## Service Overview

> **Note**:  
> A detailed overview of the available methods and usage can be found inside the code. 
> Please refer to the codebase for full documentation and examples.

### **User Management**
- **UserService**:  
  Access and manage user information using the [`UserService`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/service/UserService.kt).

### **Authentication**
- **AuthenticationService**:  
  The application automatically handles authentication per request using a filter. 
  You can access the current authenticated user via the [`AuthenticationService`](libs/core/src/main/kotlin/io/stereov/singularity/core/auth/service/AuthenticationService.kt).

### Cache Management
- **RedisService**:  
  Interact with the cache using the [`RedisService`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/service/cache/RedisService.kt) or the `redisCoroutinesCommands` bean for efficient data retrieval and storage.

### Encryption & Decryption
- **EncryptionService**:  
  Use the [`EncryptionService`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/service/encryption/service/EncryptionService.kt) to securely encrypt and decrypt values before storing them in the database.

### Hashing & Validation
- **HashService**:  
  The [`HashService`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/service/hash/HashService.kt) allows you to hash sensitive data and validate hashed values for secure comparisons.

### JWT Encoding & Decoding
- **JwtService**:  
  The [`JwtService`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/service/jwt/JwtService.kt) handles the encoding and decoding of JSON Web Tokens (JWT) for authentication and authorization.

### Two-Factor Authentication
- **TwoFactorAuthService**:  
  The [`TwoFactorAuthService`](libs/core/src/main/kotlin/io/stereov/singularity/core/global/service/twofactorauth/TwoFactorAuthService.kt) manages the setup, 
  verification, and recovery of two-factor authentication (2FA) for user accounts.

## Endpoints

### `/user`

These endpoints are used for user management and authentication. 
They are designed to be secure and efficient, leveraging JWT for stateless authentication.
The related class is [`UserSessionController`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/controller/UserSessionController.kt).

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
The related class is [`UserMailController`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/controller/UserMailController.kt).

- **POST /user/mail/verify**: Verifies the user's email address using a token.
- **GET /user/mail/verify/cooldown**: Retrieves the remaining cooldown time before the user can request another email verification.
- **POST /user/mail/verify/send**: Send the email verification token.
- **POST /user/mail/reset-password**: Resets the user's password.
- **GET /user/mail/reset-password/cooldown**: Retrieves the remaining cooldown time before the user can request another password reset email.
- **POST /user/mail/reset-password/send**: Sends a password reset email to the user.

### `/user/devices`

These endpoints are used for managing user devices and sessions.
They allow users to view and manage their active sessions and devices.
The related class is [`UserDeviceController`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/controller/UserDeviceController.kt).

- **GET /user/devices**: Retrieves a list of devices associated with the authenticated user.
- **DELETE /user/devices/{deviceId}**: Removes a specific device from the user's account.
- **DELETE /user/devices**: Clears all devices associated with the user.

### `/user/2fa`

These endpoints are used for managing two-factor authentication (2FA) for user accounts.
They provide a secure way to enhance user account security.
The related class is [`UserTwoFactorAuthController`](libs/core/src/main/kotlin/io/stereov/singularity/core/user/controller/UserTwoFactorAuthController.kt).

- **GET /user/2fa/start-setup**: Sets a token that enables the 2FA setup.
- **GET /user/2fa/setup**: Retrieves the setup information for two-factor authentication.
- **POST /user/2fa/setup**: Set up two-factor authentication for the user.
- **POST /user/2fa/verify-login**: Verifies the user's 2FA code and logs in the user.
- **GET /user/2fa/login-status**: Checks whether two-factor authentication is pending for the user.
- **POST /user/2fa/recovery**: Recovers the user's account using a recovery code.
- **POST /user/2fa/verify-step-up**: If 2FA is enabled, this endpoint is used to set a new step-up token that enables critical changes to the user account, e.g., changing the password.
- **GET /user/2fa/step-up-status**: Gets the status of the step-up needed for critical changes to the user account. Is true if 2FA is disabled.
- **POST /user/2fa/disable**: Disables two-factor authentication for the user.

## Technologies Used

- **Kotlin**: A modern, statically typed language for the JVM.
- **Spring WebFlux**: A reactive framework for building non-blocking applications.
- **MongoDB**: NoSQL database for data storage.
- **Bitwarden Secret Manager**: For managing sensitive data and secrets.
- **S3 Object Storage**: For file storage and management.
- **Redis**: In-memory caching for enhanced performance.
- **JWT**: For stateless authentication with secure tokens.
- **Two-Factor Authentication (2FA)**: Adds an extra layer of security to user accounts.
- **Log4j**: For asynchronous logging.
- **Coroutines**: Kotlin's built-in library for asynchronous programming.

## Contribution Guidelines

We welcome contributions! If you'd like to contribute to this project, please follow these steps:

1. Fork the repository.
2. Create a new feature branch (`git checkout -b feature-branch`).
3. Commit your changes (`git commit -am 'Add new feature'`).
4. Push the branch to your fork (`git push origin feature-branch`).
5. Open a pull request with a detailed description of your changes.

## License

This project is licensed under the GPLv3 License—see the [LICENSE](../../LICENSE) file for details.
If you intend for commercial use, please contact me at [contact@stereov.io](mailto:contact@stereov.io).  
