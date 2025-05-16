package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.common.model.ContentAccessDetails
import io.stereov.singularity.core.global.serializer.InstantSerializer
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ArticleOverviewResponse(
    val id: String? = null,
    val key: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val publishedAt: Instant?,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val path: String,
    val state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val colors: ArticleColors,
    val summary: String,
    val image: FileMetaData?,
    val access: ContentAccessDetails,
)
