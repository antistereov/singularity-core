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

## Codes and Passwords

Here's how you can use `Hash` for fields like passwords:

```kotlin
/**
 * A document storing a hashed code.
 */
@Document(collection = "cool-stuff")
data class CoolStuff(
    @Id val id: Object? = null,
    val code: Hash
)

@Service
class CoolStuffService(
    // Autowire the HashService 
    private val hashService: HashService
) {
    
    suspend fun createCoolStuff(code: String): CoolStuff {
        // You can hash the code using this method
        val hashedCode = hashService.hashBcrypt(code)
        return CoolStuff(code = hashedCode)
    }
    
    suspend fun codeIsValid(coolStuff: CoolStuff, code: String): Boolean {
        // You can check if a given code is identical to the stored value
        return hashService.checkBcrypt(coolStuff.code, code)
    }
}
```

## Searchable Fields

In some cases, such as email addresses for example, you want to search for a given value.
The example above does not allow this. Every time you run `hashService.hashBcrypt(code)`,
a new hash will be generated that is different to the one before.

If you want to find the cool stuff by an associated email address, you can use the `SearchableHash`.

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
    val email: SearchableHash
)

/**
 * The repository interface.
 */
interface CoolStuffRepository : CoroutineCrudRespository<CoolStuff, ObjectId> {
    
    suspend fun findByEmail(hashedEmail: SearchableHash): CoolStuff?
}

@Service
class CoolStuffService(
    // Autowire the HashService 
    private val hashService: HashService,
    // Autowire the repository
    private val repository: CoolStuffRepository
) {
    
    suspend fun createCoolStuff(email: String): CoolStuff {
        // You can create a searchable hash of the email
        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        return CoolStuff(email = hashedEmail)
    }
    
    suspend fun findByEmail(email: String): CoolStuff? {
        // Create the same hash again
        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        return repository.findByEmail(hashedEmail)
    }
}

```
