package io.stereov.singularity.file.s3.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.recoverIf
import com.github.michaelbull.result.runCatching
import com.nimbusds.jose.util.StandardCharset
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.core.exception.FileException
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
import kotlin.io.path.Path

@Service
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "s3", matchIfMissing = false)
class S3FileStorage(
    private val s3Properties: S3Properties,
    private val s3Client: S3AsyncClient,
    private val s3Presigner: S3Presigner,
    override val appProperties: AppProperties,
    override val metadataService: FileMetadataService,
    override val metadataMapper: FileMetadataMapper,
    override val storageProperties: StorageProperties
) : FileStorage() {

    override val logger = KotlinLogging.logger {}

    override suspend fun uploadRendition(req: FileUploadRequest): Result<FileUploadResponse, FileException.Operation> {
        logger.debug { "Uploading file: \"${req.key}\" as $${req.contentType}" }
        return when (req) {
            is FileUploadRequest.FilePartUpload -> doUploadFilePart(req)
            is FileUploadRequest.ByteArrayUpload -> doUploadByteArray(req)
            is FileUploadRequest.DataBufferUpload -> doUploadDataBuffer(req)
        }
    }

    private suspend fun doUploadDataBuffer(
        req: FileUploadRequest.DataBufferUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        val contentLength = AtomicLong()

        val uploadMono: Mono<PutObjectResponse> = DataBufferUtils.join(req.data)
            .flatMap { mergedBuffer ->

                val finalLength = mergedBuffer.readableByteCount().toLong()

                val contentFlux = mergedBuffer.readableByteBuffers().asSequence().toList().toTypedArray()
                val requestBody = AsyncRequestBody.fromByteBuffers(*contentFlux)

                val putRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(Path(s3Properties.path).resolve(req.key.key).toString())
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

        runSuspendCatching { uploadMono.awaitSingle() }
            .mapError { ex -> FileException.Operation("Failed to save file part to S3: ${ex.message}", ex) }
            .bind()

        FileUploadResponse(
            key = req.key.key,
            contentType = req.contentType,
            size = contentLength.get(),
            width = req.width,
            height = req.height
        )
    }


    private suspend fun doUploadFilePart(
        req: FileUploadRequest.FilePartUpload
    ): Result<FileUploadResponse, FileException.Operation> {
        val dataBufferRequest = FileUploadRequest.DataBufferUpload(
            key = req.key,
            contentType = req.contentType,
            data = req.data.content(),
            width = req.width,
            height = req.height
        )
        return doUploadDataBuffer(dataBufferRequest)
    }

    private suspend fun doUploadByteArray(
        req: FileUploadRequest.ByteArrayUpload
    ): Result<FileUploadResponse, FileException.Operation> = coroutineBinding {
        val size = req.data.size.toLong()

        val putRequest = runCatching {
            PutObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(Path(s3Properties.path).resolve(req.key.key).toString())
                .contentLength(size)
                .contentType(req.contentType)
                .acl(ObjectCannedACL.PRIVATE)
                .build()
        }
            .mapError { ex -> FileException.Operation("Failed to create PutObjectRequest: ${ex.message}", ex) }
            .bind()

        val requestBody = runCatching { AsyncRequestBody.fromBytes(req.data) }
            .mapError { ex -> FileException.Operation("Failed to append request body: ${ex.message}", ex) }
            .bind()

        runSuspendCatching {
            s3Client.putObject(putRequest, requestBody).await()
        }
            .mapError { ex -> FileException.Operation("Failed to save file to S3: ${ex.message}", ex) }
            .bind()

        FileUploadResponse(
            key = req.key.key,
            contentType = req.contentType,
            size = size,
            width = req.width,
            height = req.height
        )
    }

    override suspend fun renditionExists(key: String): Result<Boolean, FileException.Operation> {
        logger.debug { "Checking existence of file: $key" }

        return runSuspendCatching {
            s3Client.headObject { it.bucket(s3Properties.bucket).key(Path(s3Properties.path).resolve(key).toString()) }.await()
            true
        }.recoverIf(
            { ex -> ex is NoSuchKeyException },
            { false }
        ).mapError { ex -> FileException.Operation("Failed to check existence of file with key $key: ${ex.message}", ex) }
    }

    override suspend fun removeRendition(key: String): Result<Unit, FileException.Operation> {
        logger.debug { "Removing file: $key" }

        return runSuspendCatching {
            s3Client.deleteObject {
                it.bucket(s3Properties.bucket).key(Path(s3Properties.path).resolve(key).toString())
            }.await()
            Unit
        }.mapError { ex -> FileException.Operation("Failed to remove rendition with key $key: ${ex.message}", ex) }
    }

    override suspend fun getRenditionUrl(key: String): Result<String, FileException> = coroutineBinding {

        val fileExists = renditionExists(key).bind()
        resolveMetadataSyncConflicts(fileExists, key).bind()

        runSuspendCatching {
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(Path(s3Properties.path).resolve(key).toString())
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3Properties.signatureDuration))
                .getObjectRequest(getObjectRequest)
                .build()

            val presigned = s3Presigner.presignGetObject(presignRequest)
            URLDecoder.decode(presigned.url().toString(), StandardCharset.UTF_8)
        }
            .mapError { ex ->
                FileException.Operation(
                    "Failed to generate url for rendition with key $key: ${ex.message}",
                    ex
                )
            }
            .bind()
    }
}
