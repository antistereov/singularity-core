---
sidebar_position: 2
---

# Usage

The file storage system is designed to be pluggable,
allowing you to choose the storage backend that best fits your needs. 

## Example

:::warning File Size
You can only upload files smaller than the [configured maximum size](introduction.md#configuration).
:::

### Prerequisites

To configure your file storage backend, you must specify the `type` in your `application.yml` file.
The available options are [`S3`](s3.md) and [`LOCAL`](local.md).

The local file storage is already preconfigured and can be used out of the box.
Check out the [configuration options](local.md#configuration) for local file storage.

```yaml
singularity:
  file:
    storage:
      type: LOCAL # Or S3
      max-file-size: 5242880 # maximum file size in Bytes
```

### Uploading a Single File

You can inject the **`FileStorage`** interface into any service or component.
Based on your configuration, *Singularity* uses the right implementation.
You can then use its methods to manage your files.

```kotlin
@Service
class MyFileService(
    private val fileStorage: FileStorage // Inject the FileStorage interface
) {
    suspend fun uploadFile(
        // The authentication outcome of an authenticated principal
        authentication: AuthenticationOutcome.Authenticated,
        file: StreamedFile,
        key: String,
        isPublic: Boolean
    ): Result<FileMetadataDocument, FileException> {
        // Upload the file and get its metadata
        return fileStorage.upload(
            key = key,
            file = file,
            authentication = authentication,
            isPublic = true
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

### Uploading Multiple Renditions

:::info Images
If you want to upload images in multiple resolutions, 
*Singularity* already provides the [image store](images.md)
:::

Let's say you want to allow the user to upload their favorite songs.
You want to store a compressed and an uncompressed version of this song.

:::note
You can learn more about `FileKey`s [here](introduction.md#file-key).
:::


```kotlin

// create a map of rendition identifier and FileUploadRequest
val filesToUpload = mutableMapOf<String, FileUploadRequest>()

// Add the upload request for the original file
// You can create FileUploadRequest from ByteArrays
filesToUpload["original"] = FileUploadRequest.ByteArrayUpload(
    key = FileKey(
        // The prefix
        prefix = "users/123456", 
        // The filename without extension
        filename = "favorite-song", 
        // The extension
        extension = "wav"
    ), // This will result in user/123456/favorite-song-3bb2c18c-xxx.mp3
    contentType = mediaType,
    data = resizedBytes,
    width = resized.width,
    height = resized.height,
    contentLength = resizedBytes.size.toLong()
)

// Add the upload request from the compressed file 
// You can create FileUploadRequest from ByteArrays
filesToUpload["original"] = FileUploadRequest.ByteArrayUpload(
    key = FileKey(
        // The prefix
        prefix = "users/123456",
        // The filename without extension
        filename = "favorite-song",
        // The suffix identifying the file as the compressed version
        suffix = "compressed",
        // The extension
        extension = "mp3"
    ), // This will result in user/123456/favorite-song_compressed-3bb2c18c-xxx.mp3
    contentType = mediaType,
    data = resizedBytes,
    width = resized.width,
    height = resized.height,
    contentLength = resizedBytes.size.toLong()
)

// Upload the renditions
fileStorage.uploadMultipleRenditions(
    // Use a metadata key to identify these files
    key = "users/123456/favorite-song",
    // Add the upload requests
    files = filesToUpload,
    // The authentication outcome of an authenticated principal
    authentication = authentication,
    // Specify if the file should be publicly visible
    isPublic = true
)
```