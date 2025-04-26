package io.stereov.singularity.stereovio.content.article.model

import io.stereov.singularity.global.service.file.model.FileMetaData
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
@Serializable
data class Article(
    @Id
    val _id: String? = null,
    @Indexed(unique = true)
    val key: String,
    @Serializable(with = InstantSerializer::class)
    val publishedAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val backgroundImage: FileMetaData,
    val header: String,
    val summary: String,
    val url: String,
    val headerBackgroundColor: String,
    val headerTextColor: String,
) {

    val id: String
        get() = _id ?: throw InvalidDocumentException("No id found");
}
