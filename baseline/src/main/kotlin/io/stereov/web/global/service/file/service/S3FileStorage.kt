package io.stereov.web.global.service.file.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.file.model.StoredFile
import io.stereov.web.global.service.file.model.StoredFileMetaData
import io.stereov.web.properties.S3FileStorageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.InputStreamResource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.nio.file.Files
import java.util.*

@Service
@ConditionalOnProperty(prefix = "baseline.file.storage", name = ["type"], havingValue = "s3", matchIfMissing = false)
class S3FileStorage(
    private val s3Properties: S3FileStorageProperties,
    private val s3Client: S3AsyncClient,
) : FileStorage {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override suspend fun storeFile(file: FilePart, subfolder: String, filename: String?): StoredFileMetaData {
        logger.debug { "Storing file" }

        val actualFilename = filename ?: file.filename() ?: UUID.randomUUID().toString()
        val key = "$subfolder/$actualFilename"

        val tempFile = withContext(Dispatchers.IO) {
            Files.createTempFile("upload-", ".tmp")
        }.toFile()

        file.transferTo(tempFile).awaitSingleOrNull()

        val request = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(key)
            .build()

        s3Client.putObject(request, AsyncRequestBody.fromFile(tempFile)).await()

        tempFile.delete()

        return StoredFileMetaData(actualFilename, subfolder, file)
    }

    override suspend fun fileExists(subfolder: String, filename: String): Boolean {
        val key = "$subfolder/$filename"

        return try {
            s3Client.headObject {
                it.bucket(s3Properties.bucket).key(key)
            }.await()
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }

    override suspend fun loadFile(file: StoredFileMetaData): StoredFile {
        val key = "${file.subfolder}/${file.filename}"
        val inputStream = withContext(Dispatchers.IO) {
            s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(s3Properties.bucket)
                    .key(key)
                    .build(),
                AsyncResponseTransformer.toBlockingInputStream()
            ).await()
        }

        return StoredFile(InputStreamResource(inputStream), file)
    }

    override suspend fun removeFile(subfolder: String, filename: String) {
        val key = "$subfolder/$filename"
        s3Client.deleteObject {
            it.bucket(s3Properties.bucket).key(key)
        }.await()
    }

    override suspend fun removeFileIfExists(subfolder: String, filename: String): Boolean {
        return try {
            removeFile(subfolder, filename)
            true
        } catch (_: NoSuchKeyException) {
            false
        }
    }
}
