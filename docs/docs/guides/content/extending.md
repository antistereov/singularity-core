---
sidebar_position: 5
---


# Extending Content

:::note
This guide demonstrates how to create a new content type, `CoolStuff`, by integrating it into the abstract `ContentDocument` and leveraging the core services for access management and authorization.
:::

## 1. The CoolStuff Document

The `CoolStuffDocument` must extend `ContentDocument<CoolStuffDocument>` and include all required abstract fields (`key`, `access`, `trusted`, `tags`). We also define its specific content fields, like `name` and `coolLevel`.

```kotlin

@Document(collection = "cool_stuff")
data class CoolStuffDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) override var key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    
    // Core Content Fields
    override var access: ContentAccessDetails, // Manages owner, visibility, and roles
    override var trusted: Boolean,
    override var tags: MutableSet<String> = mutableSetOf(),
    
    // CoolStuff Specific Fields
    var name: String,
    var coolLevel: Int,
) : ContentDocument<CoolStuffDocument> {

    companion object {
        // Used by the generic ContentManagementController
        const val CONTENT_TYPE = "cool-stuff" 
    }
}
```

## 2. The CoolStuff Repository

We define a standard Spring Data repository interface for our new document.

```kotlin

// Note: ContentRepository is a marker/extension of the standard Spring Data repository
@Repository
interface CoolStuffRepository : ContentRepository<CoolStuffDocument> {
    // Custom query: find by a custom field
    suspend fun findByNameContaining(name: String): Flow<CoolStuffDocument>
}
```

## 3. The CoolStuff Response

:::info DTOs
It is good practice to create DTOs to send and receive information to your frontend.
This way you can hide some fields and add some aggregations to make it more readable.
:::

This DTO implements `ContentResponse<CoolStuffDocument>` and
contains the fields needed for the client, including the mapped ContentAccessDetailsResponse.

```kotlin
data class CoolStuffResponse(
// Core ContentDocument fields
val id: ObjectId,
val key: String,
val createdAt: Instant,
val updatedAt: Instant,
val trusted: Boolean,
val tags: Set<String>,
val access: ContentAccessDetailsResponse,

    // CoolStuffDocument specific fields
    val name: String,
    val coolLevel: Int,
) : ContentResponse<CoolStuffDocument>
```

## 4. Implementing a Mapper Function

This mapper provides the function to map your document to the new response DTO.

```kotlin
@Component
class CoolStuffMapper(
    private val contentAccessMapper: ContentAccessMapper // Dependency to map ContentAccessDetails
) {
    /**
     * Maps a CoolStuffDocument to the client-facing CoolStuffResponse DTO.
     */
    fun toResponse(doc: CoolStuffDocument): CoolStuffResponse {
        return CoolStuffResponse(
            id = doc.id,
            key = doc.key,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt,
            trusted = doc.trusted,
            tags = doc.tags.toSet(),
            // This makes the access details more readable
            access = contentAccessMapper.toResponse(doc.access),

            name = doc.name,
            coolLevel = doc.coolLevel,
        )
    }
}
```

## 5. The CoolStuff Service

The **`CoolStuffService`** extends the abstract **`ContentService`**,
which provides the core authorization utility (`findAuthorizedByKey`).
This means any content retrieval is automatically secured.

:::info Result Usage
Since the core `ContentService` methods now return a `Result<V, E>`, the service methods are updated to either 
propagate the `Result` or wrap their logic in `coroutineBinding { ... }` and use `bind()` 
to unwrap successful values and automatically propagate errors.

You can find more information about this design decision [here](../design-decisions.md).
:::

