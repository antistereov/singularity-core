---
sidebar_position: 3
description: Learn about roles in Singularity.
---

# Roles

**Roles** in *Singularity* allow you to control access and management of your server.

:::info
Roles are primarily used to control and limit resources related to server management. If you want to have a more fine-grained control on which user can access what resource, check out [groups](https://www.google.com/search?q=./groups.md).
:::

### Users

A "normal" user of your application. Can access most of the resources but is not allowed to manage your server.

Every registered account has the `USER` role.
This is the standard role in *Singularity*.
`USER`s can access resources that are not dealing with server administration or security.

:::info
You can learn more about users [here](../principals/introduction.md#users).
:::

### Guests

*Singularity* allows you to create `GUEST` accounts.
These accounts live only in one browser session and will be invalid as soon as the browser cache is emptied.

`GUEST`s don't store any authentication details. Neither `email` nor `password`.
Therefore, a `GUEST` cannot be reauthenticated.

:::info
You can learn more about guests [here](../principals/introduction.md#guests)

## Admins

An administrator. Has access to all resources and is allowed to manage your server. Only `USER`s can be administrators.