---
sidebar_position: 1
---

# Quickstart

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

## 1. Create a new project

Use your favorite IDE or build tool to create a new Java or Kotlin project with JDK 21 or higher.

## 2. Add Singularity to your dependencies

[![Maven Central](https://img.shields.io/maven-central/v/io.stereov.singularity/core.svg?logo=apachemaven)](https://central.sonatype.com/artifact/io.stereov.singularity/core)

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

## 3. Start MongoDB and Redis

Download the [docker-compose.yaml](https://github.com/antistereov/singularity-core/blob/354c7258e0b6416b108639224fc075d51830198b/infrastructure/docker/docker-compose.yaml)
to the root of your project and run the containers using:

```bash
docker compose up -d
```

You have a running instance of MongoDB as your database and Redis as your Cache.

## 4. Create your application class

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
## 5. Expand

Add your custom services and features to create your dream. Focus on what matters.
