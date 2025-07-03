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

### **Performance**
- ⚙️ Kotlin Coroutines for async flows.
- 🚦 Configurable rate limiting (IP & user-based).

## 🔗 Related

Need a built-in content management backend? Check out the content library:
👉 [singularity-content](https://github.com/antistereov/singularity-content)

## 🚀 Getting Started

Want to bootstrap a new web backend project? Just add a new app to the `apps/` folder and hook into the existing libraries in `libs/` — all the groundwork is already done.

## Development Setup

### Prerequisites:
- JDK 21 or higher.
- MongoDB and Redis running locally or remotely.
- SMTP server for email verification.

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
