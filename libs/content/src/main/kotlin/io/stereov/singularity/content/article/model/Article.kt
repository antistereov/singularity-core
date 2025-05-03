package io.stereov.singularity.content.article.model

import io.stereov.singularity.content.article.dto.ArticleOverviewDto
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "articles")
data class Article(
    @Id
    val _id: String? = null,
    @Indexed(unique = true)
    val key: String,
    val creatorId: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
    val updatedAt: Instant,
    val path: String,
    val state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val summary: String,
    val colors: ArticleColors,
    val image: FileMetaData?,
    val content: ArticleContent,
    val accessType: AccessType,
    val canEdit: MutableSet<String>,
    val canView: MutableSet<String>,
    var trusted: Boolean,
) {

    val id: String
        get() = _id ?: throw InvalidDocumentException("No id found")

    fun toOverviewDto() = ArticleOverviewDto(id, key, createdAt, publishedAt, updatedAt, path, state,
        title, colors, summary, image)

    companion object {
        val basePath: String
            get() = "/content/articles"

        fun create(key: String, creatorId: String, title: String, summary: String,
                   colors: ArticleColors, image: FileMetaData, content: ArticleContent,
                   accessType: AccessType, state: ArticleState = ArticleState.DRAFT, trusted: Boolean
        ): Article {
            return Article(
                _id = null,
                key = key,
                creatorId = creatorId,
                createdAt = Instant.now(),
                publishedAt = null,
                updatedAt = Instant.now(),
                path = "$basePath/$key",
                state = state,
                title = title,
                summary = summary,
                colors = colors,
                image = image,
                content = content,
                accessType = accessType,
                canEdit = mutableSetOf(creatorId),
                canView = mutableSetOf(creatorId),
                trusted = trusted
            )
        }
    }
}
