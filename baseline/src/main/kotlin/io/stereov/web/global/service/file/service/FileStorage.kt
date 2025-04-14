package io.stereov.web.global.service.file.service

import io.stereov.web.global.service.file.model.StoredFileMetaData
import io.stereov.web.global.service.file.model.StoredFile
import org.springframework.http.codec.multipart.FilePart

interface FileStorage {
    suspend fun storeFile(file: FilePart, subfolder: String, filename: String? = null): StoredFileMetaData

    suspend fun fileExists(subfolder: String, filename: String): Boolean
    suspend fun fileExists(file: StoredFileMetaData): Boolean {
        return fileExists(file.subfolder, file.filename)
    }

    suspend fun loadFile(file: StoredFileMetaData): StoredFile

    suspend fun removeFile(subfolder: String, filename: String)
    suspend fun removeFile(file: StoredFileMetaData) {
        return removeFile(file.subfolder, file.filename)
    }

    suspend fun removeFileIfExists(subfolder: String, filename: String): Boolean
    suspend fun removeFileIfExists(file: StoredFileMetaData): Boolean {
        return removeFileIfExists(file.subfolder, file.filename)
    }
}
