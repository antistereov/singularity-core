---
description: Learn more about principals.
sidebar_position: 1
---

# Introduction

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity*'s security core is built around the **Principal** concept. 
Principals represent any entity that can interact with the system, and in *Singularity*, 
there are two distinct types: **users** and **guests**.

## Principal Model

The core of the system is the **`Principal`** sealed interface, 
which defines the common contract for any authenticated or semi-authenticated entity.

```kotlin
sealed interface Principal<R: Role, S: SensitivePrincipalData> : SensitiveDocument<S> {
    val createdAt: Instant
    var lastActive: Instant
    val roles: Set<R>
    val groups: Set<String>
    override val sensitive: S
    
    // ... common methods
}
```

The two concrete implementations of this interface are:

1. **`User`**: Represents a fully registered account with persistent authentication details (password, OAuth2, etc.) 
  and is associated with a `SensitiveUserData` object.
2. **`Guest`**: Represents a temporary, session-based account 
  that does not store persistent authentication credentials and is associated with a `SensitiveGuestData` object.

## `PrincipalService`

The **`PrincipalService`** is the primary service for interacting with all 
types of principals (`User` and `Guest`) as a unified entity. 
It deals with the generic `Principal` type.

| Operation        | Description                                                            | Kotlin Signature                                                                                                                                                       |
|:-----------------|:-----------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`findById`**   | Finds a principal (either `User` or `Guest`) by its unique `ObjectId`. | `suspend fun findById(id: ObjectId): Result<Principal<out Role, out SensitivePrincipalData>, FindPrincipalByIdException>`                                              |
| **`findAll`**    | Retrieves a stream (Flow) of all principals in the system.             | `suspend fun findAll(): Result<Flow<Result<Principal<out Role, out SensitivePrincipalData>, EncryptionException>>, DatabaseException.Database>`                        |
| **`save`**       | Saves a principal (either `User` or `Guest`).                          | `suspend fun save(document: Principal<out Role, out SensitivePrincipalData>): Result<Principal<out Role, out SensitivePrincipalData>, SaveEncryptedDocumentException>` |
| **`deleteById`** | Deletes a principal, regardless of whether it is a `User` or `Guest`.  | `suspend fun deleteById(id: ObjectId): Result<Unit, DeleteEncryptedDocumentByIdException>`                                                                             |
| **`deleteAll`**  | Deletes all principals (both users and guests).                        | `suspend fun deleteAll(): Result<Unit, DeleteAllEncryptedDocumentsException>`                                                                                          |

## Users

Every registered account has the `USER` role. This is the standard role in *Singularity*. 
`USER`s can access resources that are not dealing with server administration or security.

### `UserService`

The **`UserService`** is dedicated to managing and querying only **`User`** entities. 
This service provides specialized methods for querying based on user-specific attributes like email and for paginated access.

| Operation                    | Description                                                                                                                       | Kotlin Signature                                                                                                                                                                                                                                                                           |
|:-----------------------------|:----------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`findById`**               | Finds and decrypts a **`User`** by its `ObjectId`.                                                                                | `suspend fun findById(id: ObjectId): Result<User, FindEncryptedDocumentByIdException>`                                                                                                                                                                                                     |
| **`findByEmail`**            | Finds a **`User`** by their email address (uses a hashed search).                                                                 | `suspend fun findByEmail(email: String): Result<User, FindUserByEmailException>`                                                                                                                                                                                                           |
| **`findByProviderIdentity`** | Finds a **`User`** by their external provider identity (e.g., OAuth2 principal ID).                                               | `suspend fun findByProviderIdentity(provider: String, principalId: String): Result<User, FindUserByProviderIdentityException>`                                                                                                                                                             |
| **`existsByEmail`**          | Checks if a **`User`** with the specified email exists (uses a hashed search).                                                    | `suspend fun existsByEmail(email: String): Result<Boolean, ExistsUserByEmailException>`                                                                                                                                                                                                    |
| **`findAllPaginated`**       | Retrieves a paginated list of **`User`** entities with advanced filtering options (email, roles, groups, timestamps, identities). | `suspend fun findAllPaginated(pageable: Pageable, email: String?, roles: Set<Role>?, groups: Set<String>?, createdAtBefore: Instant?, createdAtAfter: Instant?, lastActiveBefore: Instant?, lastActiveAfter: Instant?, identityKeys: Set<String>?): Result<Page<User>, GetUsersException>` |
| **`findAll`**                | Retrieves a stream (Flow) of all decrypted **`User`** entities.                                                                   | `suspend fun findAll(): Result<Flow<Result<User, EncryptionException>>, DatabaseException.Database>`                                                                                                                                                                                       |
| **`save`**                   | Encrypts and saves a new or existing **`User`** document to the database.                                                         | `suspend fun save(document: User): Result<User, SaveEncryptedDocumentException>`                                                                                                                                                                                                           |
| **`saveAll`**                | Encrypts and saves a list of **`User`** documents to the database.                                                                | `suspend fun saveAll(documents: List<User>): Result<List<User>, SaveAllEncryptedDocumentsException>`                                                                                                                                                                                       |
| **`deleteById`**             | Deletes a **`User`** by its unique `ObjectId`.                                                                                    | `suspend fun deleteById(id: ObjectId): Result<Unit, DeleteEncryptedDocumentByIdException>`                                                                                                                                                                                                 |
| **`deleteAll`**              | Deletes all **`User`** documents from the database.                                                                               | `suspend fun deleteAll(): Result<Unit, DeleteAllEncryptedDocumentsException>`                                                                                                                                                                                                              |

