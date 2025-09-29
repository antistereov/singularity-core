---
sidebar_position: 1
---

# Introduction

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity* provides a robust and pluggable file storage system for managing file uploads and their metadata. 
It's designed to handle different storage backends and ensure data consistency between the database and the actual stored files.

## Core Concepts

#### `FileStorage`

This is an abstract class that defines the core logic for managing files, 
including `upload`, `exists`, and `remove` operations. 
It provides a consistent interface for different storage implementations (e.g., S3, local). 
It also includes a consistency check to ensure the file's metadata in the database matches the actual file in storage.

#### `FileMetadataDocument`

This is the core data model for file metadata.
It stores a unique ID, a developer-friendly key, a unique ID, a content type, file size, and access details.
The `FileMetadataDocument` is stored in a MongoDB collection.

## Key Operations

#### `upload`
This method handles the file upload process. 
It takes the user's ID, the file part, a unique key, and a public flag. 
It generates a unique file name, uploads the file to the configured storage backend,
and saves its metadata to the database.

#### `exists` 
This method checks for a file's existence by verifying both the database record and the file in storage. 
It includes a consistency check: if a file exists in one place but not the other,
it will be removed from both to maintain data integrity.

#### `remove` 
This method deletes a file and its associated metadata. 
It removes the file from the storage backend and then deletes the `FileMetadataDocument` from the database.

#### `metadataResponseByKey`

This method retrieves a `FileMetadataDocument` by its key and converts it into a `FileMetadataResponse`, 
which includes a URL to access the file.

### File Key

**`FileKey`s** help you to create a unique key to identify your files.

A `FileKey` can be created based on 
* a`prefix` (e.g. `user/123456`),
* a `filename` (e.g. `avatar`),
* a `suffix` (e.g. `small`) and 
* an `extension` (e.g. `jpeg`).

A `UUID` will be attached to make the key unique to prevent **cache busting**.

```kotlin
val fileKey = FileKey(
    prefix = "user/123456",
    filename = "avatar",
    suffix = "small",
    extension = "jpeg"
)
```

This will result in `/user/123456/avatar_small-3bb2c18c-xxx.jpeg`.

### Renditions

It is possible to store multiple **renditions** of a file, e.g. a compressed version and the original.
The [image store](./images.md) automatically creates a small, medium and large rendition of the image.
If only one rendition is stored, it will be stored as `original`.

:::warning
It is important to distinguish **rendition keys** with the **key of file metadata**.

* The `key` of the **file metadata** is a **content key** which is used to control access for example.
  _(You can learn more [here](../content/introduction.md))_.
* The `key` of a **rendition** is a **file storage key**. The file will be identified by this key.

**One rendition:** If the file is uploaded through `upload`, both keys will be the same.

**Multiple renditions:** If multiple renditions are uploaded through `uploadMultipleRenditions`,
it is only possible to delete the whole collection through the key that you specified in the upload.
You can only access the renditions through the file metadata.
:::

## Configuration

| Property                               | Type            | Description                                                                                                                                                                     | Default value |
|----------------------------------------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.file.storage.type          | `LOCAL` or `S3` | The implementation of the file storage. The available options are [`S3`](s3.md) and [`LOCAL`](local.md). Check out the respective documentation for more configuration options. | `true`        |
| singularity.file.storage.max-file-size | `Long`          | The maximum file size. Default is 5MB.                                                                                                                                          | `5242880`     |
