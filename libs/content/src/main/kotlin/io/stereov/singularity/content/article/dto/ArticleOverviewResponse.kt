package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.common.content.dto.ContentAccessDetailsResponse
import io.stereov.singularity.content.common.tag.dto.TagResponse
import io.stereov.singularity.file.model.FileMetaData
import io.stereov.singularity.translate.model.Language
import org.bson.types.ObjectId
import java.time.Instant

data class ArticleOverviewResponse(
    val id: ObjectId,
    val key: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
    val updatedAt: Instant,
    val path: String,
    val state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val colors: ArticleColors,
    val lang: Language,
    val summary: String,
    val image: FileMetaData?,
    val access: ContentAccessDetailsResponse,
    val tags: List<TagResponse>
)