```kotlin

@Service
class CoolStuffService(
    override val repository: CoolStuffRepository,
    override val authorizationService: AuthorizationService,
    override val translateService: TranslateService,
    override val accessCriteria: AccessCriteria,
    // Add other required dependencies...
) : ContentService<CoolStuffDocument>() {

    override val logger = KotlinLogging.logger {}
    
    /**
     * Finds a CoolStuffDocument by key and ensures the current user has VIEWER access.
     * It now directly returns the Result from the core ContentService method.
     */
    suspend fun findCoolStuffByKey(key: String): Result<CoolStuffDocument, FindContentAuthorizedException> {
        // findAuthorizedByKey (from ContentService) handles the full authorization check:
        // 1. Checks if the object is PUBLIC.
        // 2. Checks if the user is the OWNER or explicitly has the VIEWER role (or higher).
        return findAuthorizedByKey(key, ContentAccessRole.VIEWER)
    }

    /**
     * Creates a new CoolStuffDocument. Requires the user to be a member of the CONTRIB group.
     * It uses coroutineBinding to handle and propagate exceptions from required membership checks and saving.
     */
    suspend fun createCoolStuff(key: String, ownerId: ObjectId): Result<CoolStuffDocument, Exception> = coroutineBinding {
        
        // requireEditorGroupMembership is assumed to return Result<Unit, AuthorizationException>.
        // bind() unwraps Unit on success or immediately returns the AuthorizationException on failure.
        requireEditorGroupMembership().bind()
        
        val newCoolStuff = CoolStuffDocument(
            key = key,
            access = ContentAccessDetails(ownerId), 
            trusted = false,
            name = "A new cool thing",
            coolLevel = 10
        )
        
        // repository.save is assumed to return Result<CoolStuffDocument, DatabaseException> or similar.
        // bind() unwraps the saved document or propagates the error.
        repository.save(newCoolStuff).bind()
    }
}
```

## 6. The CoolStuff Controller

The **`CoolStuffController`** exposes the read operations. We use `getOrThrow()` 
here to simplify the controller logic by allowing Spring's exception handler to 
process the errors propagated as `Result` failures from the service layer.

```kotlin

@RestController
@RequestMapping("/api/content/cool-stuff")
class CoolStuffController(
    private val coolStuffService: CoolStuffService,
    private val coolStuffMapper: CoolStuffMapper,
    private val context: ApplicationContext
) {
    
    // --- Public Retrieval Endpoint ---
    @GetMapping("/{key}")
    suspend fun getCoolStuffByKey(@PathVariable key: String): ResponseEntity<CoolStuffResponse> {
        // Authorization is handled by the service layer (findCoolStuffByKey), which returns a Result.
        // We use .getOrThrow() to unwrap the success value or let the exception bubble up.
        val coolStuffDocument = coolStuffService.findCoolStuffByKey(key)
            .getOrThrow()
            
        return ResponseEntity.ok(
            coolStuffMapper.toResponse(coolStuffDocument)
        )
    }
}
```

## 7. The CoolStuff Management Service

To gain all the default management endpoints (for changing visibility, updating the owner,
and setting the trusted state), we implement a **`CoolStuffManagementService`** that
extends the generic `ContentManagementService`.

By extending `ContentManagementService<CoolStuffDocument>`,
all the complex business logic for content management (like inviting users, checking permissions,
and updating access control lists) is automatically handled.
We update the abstract method implementations to use `coroutineBinding` and `bind()` to correctly handle the `Result` types now returned by the protected base methods (`do...`).

