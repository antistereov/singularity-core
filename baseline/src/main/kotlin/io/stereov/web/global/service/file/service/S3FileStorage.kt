package io.stereov.web.global.service.file.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.model.AccessType
import io.stereov.web.global.service.file.model.FileMetaData
import io.stereov.web.properties.S3Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
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
import java.nio.file.Files
import java.time.Duration
import java.util.*

@Service
class S3FileStorage(
    private val s3Properties: S3Properties,
    private val s3Client: S3AsyncClient,
    private val s3Presigner: S3Presigner,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Upload a file to the S3 storage.
     *
     * @param userId The ID of the user who performs the upload.
     * @param filePart The file that should be uploaded.
     * @param key The key the file will get in the storage.
     * @param public Whether the file should be publicly accessible.
     *
     * @return [FileMetaData] The metadata of the resulting upload.
     */
    suspend fun upload(userId: String, filePart: FilePart, key: String, public: Boolean): FileMetaData {
        logger.debug { "Uploading file: ${filePart.filename()} as ${filePart.headers().contentType} to key $key" }

        val tempFile = withContext(Dispatchers.IO) {
            Files.createTempFile("upload-", ".tmp")
        }.toFile()
        val contentType = filePart.headers().contentType?.toString() ?: MediaType.APPLICATION_OCTET_STREAM.toString()

        val actualKey = "$key-${UUID.randomUUID()}"

        filePart.transferTo(tempFile).awaitSingleOrNull()

        val putRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(actualKey)
            .contentType(contentType)
            .acl(if (public) ObjectCannedACL.PUBLIC_READ else ObjectCannedACL.PRIVATE)
            .build()

        val requestBody = AsyncRequestBody.fromFile(tempFile)

        s3Client.putObject(putRequest, requestBody).await()

        return FileMetaData(
            key = actualKey,
            owner = userId,
            contentType = contentType,
            accessType = if (public) AccessType.PUBLIC else AccessType.SHARED,
            sharedWith = emptyList(),
            publicUrl = if (public) getPublicUrl(actualKey) else null,
            size = filePart.headers().contentLength
        )
    }

    suspend fun fileExists(key: String): Boolean {
        logger.debug { "Checking existence of file: $key" }

        return try {
            s3Client.headObject { it.bucket(s3Properties.bucket).key(key) }.await()
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }

    suspend fun removeFile(key: String) {
        logger.debug { "Removing file: $key" }

        s3Client.deleteObject {
            it.bucket(s3Properties.bucket).key(key)
        }.await()
    }

    suspend fun removeFileIfExists(key: String): Boolean {
        logger.debug { "Removing file $key if it exists" }

        return try {
            removeFile(key)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFileUrl(metaData: FileMetaData): String {
        logger.debug { "Getting URL for file ${metaData.key}" }

        return when (metaData.accessType) {
            AccessType.PUBLIC -> getPublicUrl(metaData.key)
            AccessType.PRIVATE, AccessType.SHARED -> getPresignedUrl(metaData.key)
        }
    }

    private suspend fun getPublicUrl(key: String): String {
        return  "https://${s3Properties.bucket}.${s3Properties.uri}/$key"
    }

    private suspend fun getPresignedUrl(key: String): String {
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
