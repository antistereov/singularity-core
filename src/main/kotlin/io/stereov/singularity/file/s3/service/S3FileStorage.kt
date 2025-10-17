package io.stereov.singularity.file.s3.service

import com.nimbusds.jose.util.StandardCharset
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.core.component.DataBufferPublisher
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.model.FileUploadResponse
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.s3.properties.S3Properties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.net.URLDecoder
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

@Service
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "s3", matchIfMissing = false)
class S3FileStorage(
    private val s3Properties: S3Properties,
    private val s3Client: S3AsyncClient,
    private val s3Presigner: S3Presigner,
    override val appProperties: AppProperties,
    override val metadataService: FileMetadataService,
    override val metadataMapper: FileMetadataMapper,
    private val dataBufferPublisher: DataBufferPublisher,
    override val storageProperties: StorageProperties
) : FileStorage() {

    override val logger = KotlinLogging.logger {}

    override suspend fun uploadRendition(req: FileUploadRequest): FileUploadResponse {
        logger.debug { "Uploading file: \"${req.key}\" as $${req.contentType}" }
        return when (req) {
            is FileUploadRequest.FilePartUpload -> doUploadFilePart(req)
            is FileUploadRequest.ByteArrayUpload -> doUploadByteArray(req)
        }
    }

    private suspend fun doUploadFilePart(req: FileUploadRequest.FilePartUpload): FileUploadResponse {
        val contentLength = AtomicLong()

        val uploadMono: Mono<PutObjectResponse> = DataBufferUtils.join(req.data.content())
            .flatMap { mergedBuffer ->

                val finalLength = mergedBuffer.readableByteCount().toLong()

                val contentFlux = mergedBuffer.readableByteBuffers().asSequence().toList().toTypedArray()
                val requestBody = AsyncRequestBody.fromByteBuffers(*contentFlux)

                val putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(req.key.key)
                    .contentLength(finalLength)
                    .contentType(req.contentType)
                    .acl(ObjectCannedACL.PRIVATE)
                    .build()

                Mono.using(
                    { mergedBuffer },
                    { _ -> Mono.fromFuture(s3Client.putObject(putRequest, requestBody)) },
                    { buffer -> DataBufferUtils.release(buffer) }
                )
            }

        uploadMono.awaitSingle()

        return FileUploadResponse(
            key = req.key.key,
            contentType = req.contentType,
            size = contentLength.get(),
            width = req.width,
            height = req.height
        )
    }

    private suspend fun doUploadByteArray(req: FileUploadRequest.ByteArrayUpload): FileUploadResponse {
        val size = req.data.size.toLong()

        val putRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(req.key.key)
            .contentLength(size)
            .contentType(req.contentType)
            .acl(ObjectCannedACL.PRIVATE)
            .build()

        val requestBody = AsyncRequestBody.fromBytes(req.data)
        s3Client.putObject(putRequest, requestBody).await()

        return FileUploadResponse(
            key = req.key.key,
            contentType = req.contentType,
            size = size,
            width = req.width,
            height = req.height
        )
    }

    override suspend fun renditionExists(key: String): Boolean {
        logger.debug { "Checking existence of file: $key" }

        return try {
            s3Client.headObject { it.bucket(s3Properties.bucket).key(key) }.await()
            true
        } catch (_: NoSuchKeyException) {
            false
        }
    }

    override suspend fun removeRendition(key: String) {
        logger.debug { "Removing file: $key" }

        s3Client.deleteObject {
            it.bucket(s3Properties.bucket).key(key)
        }.await()
    }

    override suspend fun getRenditionUrl(key: String): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(s3Properties.signatureDuration))
            .getObjectRequest(getObjectRequest)
            .build()

        val presigned = s3Presigner.presignGetObject(presignRequest)
        return URLDecoder.decode(presigned.url().toString(), StandardCharset.UTF_8)
    }
}
