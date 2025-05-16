package io.stereov.singularity.content.article.model

import io.stereov.singularity.content.article.dto.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.common.dto.ContentAccessDetailsResponse
import io.stereov.singularity.content.common.model.ContentAccessDetails
import io.stereov.singularity.content.common.model.ContentAccessPermissions
import io.stereov.singularity.content.common.model.ContentDocument
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.user.model.UserDocument
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "articles")
data class Article(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) override val key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override val access: ContentAccessDetails,
    val publishedAt: Instant? = null,
    val path: String,
    var state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val summary: String = "",
    val colors: ArticleColors = ArticleColors(),
    val image: FileMetaData? = null,
    val content: String = "",
    override var trusted: Boolean,
    override val tags: MutableSet<String> = mutableSetOf()
) : ContentDocument<Article>() {

    override val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("No id found")

    fun toOverviewResponse(viewer: UserDocument?) = ArticleOverviewResponse(id, key, createdAt, publishedAt, updatedAt, path, state,
        title, colors, summary, image, ContentAccessDetailsResponse.create(access, viewer), tags)

    companion object {
        private val basePath: String
            get() = "/articles"

        fun create(key: String, ownerId: ObjectId, title: String = "", summary: String = "",
                   colors: ArticleColors = ArticleColors(), image: FileMetaData? = null, content: String = "",
                   accessType: AccessType = AccessType.PRIVATE, state: ArticleState = ArticleState.DRAFT,
                   trusted: Boolean = false,
                   userPermissions: ContentAccessPermissions = ContentAccessPermissions(),
                   groupPermissions: ContentAccessPermissions = ContentAccessPermissions()
        ): Article {
            return Article(
                _id = null,
                key = key,
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
                trusted = trusted,
                access = ContentAccessDetails(ownerId, accessType, users = userPermissions, groups = groupPermissions)
            )
        }

        fun create(dto: FullArticleResponse): Article {
            return Article(
                _id = dto.id,
                key = dto.key,
                createdAt = dto.createdAt,
                publishedAt = dto.publishedAt,
                updatedAt = dto.updatedAt,
                path = dto.path,
                state = dto.state,
                title = dto.title,
                summary = dto.summary,
                colors = dto.colors,
                image = dto.image,
                content = dto.content,
                trusted = dto.trusted,
                access = ContentAccessDetails(dto.access)
            )
        }
    }
}
