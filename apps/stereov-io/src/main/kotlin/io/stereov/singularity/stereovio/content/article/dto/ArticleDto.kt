package io.stereov.singularity.stereovio.content.article.dto

import io.stereov.singularity.core.global.serializer.InstantSerializer
import io.stereov.singularity.stereovio.content.article.model.ArticleState
import io.stereov.singularity.stereovio.content.article.model.Slide
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ArticleDto(
    val id: String? = null,
    val key: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val publishedAt: Instant?,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val url: String,
    val state: ArticleState = ArticleState.DRAFT,
    val slide: Slide,
)
