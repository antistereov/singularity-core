package io.stereov.singularity.stereovio.content.article.model

import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.stereovio.content.article.dto.ArticleOverviewDto
import io.stereov.singularity.stereovio.content.article.dto.FullArticleDto
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
    val createdAt: Instant,
    val publishedAt: Instant?,
    val updatedAt: Instant,
    val path: String,
    val state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val summary: String,
    val colors: ArticleColors,
    val image: FileMetaData,
    val content: ArticleContent
) {

    val id: String
        get() = _id ?: throw InvalidDocumentException("No id found")

    fun toContentDto() = FullArticleDto(id, key, createdAt, publishedAt, updatedAt, path, state,
        title, colors, summary, image, content)

    fun toSlideDto() = ArticleOverviewDto(id, key, createdAt, publishedAt, updatedAt, path, state,
        title, colors, summary, image)
}
