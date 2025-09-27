---
sidebar_position: 1
---

# Introduction

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::


## Core Content Object Structure

Every piece of **Content** in the system is an abstract object that implements the `ContentDocument<T>` interface. It contains not only the access configuration but also crucial metadata for identification, versioning, and security.

The following fields are common to all content types (e.g., `Article`, `FileMetadata`):

| Field           | Type                   | Description                                                                                                  |
|:----------------|:-----------------------|:-------------------------------------------------------------------------------------------------------------|
| **`id`**        | `ObjectId`             | The unique internal MongoDB ID for the document.                                                             |
| **`key`**       | `String`               | The unique, stable key used to identify and retrieve the object within the system.                           |
| **`createdAt`** | `Instant`              | The timestamp when the content object was first created.                                                     |
| **`updatedAt`** | `Instant`              | The timestamp of the last modification to the content object.                                                |
| **`trusted`**   | `Boolean`              | A server-admin controlled flag, crucial for security (e.g., ensuring integrity for critical links or files). |
| **`tags`**      | `MutableSet<String>`   | Tags used for filtering, categorization, and discovery. You can learn more [here](tags.md).                  |
| **`access`**    | `ContentAccessDetails` | The complete object detailing ownership, visibility, and explicit sharing permissions.                       |


## Detailed Access Structure

The core of the access control is encapsulated in the **`ContentAccessDetails`** object, which is part of every `ContentDocument`.

| Field             | Type                       | Description                                                                                   |
|:------------------|:---------------------------|:----------------------------------------------------------------------------------------------|
| **`ownerId`**     | `ObjectId`                 | The unique ID of the user who owns the content. The Owner implicitly has `MAINTAINER` rights. |
| **`visibility`**  | `AccessType`               | Defines the initial access state: `PRIVATE`, `PUBLIC`, or `SHARED`. Defaults to `PRIVATE`.    |
| **`users`**       | `ContentAccessPermissions` | Stores explicit roles (`MAINTAINER`, `EDITOR`, `VIEWER`) granted to individual **User IDs**.  |
| **`groups`**      | `ContentAccessPermissions` | Stores explicit roles (`MAINTAINER`, `EDITOR`, `VIEWER`) granted to **Group IDs**.            |
| **`invitations`** | `MutableSet<ObjectId>`     | A list of pending invitation document IDs linked to this content object.                      |

:::info Invitations
You can learn more about **invitations** [here](./invitations.md).
:::

### `ContentAccessPermissions`

This object is utilized by both the `users` and `groups` fields to manage explicit, non-owner permissions.

The roles are implemented as separate sets of Subject IDs (User IDs or Group IDs), which ensures that roles are **additive** (a maintainer is also an editor and viewer) and **mutually exclusive** (a subject can only be in one set at a time):

```kotlin
data class ContentAccessPermissions(
    val maintainer: MutableSet<String>,
    val editor: MutableSet<String>,
    val viewer: MutableSet<String>
)
```

:::note
When a role is assigned via the `put()` function, any existing role for that subject is explicitly removed first, confirming that each subject can only hold the **highest explicit role** they were granted.
:::


## Access States

The `Access` field determines the initial visibility of a Content object:

| State         | Visibility & Rule                                                | Description                                                                                    |
|:--------------|:-----------------------------------------------------------------|:-----------------------------------------------------------------------------------------------|
| **`PUBLIC`**  | Visible to **all** users and potentially anonymous users.        | No explicit permission check is required for viewing.                                          |
| **`PRIVATE`** | Visible only to the **Owner**.                                   | The default state. All other access attempts are denied.                                       |
| **`SHARED`**  | Visible to the **Owner** and explicitly invited Users or Groups. | Access is determined by the **Object-Specific Roles** assigned to the invited user and groups. |


## Object-Specific Roles (Shared State)

When a Content object is in the **Shared** state, the Owner and Maintainers can grant specific roles to individual Users or Groups. These roles define the level of interaction with the object.

| Role             | Permissions                                                                             | Management Rights                       |
|:-----------------|:----------------------------------------------------------------------------------------|:----------------------------------------|
| **`VIEWER`**     | Read Content                                                                            | None                                    |
| **`EDITOR`**     | Read Content, **Modify/Edit** Content                                                   | None                                    |
| **`MAINTAINER`** | Read, Modify/Edit Content, **Invite/Remove Users/Groups, Manage Access, Delete Object** | Full management rights over the object. |

### Owner's Privileges

The **Owner** is the highest authority over a Content object:

* The Owner **always** possesses all rights of a **Maintainer**.
* **Owner Change:** When the ownership is transferred to a new user, 
  the old owner is automatically demoted to a **Maintainer** role to ensure continuity of management.

## Authorization Logic

The primary access function (`hasAccess(authentication, role)`) determines 
if an authenticated user meets the required role for a given action. 
The final permission is granted if **any** of the following conditions are met:

| Required Role    | Access Granted if...                                                                                                                                                                          |
|:-----------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`MAINTAINER`** | 1. The user has the [**`ADMIN`**](../auth/roles#admins) global role. OR 2. The user/group is explicitly a **`MAINTAINER`**.                                                                   |
| **`EDITOR`**     | 1. The user has the [**`ADMIN`**](../auth/roles#admins) global role. OR 2. The user/group is explicitly a **`MAINTAINER`**. OR 3. The user/group is explicitly an **`EDITOR`**.               |
| **`VIEWER`**     | 1. The user has the [**`ADMIN`**](../auth/roles#admins) global role. OR 2. The content is **`PUBLIC`**. OR 3. The user/group is explicitly a **`MAINTAINER`**, **`EDITOR`**, or **`VIEWER`**. |

### Group Access Checking

When checking a user's access, the system iterates over all groups the user belongs to
and grants the highest role found across all explicit user roles and group roles.

## Global Server Group: Contributor

A dedicated server-wide group, renamed to **Contributor**, manages the creation of new objects and tags.

| Group Name        | Core Permissions                                    | Scope                                       |
|:------------------|:----------------------------------------------------|:--------------------------------------------|
| **`contributor`** | **Create** new Content Objects, **Create** new Tags | Server-wide permission to initiate content. |

:::info
While members of the **Contributor** group can create new Content, they only gain access to resources 
they **personally created** or to which they have been **explicitly granted access** via the **Shared** state. 
This separation ensures that creation rights do not automatically grant read/write access to all existing content.
:::
