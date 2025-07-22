package io.stereov.singularity.file.s3.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.content.file.model.FileMetadataDocument
import io.stereov.singularity.content.file.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.core.util.DataBufferPublisher
import io.stereov.singularity.file.s3.properties.S3Properties
import io.stereov.singularity.global.properties.AppProperties
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration

@Service
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "s3", matchIfMissing = false)
class S3FileStorage(
    private val s3Properties: S3Properties,
    private val s3Client: S3AsyncClient,
    private val s3Presigner: S3Presigner,
    override val appProperties: AppProperties,
    override val metadataService: FileMetadataService,
) : FileStorage() {

    override val logger = KotlinLogging.logger {}

    /**
     * Upload a file to the S3 storage.
     *
     * @param userId The ID of the user who performs the upload.
     * @param filePart The file that should be uploaded.
     * @param key The key to the file will get in the storage.
     * @param public Whether the file should be publicly accessible.
     *
     * @return [FileMetadataDocument] The metadata of the resulting upload.
     */
    override suspend fun doUpload(userId: ObjectId, filePart: FilePart, key: String, public: Boolean, contentType: MediaType): FileMetadataDocument {
        logger.debug { "Uploading file: \"${filePart.filename()}\" as $contentType" }

        val publisher = DataBufferPublisher(filePart.content()).toFlux()

        val size = filePart.content()
            .map { it.readableByteCount() }
            .reduce(0L) { acc, bytes -> acc + bytes }
            .awaitSingle()

        val putRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .contentLength(size)
            .contentType(contentType.toString())
            .acl(if (public) ObjectCannedACL.PUBLIC_READ else ObjectCannedACL.PRIVATE)
            .build()

        val requestBody = AsyncRequestBody.fromPublisher(publisher)

        s3Client.putObject(putRequest, requestBody).await()

        return FileMetadataDocument(
            ownerId = userId,
            key = key,
            contentType = contentType,
            accessType = if (public) AccessType.PUBLIC else AccessType.SHARED,
            publicUrl = if (public) getPublicUrl(key) else null,
            size = size
        )
    }

    override suspend fun doExists(key: String): Boolean {
        logger.debug { "Checking existence of file: $key" }

        return try {
            s3Client.headObject { it.bucket(s3Properties.bucket).key(key) }.await()
            true
        } catch (_: NoSuchKeyException) {
            false
        }
    }

    override suspend fun doRemove(key: String) {
        logger.debug { "Removing file: $key" }

        s3Client.deleteObject {
            it.bucket(s3Properties.bucket).key(key)
        }.await()
    }

    override suspend fun getPublicUrl(key: String): String {
        if (s3Properties.pathStyleAccessEnabled) {
            return  "${s3Properties.scheme}${s3Properties.domain}/${s3Properties.bucket}/$key"
        }
        return  "${s3Properties.scheme}${s3Properties.bucket}.${s3Properties.domain}/$key"
    }

    override suspend fun getPrivateUrl(key: String): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(s3Properties.signatureDuration))
            .getObjectRequest(getObjectRequest)
            .build()

        val presigned = s3Presigner.presignGetObject(presignRequest)
        return presigned.url().toString()
    }
}
