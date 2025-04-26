package io.stereov.singularity.stereovio.content.article.model

import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.stereovio.content.article.dto.ArticleDto
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
    val url: String,
    val state: ArticleState = ArticleState.DRAFT,
    val slide: Slide,
) {

    val id: String
        get() = _id ?: throw InvalidDocumentException("No id found")

    fun toDto() = ArticleDto(id, key, createdAt, publishedAt, updatedAt, url, state, slide)
}
