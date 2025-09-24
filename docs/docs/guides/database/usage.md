---
sidebar_position: 2
---

# Usage

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

In *Singularity*, you'll encounter three key components for data management:

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
    @Id val id: ObjectId? = null,
    /**
     * You can add more fields that will be stored in the document.
     */
    val name: String,
    val description: String,
    /**
     * You can even add complex datatypes.
     */
    val secrets: CoolStuffSecrets
)

/**
 * You can create complex datatypes that can be embedded in the document.
 */
data class CoolStuffSecrets(
    val secrets: List<String>,
)
```

### 2. Creating a Repository

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
it is highly recommended to create a service class that.

This class contains more business login or custom logging that cannot be specified in the repository.
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
    suspend fun findByNameContaining(name: String): Flow<CoolStuff> {
        logger.debug { "Fining CoolStuff with name containing $name" }
        
        // Here you call the function you called in your repository.
        return repository.findByNameContaining(name)
    }
    
}
```

### Usage

We will now create a controller that lets us query the `CoolStuff`.

```kotlin
@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val coolStuffService: CoolStuffService
) {
    
    @GetMapping("{id}")
    suspend fun getCoolStuffById(@PathVariable id: ObjectId): ResponseEntity<CoolStuffDocument> {
        return ResponseEntity.ok(coolStuffService.findById(id))
    }
    
    @GetMapping
    suspend fun getCoolStuffPaginated(
        // Add the pageable parameter so you can customize page, size and sort
        pageable: Pageable
    ): ResponseEntity<Page<CoolStuff>> {
        return ResponseEntity.ok(coolStuffService.findAllPaginated(pageable))
    }
}
```

You can query `CoolStuff` now like this `GET http://localhost:8000/api/cool-stuff?page=0&size=10&sort=name,asc`.
This will return you the first 10 documents, sorted by name.
