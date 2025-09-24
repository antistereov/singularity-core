---
sidebar_position: 1
---

# Basics

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity* provides a robust and pluggable file storage system for managing file uploads and their metadata. 
It's designed to handle different storage backends and ensure data consistency between the database and the actual stored files.

## Core Concepts

#### `FileMetadataDocument` 

This is the core data model for file metadata. 
It stores a unique ID, a developer-friendly key, a unique ID, a content type, file size, and access details. 
The `FileMetadataDocument` is stored in a MongoDB collection.

#### `FileStorage`

This is an abstract class that defines the core logic for managing files, 
including `upload`, `exists`, and `remove` operations. 
It provides a consistent interface for different storage implementations (e.g., S3, local). 
It also includes a consistency check to ensure the file's metadata in the database matches the actual file in storage.

#### `FileMetadataService`

This service manages the `FileMetadataDocument` in the MongoDB database. 
It handles operations like saving, finding, and deleting file metadata.

## Usage

The file storage system is designed to be pluggable,
allowing you to choose the storage backend that best fits your needs. 

### Key Operations

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

### Step 1: Configure File Storage

To configure your file storage backend, you must specify the `type` in your `application.yml` file.
The available options are [`S3`](s3.md) and [`LOCAL`](local.md).

The local file storage is already preconfigured and can be used out of the box.
Check out the [configuration options](local.md#configuration) for local file storage.

```yaml
singularity:
  file:
    storage:
      type: LOCAL # Or S3
```

### Step 2: Use the File Storage Service

You can inject the `FileStorage` interface into any service or component. 
Based on your configuration, *Singularity* uses the right implementation.
You can then use its methods to manage your files.

```kotlin
@Service
class MyFileService(
    private val fileStorage: fileStorage // Inject the FileStorage interface
) {
    suspend fun uploadFile(userId: ObjectId, file: FilePart, key: String) {
        // Upload the file and get its metadata
        val metadata: FileMetadataDocument = fileStorage.upload(
            userId = userId,
            filePart = file,
            key = key,
            public = true
        )
    }

    suspend fun checkFile(key: String): Boolean {
        // Check if a file exists
        return fileStorage.exists(key)
    }

    suspend fun getFileMetadata(key: String): FileMetadataResponse? {
        // Get file metadata and URL
        return fileStorage.metadataResponseByKeyOrNull(key)
    }
}
```