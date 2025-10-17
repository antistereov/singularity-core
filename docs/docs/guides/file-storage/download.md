---
sidebar_position: 3
---


# Download

The `DownloadService` component in *Singularity* provides a robust, 
non-blocking mechanism for fetching external files into the 
application memory. 

It leverages **Spring WebClient** and **Kotlin Coroutines** to handle 
downloads reactively and efficiently.

:::note Reactive Download
This service performs a full, in-memory download. The entire file content is loaded as a `ByteArray` before the function returns. 
For very large files, consider using reactive streams directly (e.g., `Flux<DataBuffer>`) in other parts of the application to avoid potential out-of-memory issues.
:::

## Core Component

### `DownloadService`

This is a **Spring `@Service`** that encapsulates the logic for an HTTP GET request to download a file. It is injected with a pre-configured `WebClient` instance.

| Function   | Signature                                           | Description                                                                                                                   |
|------------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `download` | `suspend fun download(url: String): DownloadedFile` | Performs an asynchronous, non-blocking download of a file from the specified URL. It throws a `DownloadException` on failure. |

#### Error Handling

The service provides unified error handling:

* Any network or HTTP error that occurs during the download is caught.
* The exception is wrapped and re-thrown as a **`DownloadException`**, providing context about the failed URL.
* If the remote server returns a successful response but with an empty body (`null` bytes), a `DownloadException` is thrown indicating that the file body was empty.

## Data Model

### `DownloadedFile`

The `DownloadedFile` is the value object returned by the `DownloadService`. It contains the file's raw content and essential metadata derived from the HTTP response headers.

| Property      | Type        | Description                                                                                                                                                                                      |
|:--------------|:------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `bytes`       | `ByteArray` | The raw content of the file loaded into memory.                                                                                                                                                  |
| `contentType` | `MediaType` | The file's media type (e.g., `application/pdf`, `image/jpeg`), usually extracted from the `Content-Type` HTTP header. Defaults to `MediaType.APPLICATION_OCTET_STREAM` if the header is missing. |
| `url`         | `String`    | The original URL from which the file was downloaded.                                                                                                                                             |
| `size`        | `Int`       | A derived property (getter) that returns the size of the file in bytes.                                                                                                                          |

#### Value Equality

The `DownloadedFile` data class implements custom `equals` and `hashCode` methods. This is crucial because it contains a `ByteArray` (which uses reference equality by default).

* **Equality is determined by:** The content of the `bytes` array (`contentEquals`) and the `contentType`.
* **Excluded from equality:** The `url` is **not** included in the `equals` or `hashCode` calculation, meaning two `DownloadedFile` instances with the same content and media type are considered equal, regardless of the source URL.

## Usage Example

The `download` function must be called from within a **coroutine scope** or another `suspend` function.

```kotlin
// Example of injecting and using the service
@Component
class FileProcessor(
    private val downloadService: DownloadService
) {
    suspend fun processExternalFile(link: String) {
        try {
            val file: DownloadedFile = downloadService.download(link)

            // Log file details
            logger.info { "Successfully downloaded file from ${file.url}" }
            logger.info { "Content Type: ${file.contentType}" }
            logger.info { "Size: ${file.size} bytes" }

            // Access the raw bytes for further processing (e.g., saving to storage)
            val rawData: ByteArray = file.bytes

        } catch (ex: DownloadException) {
            logger.error(ex) { "A file download failed." }
            // Handle the download failure (e.g., notify user)
        }
    }
}
```