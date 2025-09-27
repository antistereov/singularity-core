---
sidebar_position: 8
description: Learn how to use groups to control access.
---

# Groups

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

**Groups** allow fine-grained control on which information each user is allowed to see.

:::note
You can learn more on how to secure endpoints by requiring a group membership [here](./securing-endpoints.md#requiring-group-membership).
:::

A **Group** is uniquely identified by a `key`.
You can save a name and a description in multiple [translations](../content/i18n.md) to the database.

:::info
Managing groups and their members can only be done by users with the `ADMIN` role.
:::

## Managing Groups

:::info Group Keys
The group's `key` cannot be changed once created.
This is necessary to maintain consistency in the database.
:::

:::info `contributor`
The `contributor` group will always be included by default.
This group is necessary for managing content. 
You can learn more [here](../content/introduction.md#global-server-group-contributor).
:::

### Creating Groups

You can create a new group with a `key`, and translations of name and descriptions through the endpoint
[`POST /api/groups`](../../api/create-group.api.mdx).

### Getting Groups

You can retrieve all configured groups through the endpoint [`GET /api/groups`](../../api/get-groups.api.mdx).
If you need one specific group, 
you can retrieve information about it using the group's `key` through the endpoint [`GET /api/groups/{key}`](../../api/get-group-by-key.api.mdx).

### Updating Groups

You can update a group through the endpoint [`PUT /api/groups/{key}`](../../api/update-group.api.mdx).

### Deleting Groups

You can delete a group through the endpoint [`DELETE /api/groups/{key}`](../../api/delete-group.api.mdx).

## Managing Members

### Adding Members to Groups

You can add a member to a group through the endpoint [`POST /api/groups/{key}/members`](../../api/add-member-to-group.api.mdx).

This will invalidate all [`AccessToken`](./tokens.md#access-token)s for the new member.
This way, before performing a new request for the new member, a new token needs to be requested from [`POST /api/auth/refresh`](../../api/refresh-access-token.api.mdx).
The new token will contain the new group membership.

### Removing Members from Groups

You can remove a member from a group through the endpoint [`DELETE /api/groups/{key}/members`](../../api/remove-member-from-group.api.mdx).

This will invalidate all [`AccessToken`](./tokens.md#access-token)s for the removed member.
This way, before performing a new request for the new member, a new token needs to be requested from [`POST /api/auth/refresh`](../../api/refresh-access-token.api.mdx).
The new token will contain the revoke the group membership.

## Configuration

You can configure groups to be available after the first application startup.

| Property               | Type                       | Description                                         | Default |
|------------------------|----------------------------|-----------------------------------------------------|---------|
| singularity.app.groups | `List<CreateGroupRequest>` | Groups that will be created on application startup. |         |

```yaml
singularity:
  app:
    groups: 
      - key: pilots # the unique key
        translations: # translations containing name and description
          - en:
              name: Pilots
              description: The pilots of our mission.
          - de:
              name: Piloten
              description: Die Piloten unserer Mission.
      - key: passenger
        translations:
          - en:
              name: Passenger
              description: The passengers of our mission.
          - de:
              name: Passagiere
              description: Die passagiere unserer Mission.
```
