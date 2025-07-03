# **Singularity Content**

[![Maven Central](https://img.shields.io/maven-central/v/io.stereov.singularity/content.svg)](https://central.sonatype.com/artifact/io.stereov.singularity/content)

A production-ready Spring Web baseline with built-in content management, powered by MongoDB and Redis. 📝
Manage articles, pages, and structured content with ease — while keeping all the power of JWT auth, 2FA, and async processing from the [core library](https://github.com/antistereov/singularity-core).

Perfect for blogs, documentation, landing pages, and more — all fully API-driven.

## ⚡ Why Use This?

- ✅ **Batteries Included** – JWT auth, 2FA, email verification, file storage, and key rotation already set up.
- ✅ **Code Reuse Made Easy** – Shared libraries mean no more copying boilerplate between projects.
- ✅ **Production-Ready by Default** – All components are built with real-world usage and scalability in mind.
- ✅ **Fast Start, Every Time** – Create a new backend app in minutes using the existing architecture and libs.
- ✅ **Open & Extensible** – Contributions welcome! Let’s refine this into a toolkit others can benefit from too.

## 🔐 Features at a Glance

### **Content Management**
- 🧩 Abstract base for content types with **built-in access control** (users, groups, roles).
- 🌍 **Multi-language support** out of the box — store and serve content in multiple locales.
- 🏷️ Configurable tagging system for flexible content organization.
- 📝 Prebuilt `Article` class for instant publishing workflows.

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

Check out the core library:
👉 [singularity-core](https://github.com/antistereov/singularity-core)

## Development Setup

### Prerequisites:
- JDK 21 or higher.
- MongoDB and Redis running locally or remotely.
- SMTP server for email verification.

### Dependency

Add the dependency to your `build.gradle.kts`, `build.gradle` or `pom.xml` if using Maven:

**For Kotlin DSL:**
```kotlin
implementation("io.stereov.singularity:content:<version>") // Check the maven status batch for the latest version
```

**For Gradle:**
```groovy
dependencies {
   implementation 'io.stereov.singularity:content:<version>' // Check the maven status batch for the latest version
}
```

**For Maven:**
```xml
<dependency>
   <groupId>io.stereov.singularity</groupId>
   <artifactId>content</artifactId>
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
