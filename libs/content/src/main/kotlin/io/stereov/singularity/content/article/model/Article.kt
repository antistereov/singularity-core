package io.stereov.singularity.content.article.model

import io.stereov.singularity.content.article.dto.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.CreateArticleRequest
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.common.content.dto.ContentAccessDetailsResponse
import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.global.language.model.Translatable
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
    val colors: ArticleColors = ArticleColors(),
    val image: FileMetaData? = null,
    override var trusted: Boolean,
    override val tags: MutableSet<String> = mutableSetOf(),
    override val translations: MutableMap<Language, ArticleTranslation> = mutableMapOf(),
    override val primaryLanguage: Language
) : ContentDocument<Article>(), Translatable<ArticleTranslation> {

    override val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("No id found")

    fun toOverviewResponse(lang: Language, viewer: UserDocument?): ArticleOverviewResponse {
        val access = ContentAccessDetailsResponse.create(access, viewer)
        val (lang, translation) = translate(lang)

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
            tags = tags,
            title = translation.title,
            summary = translation.summary,
            access = access,
            lang = lang
        )
    }

    companion object {
        private val basePath: String
            get() = "/articles"

        fun create(req: CreateArticleRequest, key: String, ownerId: ObjectId): Article {
            val translations = mutableMapOf(req.lang to ArticleTranslation(req.title, req.summary, req.content))

            return Article(
                _id = null,
                key = key,
                createdAt = Instant.now(),
                publishedAt = null,
                updatedAt = Instant.now(),
                path = "$basePath/$key",
                state = ArticleState.DRAFT,
                colors = ArticleColors(),
                image = null,
                trusted = false,
                access = ContentAccessDetails(ownerId),
                translations = translations,
                primaryLanguage = req.lang
            )
        }

        fun create(dto: FullArticleResponse, language: Language): Article {
            val translations = mutableMapOf(language to ArticleTranslation(dto.title, dto.summary, dto.content))

            return Article(
                _id = dto.id,
                key = dto.key,
                createdAt = dto.createdAt,
                publishedAt = dto.publishedAt,
                updatedAt = dto.updatedAt,
                path = dto.path,
                state = dto.state,
                colors = dto.colors,
                image = dto.image,
                trusted = dto.trusted,
                access = ContentAccessDetails(dto.access),
                translations = translations,
                primaryLanguage = language
            )
        }
    }
}
