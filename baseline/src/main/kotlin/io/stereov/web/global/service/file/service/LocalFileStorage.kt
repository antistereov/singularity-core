package io.stereov.web.global.service.file.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.file.exception.model.DeleteFailedException
import io.stereov.web.global.service.file.exception.model.FileSecurityException
import io.stereov.web.global.service.file.exception.model.NoSuchFileException
import io.stereov.web.global.service.file.model.StoredFile
import io.stereov.web.global.service.file.model.StoredFileMetaData
import io.stereov.web.properties.LocalFileStorageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.InputStreamResource
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import kotlin.io.path.Path

@Service
@ConditionalOnProperty(prefix = "baseline.file.storage", name = ["type"], havingValue = "local", matchIfMissing = false)
class LocalFileStorage(
    private val properties: LocalFileStorageProperties,
) : FileStorage {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Store a file and return the URL.
     *
     * @param file The file to be stored.
     * @param subfolder The folder relative to the base path the file should be stored in.
     * @param filename The name of the file to be stored.
     *
     * @return The URL of the file
     */
    override suspend fun storeFile(file: FilePart, subfolder: String, filename: String?): StoredFileMetaData {
        logger.debug { "Storing file ${file.filename()} to ${subfolder}/${filename}" }

        checkSecurity(subfolder)

        val targetDir = Path(properties.basePath, subfolder).toFile()
        if (!targetDir.exists()) targetDir.mkdirs()

        val targetFileName = filename
            ?: file.filename()
            ?: UUID.randomUUID().toString()

        val targetFile = File(targetDir, targetFileName)

        file.transferTo(targetFile).awaitSingleOrNull()

        return StoredFileMetaData(targetFileName, subfolder, file)
    }

    override suspend fun fileExists(subfolder: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(properties.basePath, subfolder, filename).toFile()

        return@withContext file.exists()
    }

    /**
     * Check if the given file exists.
     *
     * @param subfolder The subfolder relative to the base directory.
     * @param filename The name of the file.
     *
     * @throws NoSuchFileException If the file does not exist.
     */
    suspend fun checkExistence(subfolder: String, filename: String) {
        val file = Path(properties.basePath, subfolder, filename).toFile()

        if (!file.exists()) throw NoSuchFileException("File not found: ${file.absolutePath}")
    }

    override suspend fun loadFile(fileMetaData: StoredFileMetaData): StoredFile = withContext(Dispatchers.IO) {
        checkSecurity(fileMetaData.subfolder)

        val file = Path(properties.basePath, fileMetaData.subfolder, fileMetaData.filename).toFile()
        if (!file.exists()) throw NoSuchFileException("File not found: ${file.absolutePath}")
        return@withContext StoredFile(
            InputStreamResource(file.inputStream()),
            fileMetaData
        )
    }

    override suspend fun removeFile(subfolder: String, filename: String) = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(properties.basePath, subfolder, filename).toFile()

        if (!file.exists()) throw NoSuchFileException("File not found: ${file.absolutePath}")
        if (!file.delete()) throw DeleteFailedException("File could not be deleted: ${file.absolutePath}")
    }

    override suspend fun removeFileIfExists(subfolder: String, filename: String) = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(properties.basePath, subfolder, filename).toFile()

        return@withContext file.delete()
    }

    private fun checkSecurity(subfolder: String) {
        val canonicalBase = File(properties.basePath).canonicalFile

        val file = Path(properties.basePath, subfolder).toFile()
        val canonicalTarget = file.canonicalFile

        if (!canonicalTarget.path.startsWith(canonicalBase.path)) {
            throw FileSecurityException("Invalid path: ${file.absolutePath}")
        }
    }
}
