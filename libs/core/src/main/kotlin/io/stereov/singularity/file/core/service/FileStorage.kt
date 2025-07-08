package io.stereov.singularity.file.core.service

import io.stereov.singularity.file.core.model.FileMetaData
import org.bson.types.ObjectId
import org.springframework.http.codec.multipart.FilePart

interface FileStorage {

    suspend fun upload(userId: ObjectId, filePart: FilePart, key: String, public: Boolean): FileMetaData
    suspend fun fileExists(key: String): Boolean
    suspend fun removeFile(key: String)
    suspend fun removeFileIfExists(key: String): Boolean
    suspend fun getPublicUrl(key: String): String
    suspend fun getPresignedUrl(key: String): String
}