```kotlin

/**
 * This is the management service for CoolStuffDocument.
 * It's so cool that all basic content management functionalities (access control,
 * ownership updates, trusted state) are inherited from the generic ContentManagementService.
 *
 * It automatically provides implementations for:
 * - changeVisibility (PUT /{key}/visibility)
 * - updateOwner (PUT /{key}/owner)
 * - setTrustedState (PUT /{key}/trusted)
 */
@Service
class CoolStuffManagementService(
    override val contentService: CoolStuffService, // Assumed to extend ContentService<CoolStuffDocument>
    override val authorizationService: AuthorizationService,
    override val invitationService: InvitationService,
    override val userService: UserService,
    override val principalMapper: UserMapper,

    // Custom dependency
    private val coolStuffMapper: CoolStuffMapper,
) : ContentManagementService<CoolStuffDocument, CoolStuffResponse>() {

    override val logger = KotlinLogging.logger {}
    override val contentType = CoolStuffDocument.CONTENT_TYPE

    // --- Implementation of Missing Abstract Methods ---

    /**
     * Implementation for abstract suspend fun acceptInvitation.
     * Uses coroutineBinding to handle and propagate errors from core logic.
     */
    override suspend fun acceptInvitation(
        req: AcceptInvitationToContentRequest,
        locale: Locale?
    ): CoolStuffResponse = coroutineBinding {
        logger.debug { "Accepting invitation for CoolStuff with token ${req.token}" }

        // 1. Call the protected core logic method (doAcceptInvitation) which now returns a Result.
        // We use bind() to unwrap the document or propagate the error.
        val coolStuff = doAcceptInvitation(req).bind()

        // 2. Map the updated document to the CoolStuffResponse DTO
        coolStuffMapper.toResponse(coolStuff)
    }

    /**
     * Implementation for abstract suspend fun inviteUser.
     * Uses coroutineBinding to handle and propagate errors from core logic.
     */
    override suspend fun inviteUser(
        key: String,
        req: InviteUserToContentRequest,
        locale: Locale?
    ): ExtendedContentAccessDetailsResponse = coroutineBinding {
        logger.debug { "Inviting user to CoolStuff with key \"$key\"" }

        // 1. Call the protected core logic method (doInviteUser) which now returns a Result.
        val coolStuff = doInviteUser(key, req).bind()

        // 2. Call the base class's helper to create the final, non-generic ExtendedContentAccessDetailsResponse
        getExtendedAccessDetailsResponse(coolStuff)
    }

    /**
     * Implementation for abstractly suspend fun changeVisibility.
     * Uses coroutineBinding to handle and propagate errors from core logic.
     */
    override suspend fun changeVisibility(
        key: String,
        req: UpdateContentVisibilityRequest,
        locale: Locale?
    ): CoolStuffResponse = coroutineBinding {
        logger.debug { "Changing visibility of CoolStuff with key \"$key\"" }

        // 1. Call the protected core logic method (doChangeVisibility) which now returns a Result.
        val coolStuff = doChangeVisibility(key, req).bind()

        // 2. Map the updated document to the CoolStuffResponse DTO
        coolStuffMapper.toResponse(coolStuff)
    }
}
```

## Automatically Generated Endpoints

After implementing the `ContentManagementService<CoolStuffDocument>`,
the following endpoints will be available:

### Updating

#### Owner

The **owner** of a `CoolStuff` with given `key` can be updated through the endpoint:

* **Endpoint:** [`PUT /api/content/cool-stuff/{key}/owner`](../../api/update-content-object-owner.api.mdx)
* **Requirements:** Only the current owner is permitted to perform this action.

#### Access

The **access** can be updated through the endpoint:

* **Endpoint:** [`PUT /api/content/cool-stuff/{key}/access`](../../api/update-content-object-access.api.mdx)
* **Requirements:** Only [`MAINTAINER`](../content/introduction.md#object-specific-roles-shared-state) of the requested file
  can perform this action.

#### Trusted State

The **trusted state** can be updated through the endpoint

* **Endpoint:**  [`PUT /api/content/cool-stuff/{key}/trusted`](../../api/update-content-object-trusted-state.api.mdx)
* **Requirements:** Only [`ADMIN`](../principals/introduction.md#admins)s can perform this action.

### Deleting

A `CoolStuff` with given `key` can be deleted through the endpoint:

* **Endpoint:** [`DELETE /api/content/cool-stuff/{key}`](../../api/delete-content-object-by-key.api.mdx)
* **Requirements:** Only [`MAINTAINER`](../content/introduction.md#object-specific-roles-shared-state)s of the requested article
  can perform this action.