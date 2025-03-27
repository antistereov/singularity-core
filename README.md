# Kotlin Spring Web Baseline

`io.stereov.web:baseline` is a Kotlin-based Spring Web application template designed to provide essential features for secure and efficient web applications. It incorporates a wide range of functionality, such as user management, two-factor authentication, JWT-based authentication, encryption, and more, making it an excellent starting point for building production-ready applications.

## Features

- **User Management**: Supports user roles (ADMIN, USER, GUEST) and includes application-specific information via the `ApplicationInfo` interface.

- **Email Verification**: Configurable email verification system, including token expiration time and resend cooldown for a smooth user experience.

- **Two-Factor Authentication (2FA)**: Full 2FA setup, verification, and recovery flow to enhance user security.

- **Custom Exceptions**: Custom exceptions built on top of `BaseWebException`, allowing for better error handling and meaningful error messages.

- **JWT Authentication**: Provides secure authentication using JWT with configurable access token expiration, refresh tokens bound to devices, and configurable token lifetimes.

- **Encryption and Hashing**: Implements encryption for sensitive data, along with secure password and token hashing.

- **HTTP-Only Authentication**: Uses secure, HTTP-only cookies to store authentication information, ensuring enhanced security.

- **MongoDB Integration**: Fully integrated with MongoDB for seamless data storage.

- **Asynchronous Programming**: Built with Kotlin coroutines for async processing, and integrates Log4j for asynchronous logging.

- **Rate Limiting**: Configurable rate-limiting for both IP and user account-based limits to prevent abuse.

- **Redis Caching**: Utilizes Redis for efficient caching and performance improvement.

## Development Setup

### Prerequisites:
- JDK 17 or higher.
- MongoDB and Redis running locally or remotely.
- SMTP server for email verification.

### Running the Application:

1. Add the dependency to your `build.gradle` (or `pom.xml` if using Maven):

   **For Gradle:**
   ```groovy
   dependencies {
       implementation 'io.stereov.web:baseline:1.0.0' // Check for the latest version
   }
   ```

   **For Maven:**
   ```xml
   <dependency>
       <groupId>io.stereov.web</groupId>
       <artifactId>baseline</artifactId>
       <version>1.0.0</version> <!-- Check for the latest version -->
   </dependency>
   ```

2. Configure the required properties in `application.yml`:

   Here are the key properties you need to set:
    - MongoDB:
       ```yaml
       spring:
         data:
           mongodb:
             uri: mongodb://<username>:<password>@<host>:<port>/<database>?authSource=admin
       ```

    - Redis (optional for caching):
       ```yaml
       spring:
         redis:
           host: <redis_host>
           port: <redis_port>
           password: <redis_password>
       ```

    - Email Verification Settings:
       ```yaml
       baseline:
         mail:
           enable-email-verification: true
           host: <smtp_host>
           port: <smtp_port>
           username: <smtp_username>
           password: <smtp_password>
       ```

    - JWT and Security Configuration:
       ```yaml
       baseline:
         security:
           jwt:
             secret-key: <jwt_secret_key>
             expires-in: 900
       ```

3. Build the application using Gradle:
   ```shell
   ./gradlew build
   ```

4. Run the application:
   ```shell
   ./gradlew bootRun
   ```

5. Access the application at `http://localhost:8000`.

## Endpoints

### `/user`
- **GET /user/me**: Retrieves information about the currently authenticated user.
- **POST /user/login**: Logs in the user and issues JWT access and refresh tokens.
- **POST /user/register**: Registers a new user and issues JWT tokens.
- **POST /user/logout**: Logs the user out and clears authentication cookies.
- **POST /user/refresh**: Refreshes the user's JWT tokens.

### `/user/mail`
- **GET /user/mail/verify-email**: Verifies the user's email address using a token.
- **GET /user/mail/email-verification-cooldown**: Retrieves the remaining cooldown time before the user can request another email verification.
- **POST /user/mail/send-verification-email**: Resends the email verification token.

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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
