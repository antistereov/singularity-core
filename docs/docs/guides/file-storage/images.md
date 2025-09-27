---
sidebar_position: 3
---

# Images

*Singularity* provides an automated way to process uploaded images in multiple resolutions.
The **`ImageStore`** provides these capabilities out-of-the-box.

## Configuration

| Property                                      | Type      | Description                                                            | Default Value |
|:----------------------------------------------|:----------|:-----------------------------------------------------------------------|:--------------|
| singularity.file.storage.image.store-original | `Boolean` | Should the original image be stored alongside the compressed versions? | `true`        |
| singularity.file.storage.s3.small             | `Int`     | The width the small image should have at least in pixels.              | `400`         |
| singularity.file.storage.s3.medium            | `Int`     | The width the medium image should have at least in pixels.             | `800`         |
| singularity.file.storage.s3.large             | `Int`     | The width the large image should have at least in pixels.              | `1920`        |

#### Example `application.yaml`

```yaml
singularity:
  file:
    storage:
      image:
        store-original: true
        small: 400
        medium: 800
        large: 1920
```

## Usage

:::warning File Size
You can only upload files smaller than the [configured maximum size](introduction.md#configuration).
:::

```kotlin
@Service
class MyImageService(
    private val imageStore: ImageStore // Inject the FileStorage interface
) {
    suspend fun uploadImage(userId: ObjectId, file: FilePart, key: String) {
        // Upload the file and get its metadata
        val metadata: FileMetadataDocument = imageStore.upload(
            // the metadata key
            key = key,
            // the file part containing the original image
            file = file,
            // the owner
            ownerId = userId,
            isPublic = true
        )
    }
}
```

The resized and compressed images will be stored as [**renditions**](introduction.md#renditions) inside the 
file metadata.
