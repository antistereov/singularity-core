package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.core.dto.ContentAccessDetailsResponse
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse
import org.bson.types.ObjectId
import java.time.Instant

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
    val lang: Language,
    val title: String,
    val summary: String,
    val content: String,
    val trusted: Boolean,
    val access: ContentAccessDetailsResponse,
    val tags: List<TagResponse>
) {

    fun toOverview(): ArticleOverviewResponse {
        return ArticleOverviewResponse(
            id = id,
            key = key,
            createdAt = createdAt,
            publishedAt = publishedAt,
            updatedAt = updatedAt,
            path = path,
            state = state,
            colors = colors,
            image = image,
            lang = lang,
            title = title,
            summary = summary,
            access = access,
            tags = tags
        )
    }
}
