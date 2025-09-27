---
sidebar_position: 4
description: Learn more about invitations.
---

# Invitations

:::note
This guide demonstrates how to create a new content type, `CoolStuff`, by integrating it into the abstract `ContentDocument` and leveraging the core services for access management and authorization.
:::

It is possible to **invite** users to view, edit or maintain a content object.

## Configuration

| Property                                                 | Type      | Description                                                                                                                                                     | Default value                                                                |
|----------------------------------------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| singularity.content.invitations.accept-url               | `String`  | The URL for accepting invitations in your frontend. Please use the placeholders `{contentType}` and `{contentKey}` which will be autofilled by the application. | `http://localhost:4200/content/{contentType}/{contentKey}/accept-invitation` |
| singularity.content.invitations.allow-unregistered-users | `Boolean` | Allow sending invitations to unregistered users.                                                                                                                | `true`                                                                       |


## Inviting a User

An invitation to a content object of **`contentType`** (**`article`** for [articles](articles.md) for example) 
and `key` can be sent through the endpoint [`POST /api/content/{contentType}/invitations/{key}`](../../api/invite-user-to-content-object.api.mdx)

This will trigger the following actions:

* An `InvitationDocument` containing information about the content object and invited user will be stored to the database.
* A token with an [expiration](../auth/tokens.md#configuration) will be generated.
* This token will be used to create a URL based on the URL you specified in the configuration.
* An email will be sent to the invited user containing this URL.
  For example, if the URL is configured to `https://example.com/content/{contentType}/{contentKey}/accept-invation`, 
  the URL for inviting a user to an article with key `my-cool-article` will be 
  `https://example.com/content/articles/my-cool-article/accept-invation?token=ey123445...`.

The invited user will receive an email notifying them about the invitation.

## Accepting an Invitation

:::info
The frontend needs to extract the `contentType`, `contentKey` and `token` from the request.
:::

The invitation can be **accepted** by requesting the endpoint [`POST /api/content/{contentType}/invitations/{contentKey}`](../../api/accept-invitation-to-content-object.api.mdx)
by specifying `contentType`, `contentKey` and `token`.

## Deleting an Invitation

Expired invitations will be deleted automatically.
You can delete an invitation with `id` before it expires through the endpoint [`DELETE /api/content/invitations/{id}`](../../api/delete-invitation-to-content-object-by-id.api.mdx).
This can only be done by [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state)s of this related content object

:::info
The invitations are included in the access details.
These can be requested through the endpoint [`GET /api/content/{contentType}/{contentKey}/access`](../../api/get-content-object-access-details.api.mdx).
:::
