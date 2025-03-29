# Kotlin + Spring Web Baseline

[![Maven Central](https://img.shields.io/maven-central/v/io.stereov.web/baseline.svg)](https://central.sonatype.com/artifact/io.stereov.web/baseline)

This is a Kotlin-based Spring Web application template designed to provide essential features for secure and efficient web applications. 
It incorporates a wide range of functionality, such as user management, two-factor authentication, JWT-based authentication, encryption, and more, making it an excellent starting point for building production-ready applications.

**Note:** 
This repository goes hand in hand with my Angular baseline. You can find it [here](https://github.com/antistereov/web-angular-baseline).

## **Why Use This?**

This baseline serves as a **foundation for all my projects**, allowing me to avoid rewriting the same authentication, security, and backend setup every time. Instead of duplicating code, I maintain a **single, well-structured repository** where all essential features are readily available.

By using this template, you get:

- ✅ **A Pre-Built, Secure Backend** – No need to implement authentication, JWT handling, 2FA, and email verification from scratch.
- ✅ **Consistency Across Projects** – Ensures all my projects follow the same best practices and architecture.
- ✅ **Time-Saving Development** – Focus on building features instead of setting up authentication, caching, and rate-limiting.
- ✅ **Open for Contributions** – If you like this approach, feel free to contribute! The goal is to refine and expand this baseline so others can benefit as well.

This repository isn't just for personal use—it's meant to be a **collaborative and evolving** foundation for secure, scalable applications. 🚀

## Features

### **Authentication & Security**
- **JWT Authentication**: Provides secure authentication using JWT with configurable access token expiration, refresh tokens bound to devices, and configurable token lifetimes.
- **Two-Factor Authentication (2FA)**: Full 2FA setup, verification, and recovery flow to enhance user security.
- **HTTP-Only Authentication**: Uses secure, HTTP-only cookies to store authentication information, ensuring enhanced security.
- **Email Verification**: Configurable email verification system, including token expiration time and resend cooldown for a smooth user experience.
- **Encryption and Hashing**: Implements encryption for sensitive data, along with secure password and token hashing.

### **User Management**
- **User Roles**: Supports user roles and includes application-specific information via the `ApplicationInfo` interface.
- **Custom Exceptions**: Custom exceptions built on top of `BaseWebException`, allowing for better error handling and meaningful error messages.

### **Data Storage & Caching**
- **MongoDB Integration**: Fully integrated with MongoDB for seamless data storage.
- **Redis Caching**: Utilizes Redis for efficient caching and performance improvement.

### **Performance & Rate Limiting**
- **Rate Limiting**: Configurable rate-limiting for both IP and user account-based limits to prevent abuse.
- **Asynchronous Programming**: Built with Kotlin coroutines for async processing, and integrates Log4j for asynchronous logging.

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
implementation("io.stereov.web:baseline:<version>") // Check for the latest version
```

**For Gradle:**
```groovy
dependencies {
   implementation 'io.stereov.web:baseline:<version>' // Check for the latest version
}
```

**For Maven:**
```xml
<dependency>
   <groupId>io.stereov.web</groupId>
   <artifactId>baseline</artifactId>
   <version>_version_</version> <!-- Check for the latest version -->
</dependency>
```

#### Configuration

Here are the key properties you need to set in your `application.yaml`:
- **Spring Configuration:**
    There are some things you need to set up in order to make things work:

    ```yaml
    server:
      # The port the server should run on
      port: 8000
    spring:
      main:
        # This setting is really important to make your application reactive
        web-application-type: reactive
      devtools:
        restart:
          # Do you want your application to restart if changes occur?
          enabled: false
          additional-paths: src/main
        logging:
          level:
          # Set log levels for your packages
          io.stereov.web: DEBUG
    ```
    If you want to enable dev tools, you have to add the following dependency:
      
    ```kotlin
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    ```

- **MongoDB:**
   
    You need to connect your backend to a MongoDB instance. This instance is used to save information about the users.
   
    ```yaml
    spring:
      data:
        mongodb:
         uri: mongodb://<username>:<password>@<host>:<port>/<database>?authSource=admin
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
        # The name of your application
        name: YourApplication
        # The base URL of your API
        base-url: http://localhost:8000
        # Whether SSL and the secure setting for HTTP-only cookies should be set
        secure: true # You might need to set it to false during development
    ```
    
- **UI Settings:**
    
    Most probably, you are using this application to control your UI.
    You have to set some properties in order to make it run as expected.
    ```yaml
    baseline:
      app:
        # The base URL of your frontend
        base-url: http://localhost:4200
    ```
      
- **Security Settings:**
   
    You need to set up some measures to secure your web application:
    ```yaml
    baseline:
      app:
        security:
          encryption:
            # Generate an encryption key that is used to encrypt entries in the database
            # You can create one here: https://generate-random.org/encryption-key-generator and use the Base64 value
            secret-key: <your-encryption-key>
          jwt:
            # Your JWT secret
            # You can generate a secret here: https://jwtsecret.com/generate
            secret-key: <your-jwt-secret>
            # In how many seconds should your JWT expire?
            expires-in: 900
          rate-limit:
            # How many calls do you allow per minute per IP address?
            ip-rate-limit-minute: 200
            # How many calls do you allow per minute per account?
            account-rate-limit-minute: 100
          two-factor:
            # How long should the recovery code be?
            recovery-code-length: 20
    ```

- **Email Verification Settings:**
   
    You can require users to enable their email. Therefore, you need to configure your own SMTP server.
    This server will send the user an email with a link to verify their email address.
    ```yaml
    baseline:
      mail:
        # Enable email verification and password reset - false on default
        enable: true 
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
    This is optional. You can enable email verification by setting `enable-verification` to true. 
    By default, it is set to `false`.
    
    **Note:** Password resets are not possible without email verification.

#### *(Optional)* Test Setup
   
I recommend the following packages if you want to test your application properly:
```kotlin
testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(group = "org.mockito")
}
testImplementation("org.junit.jupiter:junit-jupiter")
testImplementation("io.mockk:mockk:1.13.13")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxVersion")
testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
testImplementation("org.testcontainers:mongodb:$testContainersVersion")
```

## Service Overview

> **Note**:  
> A detailed overview of the available methods and usage can be found inside the code. 
> Please refer to the codebase for full documentation and examples.

### **User Management**
- **UserService**:  
  Access and manage user information using the `UserService`.

### **Authentication**
- **AuthenticationService**:  
  The application automatically handles authentication per request using a filter. 
  You can access the current authenticated user via the `AuthenticationService`.

### **Cache Management**
- **RedisService**:  
  Interact with the cache using the `RedisService` for efficient data retrieval and storage.

### **Encryption & Decryption**
- **EncryptionService**:  
  Use the `EncryptionService` to securely encrypt and decrypt values before storing them in the database.

### **Hashing & Validation**
- **HashService**:  
  The `HashService` allows you to hash sensitive data and validate hashed values for secure comparisons.

### **JWT Encoding & Decoding**
- **JwtService**:  
  The `JwtService` handles the encoding and decoding of JSON Web Tokens (JWT) for authentication and authorization.

## Endpoints

### `/user`
- **GET /user/me**: Retrieves information about the currently authenticated user.
- **POST /user/login**: Logs in the user and issues JWT access and refresh tokens.
- **POST /user/register**: Registers a new user and issues JWT tokens.
- **POST /user/logout**: Logs the user out and clears authentication cookies.
- **POST /user/refresh**: Refreshes the user's JWT tokens.

### `/user/mail`
- **POST /user/mail/verify**: Verifies the user's email address using a token.
- **GET /user/mail/verify/cooldown**: Retrieves the remaining cooldown time before the user can request another email verification.
- **POST /user/mail/verify/send**: Send the email verification token.
- **POST /user/mail/password-reset**: Resets the user's password.
- **GET /user/mail/password-reset/cooldown**: Retrieves the remaining cooldown time before the user can request another password reset email.
- **POST /user/mail/password-reset/send**: Sends a password reset email to the user.

### `/user/devices`
- **GET /user/devices**: Retrieves a list of devices associated with the authenticated user.
- **DELETE /user/devices/{deviceId}**: Removes a specific device from the user's account.
- **DELETE /user/devices**: Clears all devices associated with the user.

### `/user/2fa`
- **POST /user/2fa/setup**: Sets up two-factor authentication for the user.
- **POST /user/2fa/verify**: Verifies the user's 2FA code.
- **GET /user/2fa/status**: Checks whether two-factor authentication is pending for the user.
- **POST /user/2fa/recovery**: Recovers the user's account using a recovery code.

## Technologies Used

- **Kotlin**: A modern, statically-typed language for the JVM.
- **Spring WebFlux**: A reactive framework for building non-blocking applications.
- **MongoDB**: NoSQL database for data storage.
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

This project is licensed under the GPLv3 License - see the [LICENSE](LICENSE) file for details.
If you intend for commercial use, please contact me at [contact@stereov.io](mailto:contact@stereov.io).  
