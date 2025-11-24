---
sidebar_position: 13
---

# Errors

## Design Decision

This paragraph outlines the rationale behind our chosen error-handling strategy, 
which prioritizes **explicitness** and **type safety** across our Spring WebFlux application by leveraging a 
custom `Result` monad implementation.

:::note
We use [Michael Bull's implementation](https://github.com/michaelbull/kotlin-result) of `Result` monads.

If you want to understand why we prefer this implementation over the `koltin.Result` from the standard library,
check out [this paragraph](https://github.com/michaelbull/kotlin-result?tab=readme-ov-file#2-why-not-use-kotlinresult-from-the-standard-library).
:::


### The Problem with Unchecked Exceptions

Traditional Spring/JVM applications rely heavily on **unchecked exceptions** (`RuntimeException`). 
While convenient for "fail-fast" scenarios, this model suffers from a significant drawback in API design: 
**lack of visibility**.

:::warning
Unchecked exceptions are **not visible** in method signatures. This forces developers to "guess" or manually 
trace through the entire call stack to determine which errors a method might throw. 
This leads to brittle code, missing error states in the frontend, and a high risk of 
unexpected `500 Internal Server Errors` when an exception is thrown but not explicitly mapped.
:::

### Explicit Error Handling via Result

We adopt the `Result` monad as the primary means of signaling potential failure 
in our **Service Layer**. 
This approach enforces **compile-time error handling** without 
sacrificing the concurrency benefits of Kotlin Coroutines.

#### The Core Strategy

| Component               | Implementation                                        | Rationale                                                                                                                                                                                                                                                                                   |
|:------------------------|:------------------------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Domain Errors**       | **`sealed class DomainError`**                        | **Type Safety:** Domain-specific errors (e.g., `ArticleNotFound`, `InvalidCredentials`) are defined as concrete data objects/classes within a single, descriptive sealed class per module. These errors are the **Single Source of Truth** for error codes and HTTP statuses.               |
| **Service Methods**     | **`suspend fun fetch(): Result<T, out DomainError>`** | **Explicitness:** By returning `Result<T, E>`, the compiler forces the calling code to acknowledge and handle (or map) the potential failure (`E`). Errors cannot be forgotten.                                                                                                             |
| **Controller Boundary** | **`.getOrThrow()`**                                   | **Pragmatism:** In the Controller, we use a custom extension function (`getOrThrow`) to exit the functional `Result` chain and explicitly throw a **technical wrapper exception** (`ApiException`). This is the necessary transition to utilize Spring's exception handling infrastructure. |
| **HTTP Conversion**     | **`@ControllerAdvice`**                               | **Centralization:** A single, global `@ControllerAdvice` catches all `SingularityException` instances and converts the internal, typsafe `DomainError` payload into a standard HTTP `ResponseEntity` (correct status code, error body).                                                     |

#### Error Structuring (Per Module)

Each module (e.g., `auth`, `file`) defines its own specific sealed error class:

* `TokenService`: Returns `Result<Token, TokenException>`
* `ArticleService`: Returns `Result<Article, ArticleError>`

This structure provides **clear ownership** and **separation of concerns** for error definition.

### Benefits for Documentation (Springdoc / OpenAPI)

To document these now-hidden errors effectively for OpenAPI generation, 
we use a Custom Annotation combined with a [Springdoc](https://springdoc.org/) `OperationCustomizer`:

:::info
By applying the **`@ThrowsDomainError`** annotation to our controller endpoints and 
referencing the relevant Domain Error sealed classes (e.g., `@ThrowsDomainError([AuthenticationError::class, ArticleError::class])`), we ensure that:

1.  The `OperationCustomizer` uses **Kotlin Reflection** to scan all subclasses of the referenced sealed classes.
2.  All concrete error codes, descriptions, and HTTP statuses are automatically injected into the API documentation.

This keeps our documentation in **perfect sync** with the actual application code, eliminating manual `@ApiResponse` boilerplate.
:::

## Reference

### File

#### UNSUPPORTED_MEDIA_TYPE

* **Status:** `415 Unsupported Media Type`
* **Description:**
  Thrown to indicate that an operation failed due to an unsupported media type.
  This exception is specifically used to represent scenarios where a provided
  media type is not compatible or allowed in the current context of file operations.

#### FILE_METADATA_FAILURE

* **Status:** `500 Internal Server Error`
* **Description:** 
  Thrown to indicate that a failure occurred during the handling of file metadata.
  This exception is specifically used to represent scenarios where an operation
  involving file metadata cannot be completed successfully due to an error.

#### FILE_KEY_TAKEN

* **Status:** `409 Conflict`
* **Description:**
  This exception is thrown when attempting to use a file key that is already taken or in use.


#### FILE_METADATA_OUT_OF_SYNC

* **Status:** `500 Internal Server Error`
* **Description:**
  Represents an exception thrown when file metadata saved in the database is out of sync 
  with the expected state of the actual file.
