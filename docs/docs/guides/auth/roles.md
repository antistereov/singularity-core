---
description: Learn more about roles.
sidebar_position: 7
---

# Roles

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

**Roles** in *Singularity* allow you to control access and management of your server.
There are three roles:

- **`USER`:** A "normal" user of your application. 
    Can access most of the resources but is not allowed to manager your server.
- **`ADMIN`:** An administrator. Has access to all resources and is allowed to manage your server.
- **`GUEST`:** A user that has access to your application only in the current browser context and
    did not set up any authentication method.

:::info
Roles are primarily used to control and limit resources related to server management.
If you want to have a more fine-grained control on which user can access what resource, 
check out [groups](./groups.md).
:::

## Users

Every registered account has the `USER` role.
This is the standard role in *Singularity*.
`USER`s can access resources that are not dealing with server administration or security.

## Admins

### Granting Admin Permissions

You can grant admin permissions to a user through the endpoint [`POST /api/admins/{user-id}`](../../api/grant-admin-permissions.api.mdx).

This action can only be performed by users who already have `ADMIN` privileges.

### Revoking Admin Permissions

You can revoke admin permissions from a user through the endpoint [`DELETE /api/admins/{user-id}`](../../api/revoke-admin-permissions.api.mdx).

This action can only be performed by users who already have `ADMIN` privileges.

## Guests

*Singularity* allows you to create `GUEST` accounts.
These accounts live only in one browser session and will be invalid as soon as the browser cache is emptied.

`GUEST`s don't store any authentication details. Neither `email` nor `password`.
Therefore, a `GUEST` cannot be reauthenticated.

:::info
[`AccessToken`](./tokens.md#access-token)s and [`RefreshToken`](./tokens.md#refresh-token)s work the same for `GUEST`s.
Only re-authentication is not possible.
:::

### Creating Guest Accounts

You can create a new `GUEST` account through the endpoint [`POST /api/guests`](../../api/create-guest-account.api.mdx).

### Converting Guest Accounts To User Accounts

It is possible to convert a `GUEST` account to a `USER` account.
This can be done in two ways:

#### Adding Password Authentication

A `GUEST` can specify a username and password through [`POST /api/guests/convert-to-user`](../../api/convert-guest-to-user.api.mdx).
This allows the user to log in with the specified `username` and `password`.

#### Connecting OAuth2 Provider

:::note
You can learn more about OAuth2 in *Singularity* [here](./oauth2).
:::

By connecting an OAuth2 provider to a `GUEST` account, this account becomes a `USER` account.
The process of connecting an OAuth2 provider is the same as for users.
You can follow [this](./oauth2.md#connecting-an-oauth2-provider-to-an-existing-account) guide.