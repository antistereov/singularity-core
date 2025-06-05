package io.stereov.singularity.content.article.model

import io.stereov.singularity.content.article.dto.CreateArticleRequest
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.file.model.FileMetaData
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "articles")
data class Article(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) override var key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override var access: ContentAccessDetails,
    val publishedAt: Instant? = null,
    var path: String,
    var state: ArticleState = ArticleState.DRAFT,
    var colors: ArticleColors = ArticleColors(),
    var image: FileMetaData? = null,
    override var trusted: Boolean,
    override var tags: MutableSet<String> = mutableSetOf(),
    override val translations: MutableMap<Language, ArticleTranslation> = mutableMapOf(),
    override val primaryLanguage: Language
) : ContentDocument<Article>(), Translatable<ArticleTranslation> {

    override val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("No id found")

    companion object {
        val basePath: String
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
                access = ContentAccessDetails.create(dto.access, dto.owner.id),
                translations = translations,
                primaryLanguage = language
            )
        }
    }
}
