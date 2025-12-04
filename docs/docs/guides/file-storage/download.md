---
sidebar_position: 3
---


# Download

The `DownloadService` component in *Singularity* provides a robust,
non-blocking mechanism for fetching external files using **reactive streams**.

It leverages **Spring WebClient** and **Kotlin Coroutines** to handle
downloads reactively and efficiently.

:::info Reactive Streaming
This service performs a file download by **streaming** the content as a `Flux<DataBuffer>`.
The entire file content is **not** loaded into a single `ByteArray` in memory,
making this approach efficient for handling large files. The caller is responsible for
consuming the reactive stream.
:::

## Core Components

### `DownloadService`

This is a **Spring `@Service`** that encapsulates the logic for an HTTP GET request to download a file. It is injected with a pre-configured `WebClient` instance.

| Function   | Signature                                                                    | Description                                                                                                                                                                                     |
|------------|------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `download` | `suspend fun download(url: String): Result<StreamedFile, DownloadException>` | Performs an asynchronous, non-blocking download, returning the file content as a reactive stream. Returns a **`Result`** containing **`StreamedFile`** or a **`DownloadException`** on failure. |

#### Error Handling

The service provides unified error handling:

* An exception of type **`DownloadException`** is returned via the `Result`'s `Err` channel. This exception is a sealed class with specific subtypes (e.g., `FileNotFound`, `NetworkError`, `Timeout`, `FileTooLarge`) that provide context about the failure.

### `StreamedFile`

The `StreamedFile` data class encapsulates the downloaded file's metadata and its content stream.

| Field         | Type               | Description                                                        |
|---------------|--------------------|--------------------------------------------------------------------|
| `content`     | `Flux<DataBuffer>` | The reactive stream of data buffers containing the file's content. |
| `contentType` | `MediaType`        | The file's media type (e.g., `application/pdf`).                   |
| `url`         | `String`           | The source URL of the file.                                        |

## Example

The `download` function returns a `Result`, so we use **`.bind()`** or **`.getOrThrow()`** (in a Controller/top-level function) to unwrap the `StreamedFile` and access the reactive content stream.

```kotlin
// Example of injecting and using the service
@Component
class FileProcessor(
    private val downloadService: DownloadService
) {
    /**
     * Downloads an external file and initiates consumption of the content stream.
     * Note: Reactive streams must be actively consumed (e.g., saved to disk) to prevent resource leaks.
     */
    suspend fun processExternalFile(link: String): Result<Unit, DownloadException> = coroutineBinding {
        // download() returns Result<StreamedFile, DownloadException>.
        // bind() unwraps the StreamedFile or propagates the DownloadException.
        val streamedFile: StreamedFile = downloadService.download(link).bind()

        // Log file details
        logger.info { "Successfully initiated download stream from ${streamedFile.url}" }
        logger.info { "Content Type: ${streamedFile.contentType}" }

        // Access the reactive content stream (Flux<DataBuffer>)
        val fileContentStream: Flux<DataBuffer> = streamedFile.content

        // IMPORTANT: The file content must be actively consumed here,
        // e.g., by piping the Flux<DataBuffer> to a file sink or processing it reactively.
        // Failing to consume the Flux can lead to resource leaks (e.g., open connections).
        // Example: fileContentStream.doOnNext { dataBuffer -> /* process data */ }.awaitLast()
    }
}
```