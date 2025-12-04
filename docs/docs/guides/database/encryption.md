---
sidebar_position: 4
---

# Encryption

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Sensitive information like phone addresses or passwords should not be stored in the database as clear text. This would cause a major security risk.

:::warning 
You cannot search encrypted data.
If you need to search fields, then consider using [searchable hashes](./hashing.md#searchable-hashing).
:::

## `SensitiveCrudService<S, T, E>`

The `SensitiveCrudService` is a specialized abstract service designed to handle documents where a portion of the data must be stored in an encrypted format in the database.

It performs **automatic encryption** upon save and **automatic decryption** upon retrieval, ensuring developers work with clear-text documents (`T`) while the persistence layer handles the encrypted format (`E`).

The service uses the following type parameters:
* **`S`**: The Sensitive Data Transfer Object (DTO) containing the fields to be encrypted.
* **`T`**: The clear-text, developer-facing document (`T: SensitiveDocument<S>`).
* **`E`**: The encrypted document stored in the database (`E: EncryptedSensitiveDocument<S>`).

This service extends `CrudService<E>`, which means it inherits all standard CRUD methods, but overrides the key retrieval and saving operations to handle the encryption/decryption process.

### Core CRUD Methods (Overridden for Decryption)

These methods operate on the **clear-text document (`T`)**, but internally handle the persistence of the encrypted document (`E`).

| Function           | Description                                                                                                                                              | Signature                                                                                                                             |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `save`             | Encrypts the sensitive data in `T`, persists the encrypted document (`E`), decrypts the result, and returns the persisted **clear-text document (`T`)**. | `suspend fun save(entity: T): Result<T, SaveEncryptedDocumentException>`                                                              |
| `findById`         | Retrieves the encrypted document (`E`) by ID, decrypts it, and returns the **clear-text document (`T`)**.                                                | `suspend fun findById(id: ObjectId): Result<T, FindEncryptedDocumentException>`                                                       |
| `findAll`          | Retrieves and decrypts all documents, returning them as a coroutine `Flow` of **clear-text documents (`T`)**.                                            | `suspend fun findAll(): Flow<T>`                                                                                                      |
| `findAllPaginated` | Retrieves a paginated list of encrypted documents (`E`), decrypts them, and returns a `Page` of **clear-text documents (`T`)**.                          | `suspend fun findAllPaginated(pageable: Pageable, criteria: Criteria?): Result<Page<T>, FindAllEncryptedDocumentsPaginatedException>` |


### Encryption/Decryption Helpers

These methods are used internally but can be called directly if needed for custom operations.

| Function  | Description                                                                                  | Signature                                                                   |
|-----------|----------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| `encrypt` | Converts a clear-text document (`T`) into its encrypted database representation (`E`).       | `suspend fun encrypt(document: T): Result<E, EncryptionException>`          |
| `decrypt` | Converts an encrypted database document (`E`) back into its clear-text representation (`T`). | `suspend fun decrypt(encryptedDocument: E): Result<T, DecryptionException>` |


### Abstract Mapping Methods

These two abstract methods **must be implemented** by the concrete service class. They define the business logic for mapping data between the clear-text and encrypted document types.

| Function    | Description                                                                                                                                                                                                                                                                   | Signature                                                                          |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| `doEncrypt` | **Mandatory:** Defines how to take the clear-text document (`T`) and the framework-provided **encrypted sensitive data** (`Encrypted<S>`) and combine them to create the final encrypted document (`E`) for storage. This is where you would calculate **searchable hashes**. | `abstract suspend fun doEncrypt(document: T, encryptedSensitive: Encrypted<S>): E` |
| `doDecrypt` | **Mandatory:** Defines how to take the encrypted database document (`E`) and the framework-provided **decrypted sensitive data** (`S`) and combine them to reconstruct the final clear-text document (`T`) for the application.                                               | `abstract suspend fun doDecrypt(encryptedDocument: E, decryptedSensitive: S): T`   |

## Example

### 1. Defining the Documents

For encryption, you need three parts:

1.  **Sensitive Data DTO (`SensitiveCoolStuffData`)**: The fields you want to encrypt.
2.  **Developer-Facing Document (`CoolStuff`)**: The document you work with in your code, which contains the clear-text sensitive data.
3.  **Encrypted Document (`EncryptedCoolStuff`)**: The actual MongoDB document that stores the encrypted sensitive data and any searchable fields.


```kotlin
// 1. The data that will be encrypted (Only visible in code, never in DB)
data class SensitiveCoolStuffData(
    val secret: String, // e.g., an API key
    val creditCard: String,
)

// 2. The developer-facing document that contains the clear-text sensitive data
@Document(collection = "cool-stuff")
data class CoolStuff(
    @Id val id: ObjectId? = null,
    val name: String,
    val phone: String, // This will be hashed for searching
    // Embed the clear-text sensitive data DTO
    override val sensitive: SensitiveCoolStuffData,
) : SensitiveDocument<SensitiveCoolStuffData>

// 3. The actual document stored in the database
@Document(collection = "cool-stuff")
data class EncryptedCoolStuff(
    @Id override val _id: ObjectId? = null,
    // This is the encrypted ciphertext provided by the framework
    override val sensitive: Encrypted<SensitiveCoolStuffData>,
    // The searchable hash
    val phone: SearchableHash,
) : EncryptedSensitiveDocument<SensitiveCoolStuffData>
```

### 2. The Sensitive Repository

The repository must be typed for the **encrypted** document, as this is what is persisted in the database.

```kotlin
interface CoolStuffRepository : SensitiveCrudRepository<EncryptedCoolStuff> {
    
    // You can query on searchable fields like the phone hash
    suspend fun findByPhone(hashedPhone: SearchableHash): EncryptedCoolStuff?
}
```

### 3. The CoolStuff Sensitive Service

The service extends `SensitiveCrudService`. 
This base class is **Result-aware**. Your service methods must use `coroutineBinding` and `bind()` to manage error propagation, 
ensuring the returned `Result`'s success channel (`Ok(V)`) is non-nullable.

:::warning Null Safety in Result
If an operation is expected to return a value but returns `null` (e.g., finding a document), 
this should be treated as a failure and mapped to an `Err(Exception)` rather than `Ok(null)`.
:::



```kotlin
@Service
class CoolStuffService(
    override val repository: CoolStuffRepository,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    // The hash service is needed to create the searchable hash
    private val hashService: HashService,
    // All other necessary dependencies for the base service
    override val encryptionService: EncryptionService,
    override val encryptionSecretService: EncryptionSecretService,
    override val objectMapper: ObjectMapper,
) : SensitiveCrudService<SensitiveCoolStuffData, CoolStuff, EncryptedCoolStuff>() {

    override val logger = KotlinLogging.logger {}
    override val sensitiveClazz = SensitiveCoolStuffData::class.java
    override val documentClazz = CoolStuff::class.java
    override val encryptedDocumentClazz = EncryptedCoolStuff::class.java


    /**
     * Creates a new CoolStuff document, encrypting sensitive data before saving.
     * Propagates any [EncryptionException], [HashException], or [DatabaseException].
     */
    suspend fun createCoolStuff(
        name: String, 
        phone: String, 
        secret: String
    ): Result<CoolStuff, Exception> = coroutineBinding {
        
        // 1. Create the document to be encrypted.
        val newCoolStuff = CoolStuff(
            name = name,
            phone = phone,
            sensitive = SensitiveCoolStuffData(secret = secret, creditCard = "4444..."),
        )

        // 2. Encrypt the sensitive data. encrypt() returns Result<EncryptedCoolStuff, EncryptionException>.
        val encrypted = encrypt(newCoolStuff).bind()

        // 3. Save the encrypted document. repository.save is assumed to return Result<EncryptedCoolStuff, DatabaseException>.
        val saved = repository.save(encrypted).bind()

        // 4. Decrypt the document before returning it. decrypt() returns Result<CoolStuff, DecryptionException>.
        decrypt(saved).bind()
    }


    /**
     * Implementation for the abstract method: Specifies how to create the database document
     * from the encrypted sensitive data and clear-text fields.
     *
     * It must handle any HashService errors and forward them as [EncryptionException].
     */
    override suspend fun doEncrypt(
        document: CoolStuff,
        encryptedSensitive: Encrypted<SensitiveCoolStuffData>
    ): Result<EncryptedCoolStuff, EncryptionException> = coroutineBinding {
        
        // 1. Create the searchable hash from the clear-text phone number.
        // hashSearchableHmacSha256 returns Result<SearchableHash, HashException>.
        val hashedPhone = hashService.hashSearchableHmacSha256(document.phone)
            // Map the specific HashException to a generic EncryptionException sub-type for domain consistency.
            .mapError { ex -> 
                EncryptionException.Hashing(
                    "Failed to create searchable hash for phone: ${ex.message}", 
                    ex
                ) 
            }
            .bind() // Propagates the mapped error via coroutineBinding.

        // 2. Map all fields to the EncryptedCoolStuff object.
        EncryptedCoolStuff(
            _id = document.id,
            sensitive = encryptedSensitive,
            phone = hashedPhone
        )
    }


    /**
     * Implementation for the abstract method: Specifies how to create the clear-text document
     * from the decrypted sensitive data and other fields in the database document.
     */
    override suspend fun doDecrypt(
        encryptedDocument: EncryptedCoolStuff,
        decryptedSensitive: SensitiveCoolStuffData
    ): Result<CoolStuff, EncryptionException> {
        return Ok(
            CoolStuff(
                id = encryptedDocument._id, 
                name = "name from a non-encrypted field, if it existed",
                sensitive = decryptedSensitive,
                // Cannot retrieve clear-text phone from hash, so we leave it empty or map from another field
                phone = "Decrypted document (phone unavailable)", 
            )
        )
    }

    /**
     * Finds a CoolStuff document by email address.
     * Returns [Result<CoolStuff, Exception>] - no nullable success type.
     */
    suspend fun findByPhone(phone: String): Result<CoolStuff, Exception> = coroutineBinding {
        // 1. Hash the search input
        val hashedPhone = hashService.hashSearchableHmacSha256(phone)
            .mapError { ex -> 
                EncryptionException.Hashing(
                    "Failed to hash search term: ${ex.message}", 
                    ex
                ) 
            }
            .bind() // Propagates HashException.

        // 2. Query the repository for the encrypted document
        val encryptedCoolStuff = repository.findByPhone(hashedPhone)
            // If the repository returns null, we throw an exception to exit the coroutineBinding block
            // and return an Err result, satisfying the non-nullable Ok requirement.
            ?: throw FindDocumentException.NotFound("CoolStuff document not found by phone hash")

        // 3. Decrypt it. decrypt() returns Result<CoolStuff, DecryptionException>.
        decrypt(encryptedCoolStuff).bind() // Propagates DecryptionException.
    }.mapError {
        // This ensures the exception thrown (e.g., FindDocumentException.NotFound)
        // or the one propagated via bind() (e.g., EncryptionException) is returned.
        it as Exception 
    }
}
```