### Admins

#### Granting Admin Permissions

You can grant admin permissions to a `USER` through the endpoint [`POST /api/admins/{user-id}`](../../api/grant-admin-permissions.api.mdx).

This action can only be performed by users who already have `ADMIN` privileges.

#### Revoking Admin Permissions

You can revoke admin permissions from a `USER` through the endpoint [`DELETE /api/admins/{user-id}`](../../api/revoke-admin-permissions.api.mdx).

This action can only be performed by users who already have `ADMIN` privileges.

## Guests

*Singularity* allows you to create **`GUEST`** accounts.
These accounts live only in one browser session and will be invalid as soon as the browser cache is emptied. 
`GUEST`s don't store any persistent authentication details like `email` or `password`.

### `GuestService`

The **`GuestService`** is dedicated to managing and querying only **`Guest`** entities. It handles the creation of new guest accounts.

| Operation              | Description                                                                                                                                | Kotlin Signature                                                                                                                          |
|:-----------------------|:-------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------------------------------|
| **`createGuest`**      | Creates and saves a new **`Guest`** principal in the system.                                                                               | `suspend fun createGuest(req: CreateGuestRequest): Result<Guest, SaveEncryptedDocumentException>`                                         |
| **`findById`**         | Finds and decrypts a **`Guest`** by its `ObjectId`. This method is specialized to ensure the document found is a `Guest` and not a `User`. | `suspend fun findById(id: ObjectId): Result<Guest, FindEncryptedDocumentByIdException>`                                                   |
| **`findAllPaginated`** | Retrieves all **`Guest`** documents matching the specified criteria in a paginated manner.                                                 | `suspend fun findAllPaginated(pageable: Pageable, criteria: Criteria?): Result<Page<Guest>, FindAllEncryptedDocumentsPaginatedException>` |
| **`findAll`**          | Retrieves a stream (Flow) of all decrypted **`Guest`** entities.                                                                           | `suspend fun findAll(): Result<Flow<Result<Guest, EncryptionException>>, DatabaseException.Database>`                                     |
| **`save`**             | Encrypts and saves a new or existing **`Guest`** document to the database.                                                                 | `suspend fun save(document: Guest): Result<Guest, SaveEncryptedDocumentException>`                                                        |
| **`saveAll`**          | Encrypts and saves a list of **`Guest`** documents to the database.                                                                        | `suspend fun saveAll(documents: List<Guest>): Result<List<Guest>, SaveAllEncryptedDocumentsException>`                                    |
| **`deleteById`**       | Deletes a **`Guest`** by its unique `ObjectId`.                                                                                            | `suspend fun deleteById(id: ObjectId): Result<Unit, DeleteEncryptedDocumentByIdException>`                                                |
| **`deleteAll`**        | Deletes all **`Guest`** documents from the database.                                                                                       | `suspend fun deleteAll(): Result<Unit, DeleteAllEncryptedDocumentsException>`                                                             |

### Converting Guest Accounts To User Accounts

It is possible to convert a `GUEST` account to a `USER` account. This can be done in two ways:

#### Adding Password Authentication

A `GUEST` can specify a username and password through [`POST /api/guests/convert-to-user`](../../api/convert-guest-to-user.api.mdx). 
This allows the user to log in with the specified `username` and `password`.

#### Connecting OAuth2 Provider

:::note
You can learn more about OAuth2 in *Singularity* [here](https://www.google.com/search?q=./oauth2).
:::

By connecting an OAuth2 provider to a `GUEST` account, this account automatically becomes a `USER` account. 
The process of connecting an OAuth2 provider is the same as for users. 
You can follow [this](../auth/oauth2.md#connecting-an-oauth2-provider-to-an-existing-account) guide.

## Roles

**Roles** in *Singularity* allow you to control access and management of your server. There are three primary roles:

- **`USER`:** A "normal" user of your application. Can access most of the resources but is not allowed to manage your server.
- **`ADMIN`:** An administrator. Has access to all resources and is allowed to manage your server. Only `USER`s can be administrators.
- **`GUEST`:** A user that has access to your application only in the current browser context and did not set up any authentication method.

:::info
Roles are primarily used to control and limit resources related to server management. If you want to have a more fine-grained control on which user can access what resource, check out [groups](https://www.google.com/search?q=./groups.md).
:::