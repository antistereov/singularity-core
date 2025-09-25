package io.stereov.singularity.content.article.dto.response

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*

data class FullArticleResponse(
    val id: ObjectId,
    val key: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
    val updatedAt: Instant,
    val owner: UserOverviewResponse,
    val path: String,
    var state: ArticleState = ArticleState.DRAFT,
    val colors: ArticleColors,
    val image: FileMetadataResponse?,
    val locale: Locale,
    val title: String,
    val summary: String,
    val content: String,
    val trusted: Boolean,
    val access: ContentAccessDetailsResponse,
    val tags: List<TagResponse>
) : ContentResponse<Article>
