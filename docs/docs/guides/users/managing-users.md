---
sidebar_position: 1
description: Learn how to manage users.
---

# Managing Users

The User Management system provides administrative tools to manage user accounts within the application. 

These capabilities are restricted to users with [`ADMIN`](../auth/roles.md#admins) permissions, 
ensuring that sensitive user data and account actions are handled securely.

## Core Functions

The system supports the following key operations for managing users:

### User Retrieval

This is accessible to everyone and can be requested through [`GET /api/users/{id}`](../../api/get-user-by-id.api.mdx).

Detailed information for a single user can be retrieved using their unique ID.
This is useful for viewing a complete profile and verifying account details.

### User Search and Discovery

This action is only accessible for users with [`ADMIN`](../auth/roles.md#admins) permissions.
It can be requested through [`GET /api/users`](../../api/get-users.api.mdx).

* Administrators can search for users using a variety of filters.
* Filtering can be performed by a user's exact email address for precise lookups.
* You can also filter users by their assigned **roles** (e.g., [`ADMIN`](../auth/roles.md#admins), 
  [`USER`](../auth/roles.md#users)) or by the **groups** they belong to.
* The system allows searching for users who are connected to specific **identity providers** 
  (e.g., `google`, `github`), making it easy to manage accounts authenticated through external services.
* Time-based filtering is available to find users based on when their accounts were **created** 
  or when they were **last active**. 
  This is useful for identifying new, inactive, or long-standing users.
* All searches are paginated, allowing for efficient browsing of large user bases without performance issues.

### User Deletion

The system provides a function to permanently delete a user account. 
This action is irreversible and should be performed with caution.

Deleting a user is only permitted for users with [`ADMIN`](../auth/roles.md#admins) privileges.
It can be requested through [`DELETE /api/users/{id}`](../../api/delete-user-by-id.api.mdx).