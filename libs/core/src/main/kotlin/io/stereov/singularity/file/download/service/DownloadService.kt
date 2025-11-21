package io.stereov.singularity.file.download.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.download.exception.DownloadException
import io.stereov.singularity.file.download.model.StreamedFile
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono

/**
 * Service responsible for downloading files from remote URLs.
 *
 * The `DownloadService` handles downloading content by streaming it as a flow of data buffers,
 * supporting efficient memory usage for large files. It utilizes a provided `WebClient` to
 * perform HTTP requests and process server responses to stream the file.
 *
 * This service is designed to handle potential errors during file downloads gracefully, wrapping
 * successes or failures into a `Result` for better error handling and control in calling components.
 *
 * @constructor Creates an instance of `DownloadService`.
 * @param webClient The `WebClient` instance used to make HTTP requests for downloading files.
 */
@Service
class DownloadService(
    private val webClient: WebClient
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Downloads a file from the specified URL and streams its contents as a flow of data buffers.
     *
     * @param url The URL from which the file will be downloaded.
     * @return A [Result] containing the [StreamedFile] on success, or a [DownloadException] on failure.
     */
    suspend fun download(url: String): Result<StreamedFile, DownloadException> {
        logger.debug { "Downloading file from URL $url" }

        return runSuspendCatching {
            webClient.get()
                .uri(url)
                .exchangeToMono { clientResponse ->
                    val headers = clientResponse.headers().asHttpHeaders()
                    val contentType = headers.contentType ?: MediaType.APPLICATION_OCTET_STREAM

                    val bodyFlux = clientResponse.bodyToFlux<DataBuffer>()
                    val contentFlow = bodyFlux
                        .map { DataBufferUtils.retain(it) }
                        .asFlow()

                    Mono.just(
                        StreamedFile(
                            url = url,
                            contentType = contentType,
                            content = contentFlow
                        )
                    )
                }
                .awaitSingle()
        }.mapError { ex ->
            DownloadException("Failed to stream file form URL $url: ${ex.message}", ex)
        }
    }
}
