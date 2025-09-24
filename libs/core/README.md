<h1 align="center">Singularity</h1>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.stereov.singularity/core">
    <img src="https://img.shields.io/maven-central/v/io.stereov.singularity/core.svg?logo=apachemaven" alt="Maven Central">
  </a>
  <a href="https://singularity.stereov.io">
    <img src="https://img.shields.io/badge/docs-available-brightgreen?logo=docusaurus" alt="Docs">
  </a>
  <a href="https://github.com/antistereov/singularity-core?tab=GPL-3.0-1-ov-file">
    <img src="https://img.shields.io/github/license/antistereov/singularity-core?logo=gnu" alt="License">
  </a>
</p>

<p align="center">
  <a href="https://stereov.io">
    <img src="https://img.shields.io/badge/My_Website-stereov.io-blue" alt="Website">
  </a>
  <a href="https://instagram.com/antistereov.coding">
    <img src="https://img.shields.io/badge/Instagram-@antistereov.coding-ff69b4?logo=instagram" alt="Instagram">
  </a>
  <a href="https://twitter.com/antistereov">
    <img src="https://img.shields.io/badge/X-@antistereov-1DA1F2?logo=x" alt="Twitter">
  </a>
</p>

<p align="center">
  Welcome to the foundation for your next backend! 
  Build your app with everything from authentication to content management, ready out of the box. 🚀
</p>
<p align="center">
  Save time, ensure consistency, and focus on features whether you're building an API,
  a microservice, or a full-stack app.
</p>

## ⚡ Why Use This?

- ✅ **Batteries Included:** Authentication with 2FA and email verification, content management, file storage, and key rotation already set up.
- ✅ **Production-Ready by Default:** All components are built with real-world usage and scalability in mind. This foundation is built on [Spring](https://spring.io), ensuring a reliable, mature, and modern technology stack.
- ✅ **Open & Extensible:** Contributions welcome! Let’s refine this into a toolkit others can benefit from too.

## 🔐 Features at a Glance

### **Authentication & User Management**
- 🔒 JWT auth with refresh tokens, 2FA, secure HTTP-only cookies.
- 📧 Email verification with expiration and resend control.
- 🧑‍💻 Role-based user access with custom exceptions for better error handling.

### **Data & Caching**
- 💾 MongoDB for persistence, Redis for caching and session storage.
- 🗂️ S3-based object storage abstraction with local fallback.

### **Content Management**
- 🧩 Abstract base for content types with **built-in access control** (users, groups, roles).
- 🌍 **Multi-language support** out-of-the-box — store and serve content in multiple locales.
- 🏷️ Configurable tagging system for flexible content organization.
- 📝 Prebuilt `Article` class for instant publishing workflows.

### **Security & Key Management**
- 🔑 Secret manager integration with **automated key rotation** for your secrets.

### **Performance**
- ⚙️ Kotlin Coroutines for async flows.
- 🚦 Configurable IP and user-based rate limiting.

## 🚀 Quickstart

### 1. Create a new project

Use your favorite IDE or build tool to create a new Java or Kotlin project with JDK 21 or higher.

### 2. Add Singularity to your dependencies

Add the dependency to your `build.gradle.kts`, `build.gradle` or `pom.xml` if using Maven:

**For Gradle with Kotlin DSL:**
```kotlin
dependencies {
    implementation("io.stereov.singularity:core:<version>") // Check the maven status batch for the latest version
}
```

**For Gradle with Groovy:**
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

### 3. Start MongoDB and Redis

Download the [docker-compose.yaml](https://github.com/antistereov/singularity-core/blob/354c7258e0b6416b108639224fc075d51830198b/infrastructure/docker/docker-compose.yaml) 
to the root of your project and run the containers using:

```bash
docker compose up -d
```

You have a running instance of MongoDB as your database and Redis as your Cache.

### 4. Create your application class

Create a Spring Boot Application in Kotlin:

```kotlin
@SpringBootApplication
class YourApplication

fun main() {
  runApplication<YourApplication>()
}
```

Or in Java:

```java
@SpringBootApplication
public class App {
  public static void main(String[] args) {
    SpringApplication.run(App.class, args);
  }
}
```
### 5. Expand

Add your custom services and features to create your dream. Focus on what matters.

## 📚 Documentation

You can find a detailed guide to this library here: [https://singularity.stereov.io](https://singularity.stereov.io).

It also provides a test application to test the preconfigured endpoints in Swagger UI or your favorite tool.

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

## 📄 License

This project is licensed under the GPLv3 License—see the [LICENSE](../../LICENSE) file for details.
If you intend for commercial use, please contact me at [contact@stereov.io](mailto:contact@stereov.io).  
