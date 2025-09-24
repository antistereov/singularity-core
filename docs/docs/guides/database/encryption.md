---
sidebar_position: 4
---

# Encryption

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Sensitive information like phone addresses or passwords should not be stored in the database as clear text.
This would cause a major security risk.

*Singularity* offers a way to encrypt these fields safely.
The [`SensitiveCrudService`](https://github.com/antistereov/singularity-core/tree/main/src/main/kotlin/io/stereov/singularity/database/encryption/service/SensitiveCrudService.kt)
provides methods encrypt and decrypt documents.

:::info Encryption vs. Hashing
Encryption is a **two-way process** that transforms data into an unreadable format (ciphertext). The original data can be recovered from the ciphertext using a secret key.

* **Primary Use:** Protecting confidential data and ensuring only authorized parties can read it.
* **Key Feature:** The original data **can be recovered** by using the correct key.
* **Example:** You can encrypt a document and later decrypt it to read its contents.

If you don't need to restore the initial value, check out [hashing](hashing.md).
:::

:::warning 
You cannot search encrypted data.
If you need to search fields, then consider using [searchable hashes](./hashing.md#searchable-fields).

You can use the searchable hashes for queries and store the sensitive data encrypted.
This way you retrieve the initial value as well.
:::

## Usage

### Encrypting Whole Documents

We can also encrypt the whole document by encapsulating the sensitive information in a separate data class.

#### 1. Creating a Sensitive Information Class

The first step is creating a class that contains sensitive information.
The provided fields will be encrypted before they get stored.


```kotlin
data class SensitiveCoolStuffData(
    val phone: String
)
```

#### 2. Creating a Decrypted Document Class

This class is only needed in the code, no objects of this class will be stored.
It needs to implement the [`SensitiveDocument`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/encryption/model/SensitiveDocument.kt) interface.

Make sure to override the sensitive field and specify the type generic.

```kotlin
data class CoolStuff(
    val id: ObjectId? = null,
    // override the sensitive field with the data class for the sensitive information
    override val sensitive: SensitiveCoolStuffData
    // add the type parameter for the data class
) : SensitiveDocument<SensitiveCoolStuffData>
```

#### 3. Creating an Encrypted Document Class

This is the actual document class that will be stored to the database.
It is the encrypted representation of the `CoolStuff` class.

It needs to implement the [`EncryptedSensitiveDocument`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/encryption/model/EncryptedSensitiveDocument.kt) interface.
Make sure to override the `id` and `sensitive` field and specify the type generic.

```kotlin
@Document(collection = "cool_stuff")
data class EncryptedCoolStuff(
    // Since this document is stored in the database, an ID is required.
    @Id override val _id: ObjectId? = null,
    // Now the type is Encrypted<SensitiveCoolData> - the data is encrypted.
    override val sensitive: Encrypted<SensitiveCoolStuffData>
) : EncryptedSensitiveDocument<SensitiveCoolStuffData>
```

#### 4. Creating a Repository

Create an interface based on the [`SensitiveCrudRepository`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/encryption/repository/SensitiveCrudRepository.kt).
It already includes the most relevant methods.

```kotlin
interface EncryptedCoolStuffRepository : SensitiveCrudRepository<EncryptedCoolStuffDocument>
```

#### 5. Creating a Service

Create a service class based on the [`SensitiveCrudService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/encryption/service/SensitiveCrudService.kt).
It already includes the base logic for storing and retrieving data.
It will return the document in decrypted form.

There are two methods you need to implement: `doDecrypt` and `doEncrypt` that specify how to map an encrypted to a decrypted; and the other way round.

```kotlin
@Service
class CoolStuffService(
    // Autowire the repository
    override val repository: EncryptedCoolStuffRepository,
    // Autowire EncryptionService, ReactiveMongoTemplate and EncryptionSecretService
    // All are included in Singularity
    override val encryptionService: EncryptionService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    override val encryptionSecretService: EncryptionSecretService,
) : SensitiveCrudService<
        // The type of sensitive data
        SensitiveCoolStuffData, 
        // The decrypted document
        CoolStuff,
        // The encrypted document
        EncryptedCoolStuff
>() {
    // Create an instance of logger
    override val logger = KotlinLogging.logger {}
    
    // This field will hold the class of SensitiveUserData in memory - must be the same as in the type generic
    override val sensitiveClazz = SensitiveCoolStuffData::class.java
    
    // This field will hold the class of EncryptedCoolStuff in memory - must be the same as in the type generic
    override val encryptedDocumentClazz = EncryptedCoolStuff::class.java

    // Implement the doEncrypt method that specifies how to create an encrypted version of the document.
    override suspend fun doDecrypt(
        // This is the encrypted document
        encrypted: EncryptedCoolStuff,
        // This is the already decrypted sensitive data
        decryptedSensitive: SensitiveCoolStuffData
    ) = CoolStuff(
        id = encrypted._id, 
        sensitive = decryptedSensitive
    )

    override suspend fun doEncrypt(
        // This is the decrypted document
        document: CoolStuff,
        // This is the already encrypted sensitive data
        encryptedSensitive: Encrypted<SensitiveCoolStuffData>
    ) = EncryptedCoolStuff(
        _id = document.id,
        sensitive = encryptedSensitive
    )
}
```

#### Usage

We will now create a controller that lets us query the `CoolStuff`.
The `SensitiveCrudService` takes all the decryption logic away from you and
lets you focus on what matters.

All the methods like `findById` return decrypted versions of the document by default.

```kotlin
@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val coolStuffService: CoolStuffService
) {
    
    @GetMapping("{id}")
    suspend fun getCoolStuffById(@PathVariable id: ObjectId): ResponseEntity<CoolStuffDocument> {
        // This will return the decrypted CoolStuff decrypted
        return ResponseEntity.ok(coolStuffService.findById(id))
    }
    
    @GetMapping
    suspend fun getCoolStuffPaginated(
        // Add the pageable parameter so you can customize page, size and sort
        pageable: Pageable
    ): ResponseEntity<Page<CoolStuff>> {
        // This will return the decrypted CoolStuff decrypted
        return ResponseEntity.ok(coolStuffService.findAllPaginated(pageable))
    }
}
```
### Encrypting Whole Documents With Searchability

As stated earlier, it is not possible to search values of the `SenstiveCoolData`.
If you want to search the `phone` field, you can create an extra `SearchableHash` field.
You can learn more about hashes [here](./hashing.md).

For this, you only need to add the extra field to the [`EncryptedCoolStuff`](#2-creating-a-decrypted-document-class):

```kotlin
@Document(collection = "cool_stuff")
data class EncryptedCoolStuff(
    @Id override val _id: ObjectId? = null,
    override val sensitive: Encrypted<SensitiveCoolStuffData>,
    
    // The new field:
    val phone: SearchableHash
) : EncryptedSensitiveDocument<SensitiveCoolStuffData>
```

And update the [`CoolStuffRepository`](#4-creating-a-repository) and [`CoolStuffService`](#5-creating-a-service):

```kotlin
interface EncryptedCoolStuffRepository : SensitiveCrudRepository<EncryptedCoolStuffDocument> {
    
    // Add a method to search by phone
    suspend fun findByPhone(phone: SearchableHash)
}

@Service
class CoolStuffService(
    override val repository: EncryptedCoolStuffRepository,
    override val encryptionService: EncryptionService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    override val encryptionSecretService: EncryptionSecretService,
    // Add the HashService
    private val hashService: HashService
) : SensitiveCrudService<SensitiveCoolStuffData, CoolStuff, EncryptedCoolStuff>() {
    // ... (rest of the service, including doEncrypt and doDecrypt) ...
    
    // Implement the doEncrypt method that specifies how to create an encrypted version of the document.
    override suspend fun doEncrypt(
        // This is the developer-facing object
        document: CoolStuff,
        // This is the already encrypted sensitive data, provided by the framework
        encryptedSensitive: Encrypted<SensitiveCoolStuffData>
    ): EncryptedCoolStuff {
        // 1. Create the searchable hash from the original string.
        val hashedPhone = hashService.hashSearchableHmacSha256(document.phone)

        // 2. Map all fields to the EncryptedCoolStuff object.
        return EncryptedCoolStuff(
            // Map the ID from the developer-facing object
            _id = document.id,
            // The framework has already encrypted this data for you
            sensitive = encryptedSensitive,
            // Map the newly created searchable hash
            phone = hashedPhone
        )
    }
        

    // A new method to find a document by its searchable hash
    suspend fun findByPhone(phone: String): CoolStuff? {
        val hashedPhone = hashService.hashSearchableHmacSha256(phone)

        // This query is performed on the database level
        val encryptedCoolStuff = repository.findByPhone(hashedPhone)

        // Return the decrypted document
        return encryptedCoolStuff?.let { decrypt(it) }
    }
}
```


