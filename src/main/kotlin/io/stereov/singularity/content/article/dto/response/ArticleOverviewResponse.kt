package io.stereov.singularity.content.article.dto.response

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*

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
    val locale: Locale,
    val summary: String,
    val image: FileMetadataResponse?,
    val access: ContentAccessDetailsResponse,
    val tags: List<TagResponse>
) : ContentResponse<Article>
