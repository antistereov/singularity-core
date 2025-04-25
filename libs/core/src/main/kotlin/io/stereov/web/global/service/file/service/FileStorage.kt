package io.stereov.web.global.service.file.service

import io.stereov.web.auth.model.AccessType
import io.stereov.web.global.service.file.model.FileMetaData
import org.springframework.http.codec.multipart.FilePart

interface FileStorage {

    suspend fun upload(userId: String, filePart: FilePart, key: String, public: Boolean): FileMetaData
    suspend fun fileExists(key: String): Boolean
    suspend fun removeFile(key: String)
    suspend fun removeFileIfExists(key: String): Boolean

    suspend fun getFileUrl(metaData: FileMetaData): String {
        return when (metaData.accessType) {
            AccessType.PUBLIC -> getPublicUrl(metaData.key)
            AccessType.PRIVATE, AccessType.SHARED -> getPresignedUrl(metaData.key)
        }
    }

    suspend fun getPublicUrl(key: String): String
    suspend fun getPresignedUrl(key: String): String
}
