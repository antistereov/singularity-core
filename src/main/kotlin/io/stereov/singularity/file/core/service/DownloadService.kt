package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.core.exception.model.DownloadException
import io.stereov.singularity.file.core.model.DownloadedFile
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class DownloadService(
    private val webClient: WebClient
) {

    private val logger = KotlinLogging.logger {}

    suspend fun download(url: String): DownloadedFile {
        logger.debug { "Downloading file from URL $url" }

        val response = webClient.get()
            .uri(url)
            .retrieve()
            .toEntity(ByteArray::class.java)
            .onErrorResume { ex -> throw DownloadException("Failed to download file from URL $url", ex) }
            .awaitFirst()

        val headers = response.headers
        val contentType = headers.contentType
            ?: MediaType.APPLICATION_OCTET_STREAM

        val rawBytes = response.body ?: throw DownloadException("File body was empty")

        return DownloadedFile(bytes = rawBytes, contentType = contentType, url = url)
    }

}