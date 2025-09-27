---
sidebar_position: 6
---

# Metadata

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

When storing a file, a metadata document will be saved in the database.
This document implements the `ContentDocument<T>` interface.
Therefore, it contains an **owner**, **access details**, **tags** and a **trusted state** by default.

:::info Content Management
You can learn more about content management [here](../content/introduction.md).
:::

## Usage

### Accessing File Metadata

:::info
These endpoints return only the metadata that is accessible by the requester.
You can learn more [here](../content/introduction.md#authorization-logic).
:::

* You can request a file metadata by its `key` through the endpoint
  [`GET /api/content/files/{key}`](../../api/get-file-metadata-by-key.api.mdx).
* You can request and filter file metadata through the endpoint
  [`GET /api/content/files`](../../api/get-file-metadata.api.mdx).
* The extended access details for a file with `key` are accessible through the endpoint
  [`GET /api/content/files/access`](../../api/get-content-object-access-details.api.mdx).

### Updating File Metadata

#### Owner

The **owner** of a file with given `key` can be updated through the endpoint:

* **Endpoint:** [`PUT /api/content/files/{key}/owner`](../../api/update-content-object-owner.api.mdx)
* **Requirements:** Only the current owner is permitted to perform this action.

#### Access

The **access** can be updated through the endpoint:

* **Endpoint:** [`PUT /api/content/files/{key}/access`](../../api/update-content-object-access.api.mdx)
* **Requirements:** Only [`MAINTAINER`](../content/introduction.md#object-specific-roles-shared-state) of the requested file
  can perform this action.

#### Trusted State

The **trusted state** can be updated through the endpoint
* **Endpoint:**  [`PUT /api/content/files/{key}/trusted`](../../api/update-content-object-trusted-state.api.mdx)
* **Requirements:** Only [`ADMIN`](../auth/roles.md#admins)s can perform this action.

### Deleting File Metadata

It is not possible to delete file metadata directly. 
Deleting files is the responsibility of the `FileStorage`.
It will automatically delete all associated metadata.
