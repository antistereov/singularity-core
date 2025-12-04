---
sidebar_position: 3
---

# Hashing

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Sensitive information like email addresses or passwords should not be stored in the database as clear text.
This would cause a major security risk.

*Singularity* offers a way to store these fields safely.
The [`HashService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/hash/service/HashService.kt)
provides methods to hash, check and search hashes.

:::info Hashing vs. Encryption
Hashing is a **one-way process** used to transform data into a fixed-length string of characters. 
It is not reversible, 
which makes it ideal for storing sensitive information like passwords and for verifying data integrity.

* **Primary Use:** Storing passwords and verifying data has not been tampered with.
* **Key Feature:** The original data cannot be recovered from the hash.
* **Example:** You can check if a password is correct, but you cannot find the password from the stored hash.

If you need to restore the initial value, check out [encryption](./encryption.md).
:::

## Core Components

### `HashService`

The `HashService` is the primary component for secure, one-way data transformation within *Singularity*.

This service supports two main types of hashing:
1.  **BCrypt** for **passwords and codes** (designed for verification, not searchability).
2.  **HMAC-SHA256** for **searchable fields** (designed to be deterministic and searchable).

#### **Explanation**

The `HashService` class provides functions to:
* **Generate `Hash` objects** (using BCrypt) for security-critical data like passwords, where the hash changes every time it is generated, making rainbow table attacks impossible.
* **Generate `SearchableHash` objects** (using HMAC-SHA256) for data that needs to be securely stored but also queried (e.g., finding a user by a hashed email).
* **Verify** an input string against an existing hash or searchable hash.

It uses the `HashSecretService` to retrieve a secure, fixed secret for the HMAC-SHA256 operation, ensuring that the **`SearchableHash`** result is **deterministic** (the same input always produces the same hash).

#### **Core Signatures**

| Function                     | Description                                                                                                            | Signature                                                                                                     |
|------------------------------|------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `verifyBcrypt`               | Checks if the plain `input` string matches the stored **BCrypt** `hash`. Returns `true` on match.                      | `suspend fun verifyBcrypt(input: String, hash: Hash): Result<Boolean, HashException>`                         |
| `hashBcrypt`                 | Generates a new, randomized **BCrypt hash** from the `input` string. The result is non-deterministic.                  | `suspend fun hashBcrypt(input: String): Result<Hash, HashException>`                                          |
| `verifySearchableHmacSha256` | Checks if the plain `input` string matches the stored **SearchableHash**. Returns `true` on match.                     | `suspend fun verifySearchableHmacSha256(input: String, hash: SearchableHash): Result<Boolean, HashException>` |
| `hashSearchableHmacSha256`   | Generates a **deterministic SearchableHash** using HMAC-SHA256 and a secret key. Used for fields that must be queried. | `suspend fun hashSearchableHmacSha256(input: String): Result<SearchableHash, HashException>`                  |


### **Model Descriptions**

The `HashService` operates on two data models:

| Model                | Description                                                                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`Hash`**           | Used for passwords and codes. Contains only the `data: String` (the non-searchable, randomized hash).                                                 |
| **`SearchableHash`** | Used for searchable fields (e.g., email). Contains the deterministic `data: String` and a `secretId: UUID` to track the secret used for its creation. |

## Examples

### Codes and Passwords

Here's how you can use [`Hash`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/hash/model/Hash.kt) for fields like passwords. BCrypt is designed to be slow, so every time you call `hashService.hashBcrypt(code)`,
a new hash will be generated that is different to the one before.

:::caution Result Return Type
`HashService` methods now return a `Result<V, E>`. In the service layer, we use `coroutineBinding` and `bind()` to unwrap the successful value or propagate the error, typically a `HashException`.
:::

```kotlin
/**
 * A document storing a hashed code.
 */
@Document(collection = "cool-stuff")
data class CoolStuff(
    @Id val id: ObjectId? = null,
    // The field storing the BCrypt hash
    val code: Hash
)

/**
 * The repository interface.
 */
interface CoolStuffRepository : CoroutineCrudRepository<CoolStuff, ObjectId> {
    // ... custom methods
}

@Service
class CoolStuffService(
    private val hashService: HashService,
    private val repository: CoolStuffRepository
) {
    
    /**
     * Creates a new CoolStuff document with a BCrypt hashed code.
     * Uses coroutineBinding to handle and propagate HashExceptions or DatabaseExceptions.
     */
    suspend fun createCoolStuff(code: String): Result<CoolStuff, Exception> = coroutineBinding {
        // Hashing the code using BCrypt, which returns Result<Hash, HashException>.
        // bind() unwraps the Hash object or propagates HashException.
        val hashedCode = hashService.hashBcrypt(code).bind()
        
        val newCoolStuff = CoolStuff(code = hashedCode)
        
        // Assuming repository.save returns Result<CoolStuff, DatabaseException>.
        // bind() unwraps the saved document or propagates the database error.
        repository.save(newCoolStuff).bind()
    }

    /**
     * Checks if the input string matches the stored BCrypt hash.
     * Returns the Result<Boolean, HashException> directly.
     */
    suspend fun checkPassword(input: String, document: CoolStuff): Result<Boolean, HashException> {
        // checkBcrypt now returns Result<Boolean, HashException>, which we return directly.
        return hashService.checkBcrypt(input, document.code)
    }
}
```

### Searchable Hashing

If you want to find the cool stuff by an associated email address, you can use the [`SearchableHash`](https://www.google.com/search?q=https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/hash/model/SearchableHash.kt).

:::info
The `SearchableHash` will be generated using a secret.
This secret will make the result deterministic.
Every call with the same input and secret will return the same result.

Since it is a hash, the initial value cannot be retrieved again.
:::

```kotlin
/**
 * A document storing a hashed code.
 */
@Document(collection = "cool-stuff")
data class CoolStuff(
    @Id val id: ObjectId? = null,
    // The field storing the SearchableHash
    val email: SearchableHash
)

/**
 * The repository interface.
 */
interface CoolStuffRepository : CoroutineCrudRepository<CoolStuff, ObjectId> {
    
    suspend fun findByEmail(hashedEmail: SearchableHash): CoolStuff?
}

@Service
class CoolStuffService(
    // Autowire the HashService 
    private val hashService: HashService,
    // Autowire the repository
    private val repository: CoolStuffRepository
) {
    
    /**
     * Creates a new CoolStuff document with an HMAC-SHA256 searchable hash for the email.
     * Uses coroutineBinding to handle and propagate HashExceptions or DatabaseExceptions.
     */
    suspend fun createCoolStuff(email: String): Result<CoolStuff, Exception> = coroutineBinding {
        // hashSearchableHmacSha256 returns Result<SearchableHash, HashException>.
        val hashedEmail = hashService.hashSearchableHmacSha256(email).bind()
        
        val newCoolStuff = CoolStuff(email = hashedEmail)
        
        // Assuming repository.save returns Result<CoolStuff, DatabaseException>.
        repository.save(newCoolStuff).bind()
    }
    
    /**
     * Finds a CoolStuff document by email address.
     * The process hashes the input email and then queries the repository.
     * Returns the nullable document wrapped in Result<CoolStuff?, HashException>
     */
    suspend fun findByEmail(email: String): Result<CoolStuff?, HashException> = coroutineBinding {
        // Create the same hash again. bind() unwraps SearchableHash or propagates HashException.
        val hashedEmail = hashService.hashSearchableHmacSha256(email).bind()
        
        // Since the repository call returns a nullable document (CoolStuff?),
        // coroutineBinding automatically wraps this success in Ok(CoolStuff?).
        repository.findByEmail(hashedEmail)
    }.mapError { it as HashException } // Maps the generic error from coroutineBinding to the specific HashException.
}
```
