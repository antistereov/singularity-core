package io.stereov.web.global.service.file.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.file.exception.model.DeleteFailedException
import io.stereov.web.global.service.file.exception.model.FileSecurityException
import io.stereov.web.global.service.file.exception.model.NoSuchFileException
import io.stereov.web.properties.AppProperties
import io.stereov.web.properties.FileProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import kotlin.io.path.Path

@Service
class FileService(
    private val fileProperties: FileProperties,
    private val appProperties: AppProperties,
) {

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
    suspend fun storeFile(file: FilePart, subfolder: String, filename: String? = null): String {
        logger.debug { "Storing file ${file.filename()} to ${subfolder}/${filename}" }

        checkSecurity(subfolder)

        val targetDir = Path(fileProperties.basePath, subfolder).toFile()
        if (!targetDir.exists()) targetDir.mkdirs()

        val targetFileName = filename
            ?: file.filename()
            ?: UUID.randomUUID().toString()

        val targetFile = File(targetDir, targetFileName)

        file.transferTo(targetFile).awaitSingleOrNull()

        return getFileUrl(subfolder, targetFileName)
    }

    suspend fun fileExists(subfolder: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(fileProperties.basePath, subfolder, filename).toFile()

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
        val file = Path(fileProperties.basePath, subfolder, filename).toFile()

        if (!file.exists()) throw NoSuchFileException("File not found: ${file.absolutePath}")
    }

    suspend fun loadFile(subfolder: String, filename: String): File = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(fileProperties.basePath, subfolder, filename).toFile()
        if (!file.exists()) throw NoSuchFileException("File not found: ${file.absolutePath}")
        return@withContext file
    }

    suspend fun removeFile(subfolder: String, filename: String) = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(fileProperties.basePath, subfolder, filename).toFile()

        if (!file.exists()) throw NoSuchFileException("File not found: ${file.absolutePath}")
        if (!file.delete()) throw DeleteFailedException("File could not be deleted: ${file.absolutePath}")
    }

    suspend fun removeFileIfExists(subfolder: String, filename: String) = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(fileProperties.basePath, subfolder, filename).toFile()

        return@withContext file.delete()
    }

    private fun checkSecurity(subfolder: String) {
        val canonicalBase = File(fileProperties.basePath).canonicalFile

        val file = Path(fileProperties.basePath, subfolder).toFile()
        val canonicalTarget = file.canonicalFile

        if (!canonicalTarget.path.startsWith(canonicalBase.path)) {
            throw FileSecurityException("Invalid path: ${file.absolutePath}")
        }
    }

    /**
     * Get the URL of the given file. It will throw a [NoSuchFileException] if the file does not exits.
     *
     * @param subfolder The subfolder relative to the base directory.
     * @param filename The name of the file.
     *
     * @throws NoSuchFileException If the file does not exist.
     */
    suspend fun getFileUrl(subfolder: String, filename: String): String {
        checkExistence(subfolder, filename)

        return "${appProperties.baseUrl}/static/${subfolder}/${filename}"
    }
}
