---
sidebar_position: 2
---

# Usage

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

In *Singularity*, you'll encounter three key parts for data management:

- **Document**: This is the data class that represents a MongoDB document. 
    It's often a `data class` in Kotlin, and you'll annotate it with `@Document` to mark it as a persistent entity. 
    You can also use other annotations like `@Id` to specify the primary key field, and `@Field` to define a custom name for a field in the database.

- **Repository**: This is an interface that extends a Spring Data repository, such as `CoroutineCrudRepository`. 
    It provides a set of standard CRUD (Create, Read, Update, Delete) operations out-of-the-box, 
    saving you from writing boilerplate code. 
    Spring Data automatically generates the implementation for this interface at runtime. 
    You can also define custom query methods by following specific naming conventions or using the `@Query` annotation.

- **Service**: The service layer acts as the business logic and orchestrates interactions between the repository and other parts of the application. 
    It typically injects the repository and uses its methods to perform data-related tasks. 
    This separation of concerns keeps your business logic clean and independent of the data persistence details.

## Core Components

*Singularity* provides base service interfaces for all database interactions. These interfaces abstract away the data persistence details and are built around the **`Result`** type for robust error handling.

### Core Document Models

| Model         | Description                                                                                                                                                                                                                                           |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`WithId`**  | The fundamental interface for all database documents, marking them with an internal MongoDB primary key, `_id` of type `ObjectId`. It provides a property to safely retrieve the non-nullable ID via a `Result<ObjectId, DocumentException.Invalid>`. |
| **`WithKey`** | Extends `WithId` by adding an external, application-level unique identifier, **`key`** of type `String`. This is often used for user-facing lookups (e.g., a slug or human-readable ID).                                                              |

#### `WithId`

This interface is the base for documents stored in MongoDB, providing the primary key field.

| Property | Signature                                             | Description                                                                                                                                                                                         |
|----------|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `_id`    | `val _id: ObjectId?`                                  | The internal MongoDB primary key. It is nullable before the document is first saved.                                                                                                                |
| `id`     | `val id: Result<ObjectId, DocumentException.Invalid>` | A read-only property that attempts to unwrap the non-nullable `_id`. Returns a **`Result`** to handle the case where the ID is missing (e.g., if you try to use the ID before saving the document). |

#### `WithKey`

This interface extends `WithId` and adds a unique application-level key for easy, public lookups.

| Property | Signature                                             | Description                                                                                                    |
|----------|-------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| `key`    | `val key: String`                                     | The **unique, application-level key** used for external lookups and identification (e.g., a URL slug or UUID). |
| `_id`    | `val _id: ObjectId?`                                  | Inherited from `WithId`.                                                                                       |
| `id`     | `val id: Result<ObjectId, DocumentException.Invalid>` | Inherited from `WithId`.                                                                                       |

### `CrudService<T: WithId>`

This is the base service interface for documents identified by an internal `ObjectId`.

| Function           | Description                                                                                                               | Signature                                                                                                                    |
|--------------------|---------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `save`             | Saves or updates a document in the database, returning the persisted document or a `SaveDocumentException`.               | `suspend fun save(entity: T): Result<T, SaveDocumentException>`                                                              |
| `findById`         | Retrieves a document by its primary `ObjectId`.                                                                           | `suspend fun findById(id: ObjectId): Result<T, FindDocumentException>`                                                       |
| `existsById`       | Checks if a document with the given `ObjectId` exists.                                                                    | `suspend fun existsById(id: ObjectId): Result<Boolean, ExistsDocumentException>`                                             |
| `deleteById`       | Deletes a document by its `ObjectId`, returning `Unit` on success.                                                        | `suspend fun deleteById(id: ObjectId): Result<Unit, DeleteDocumentException>`                                                |
| `findAll`          | Retrieves all documents as a reactive Kotlin `Flow<T>`.                                                                   | `suspend fun findAll(): Flow<T>`                                                                                             |
| `findAll(sort)`    | Retrieves all documents as a reactive Kotlin `Flow<T>`, sorted by the provided `Sort` criteria.                           | `suspend fun findAll(sort: Sort): Flow<T>`                                                                                   |
| `findAllPaginated` | Retrieves documents with pagination (page, size, sort) and optional MongoDB criteria, returning a `Result<Page<T>, ...>`. | `suspend fun findAllPaginated(pageable: Pageable, criteria: Criteria?): Result<Page<T>, FindAllDocumentsPaginatedException>` |

### `CrudServiceWithKey<D: WithKey>`

This interface extends `CrudService<D>` and adds methods for documents that include a unique string `key`.

| Function      | Description                                                                 | Signature                                                                             |
|---------------|-----------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `findByKey`   | Retrieves a document by its unique string `key`.                            | `suspend fun findByKey(key: String): Result<D, FindDocumentByKeyException>`           |
| `existsByKey` | Checks if a document with the given string `key` exists.                    | `suspend fun existsByKey(key: String): Result<Boolean, ExistsDocumentByKeyException>` |
| `deleteByKey` | Deletes a document by its unique string `key`, returning `Unit` on success. | `suspend fun deleteByKey(key: String): Result<Unit, DeleteDocumentByKeyException>`    |

