package io.stereov.web.global.service.file.service

import io.stereov.web.global.service.file.exception.model.DeleteFailedException
import io.stereov.web.global.service.file.exception.model.FileSecurityException
import io.stereov.web.global.service.file.exception.model.NoSuchFileException
import io.stereov.web.properties.FileProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.*
import kotlin.io.path.Path

@Service
class FileService(
    private val fileProperties: FileProperties,
) {

    suspend fun storeFile(file: MultipartFile, subfolder: String): String = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val targetDir = Path(fileProperties.basePath, subfolder).toFile()
        if (!targetDir.exists()) targetDir.mkdirs()

        val targetFile = File(targetDir, file.originalFilename ?: UUID.randomUUID().toString())
        file.transferTo(targetFile)

        return@withContext targetFile.absolutePath
    }

    suspend fun fileExists(subfolder: String, filename: String): Boolean = withContext(Dispatchers.IO) {
        checkSecurity(subfolder)

        val file = Path(fileProperties.basePath, subfolder, filename).toFile()

        return@withContext file.exists()
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
}