## Example

Let us store `CoolStuff` in the database!

### 1. Creating a Document Class

```kotlin
// Use the @Document annotation and specify the collection to store the documents in
@Document(collection = "cool_stuff")
data class CoolStuffDocument(
    /**
     * Annotate the ID field with @Id.
     * You have to make the ID field nullable. This influences the behavior of saving or updating documents:
     * - If the ID is null, when saved, a new document will be created and the ID will be set by MongoDB.
     * - If set, MongoDB updates the document with the given ID or creates this document no with this ID existed.
     */
    @Id override var _id: ObjectId? = null,
    /**
     * You can add more fields that will be stored in the document.
     */
    val name: String,
    val description: String,
    /**
     * You can even add complex datatypes.
     */
    val secrets: CoolStuffSecrets
) : WithId

/**
 * You can create complex datatypes that can be embedded in the document.
 */
data class CoolStuffSecrets(
    val secrets: List<String>,
)
```

:::info IDs
Please note that the `_id` field is nullable.
The `WithId` interface contains the getter for `id` which returns `Result<ObjectId, DocumentException.Invalid>`.
This allows accessing the `id` property safely.
:::

### 2\. Creating a Repository

```kotlin
// Create a repository and specify the document type (CoolStuffDocument) and the ID type (ObjectID).
interface CoolStuffRepository : CoroutineCrudRepository<CoolStuffDocument, ObjectId> {

    /**
     * Methods like findById or deleteAll already exist by default.
     * You can add custom queries specifying a field
     */
    suspend fun existsByName(name: String): Boolean
    suspend fun findByNameOrNull(name: String): CoolStuffDocument?

    /**
     * You can also create custom queries:
     * This query finds documents where the 'name' field contains the specified string.
     * The 'i' option makes the search case-insensitive.
     */
    @Query("{ 'name' : { \$regex: ?0, \$options: 'i' } }")
    suspend fun findByNameContaining(name: String): Flow<CoolStuffDocument>
}
```

### 3. Creating a Service

:::info
Although you can already use the repository for all tasks,
it is highly recommended to create a service class.

This class contains more business logins or custom logging that cannot be specified in the repository.

Note that methods inherited from `CrudService`, such as `findById` and `findAllPaginated`, return a `Result<V, E>` type. You must handle this result in your calling code.
:::

*Singularity* provides a built-in [`CrudService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/database/core/service/CrudService.kt).
It already implements useful methods, such as `findById`, `deleteAll` or even `findAllPaginated`
which allows you to specify a page number, size, sorting and extra criteria.

```kotlin
// Create a new class with the @Service annotation.
@Service
class CoolStuffService(
    // Autowire the repository you created.
    override val repository: CoolStuffRepository,
    // Autowire the ReactiveMongoTemplate. This bean will be created by Singularity.
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
): CrudService<CoolStuffDocument> {
    
    //You need to override a few more properties:
    override val logger = KotlinLogging.logger {}
    override val clazz = CoolStuffDocument::class.java
    
    // You can now add some extra methods
    suspend fun findByNameContaining(name: String): Flow<CoolStuffDocument> {
        logger.debug { "Finding CoolStuff with name containing $name" }
        
        // Here you call the function you called in your repository.
        // Repository functions usually return Flow or nullable objects, not Result,
        // so they can be called directly.
        return repository.findByNameContaining(name)
    }
    
}
```

### Usage

We will now create a controller that lets us query the `CoolStuff`.

:::info Result Handling in Controller
The methods `findById` and `findAllPaginated` now return `Result`s. In a REST controller,
it's often simplest to use **`.getOrThrow()`** to unwrap the success value. 
If the service call results in an error (`Err`), `getOrThrow()` throws the contained exception, 
which can then be caught and processed by Spring's global exception handler (e.g., to return a 404 or 500 status).
:::

```kotlin
@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val coolStuffService: CoolStuffService
) {
    
    @GetMapping("{id}")
    suspend fun getCoolStuffById(@PathVariable id: ObjectId): ResponseEntity<CoolStuffDocument> {
        // findById returns Result<CoolStuffDocument, FindDocumentException>.
        // We use .getOrThrow() to unwrap the document or throw an exception on failure.
        val coolStuff = coolStuffService.findById(id).getOrThrow()
        return ResponseEntity.ok(coolStuff)
    }
    
    @GetMapping
    suspend fun getCoolStuffPaginated(
        // Add the pageable parameter so you can customize page, size and sort
        pageable: Pageable
    ): ResponseEntity<Page<CoolStuffDocument>> {
        // findAllPaginated returns Result<Page<CoolStuffDocument>, FindAllDocumentsPaginatedException>.
        val coolStuffPage = coolStuffService.findAllPaginated(pageable).getOrThrow()
        return ResponseEntity.ok(coolStuffPage)
    }
}
```

You can query `CoolStuff` now like this `GET http://localhost:8000/api/cool-stuff?page=0&size=10&sort=name,asc`.
This will return you the first 10 documents, sorted by name.
