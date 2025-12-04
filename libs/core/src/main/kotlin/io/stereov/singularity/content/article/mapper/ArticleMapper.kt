package io.stereov.singularity.content.article.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.recoverIf
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.exception.CreateFullArticleResponseException
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.translate.service.TranslateService
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = true)
class ArticleMapper(
    private val appProperties: AppProperties,
    private val userService: UserService,
    private val translateService: TranslateService,
    private val tagMapper: TagMapper,
    private val tagService: TagService,
    private val fileStorage: FileStorage,
    private val principalMapper: PrincipalMapper,
) {

    fun createOverview(article: FullArticleResponse): ArticleOverviewResponse {
        return ArticleOverviewResponse(
            id = article.id,
            key = article.key,
            createdAt = article.createdAt,
            publishedAt = article.publishedAt,
            updatedAt = article.updatedAt,
            path = article.path,
            state = article.state,
            colors = article.colors,
            image = article.image,
            locale = article.locale,
            title = article.title,
            summary = article.summary,
            access = article.access,
            tags = article.tags
        )
    }

    fun createArticle(dto: FullArticleResponse, locale: Locale?): Article {
        val actualLocale = locale ?: appProperties.locale

        val translations = mutableMapOf(actualLocale to ArticleTranslation(dto.title, dto.summary, dto.content))
        requireNotNull(dto.owner) { "Owner of article ${dto.key} does not exist" }

        return Article(
            _id = dto.id,
            key = dto.key,
            createdAt = dto.createdAt,
            publishedAt = dto.publishedAt,
            updatedAt = dto.updatedAt,
            path = dto.path,
            state = dto.state,
            colors = dto.colors,
            imageKey = dto.image?.key,
            trusted = dto.trusted,
            access = ContentAccessDetails.create(dto.access, dto.owner.id),
            translations = translations,
        )
    }

    suspend fun createFullResponse(
        article: Article,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?,
        ownerId: ObjectId? = null
    ): Result<FullArticleResponse, CreateFullArticleResponseException> = coroutineBinding {
        val actualOwnerId = ownerId ?: article.access.ownerId
        val actualOwner = userService.findById(actualOwnerId)
            .recoverIf(
                { it is FindEncryptedDocumentByIdException.NotFound },
                { null }
            )
            .mapError { CreateFullArticleResponseException.Database("Failed to find owner of article with key ${article.key}: ${it.message}", it) }
            .bind()

        val access = ContentAccessDetailsResponse.create(article.access, authenticationOutcome)
        val (articleLang, translation) = translateService.translate(article, locale)
            .mapError { ex -> CreateFullArticleResponseException.NoTranslations("Article contains no translations: ${ex.message}", ex) }
            .bind()

        val tags = article.tags.mapNotNull { key ->
            tagService.findByKey(key)
                .recoverIf(
                    { it is FindDocumentByKeyException.NotFound },
                    {
                        article.tags.remove(key)
                        null
                    }
                )
                .mapError {
                    CreateFullArticleResponseException.Database(
                        "Failed to find tag with key $key: ${it.message}",
                        it
                    )
                }
                .bind()
        }
            .map { tag ->
                tagMapper.createTagResponse(tag, articleLang)
                    .mapError { ex -> CreateFullArticleResponseException.Database("Failed to map tag to response: ${ex.message}", ex) }
                    .bind()
            }


        val image = article.imageKey?.let { fileStorage.metadataResponseByKey(it, authenticationOutcome) }
            ?.mapError { ex -> CreateFullArticleResponseException.File("Failed to fetch image metadata: ${ex.message}", ex) }
            ?.bind()

        val articleId = article.id
            .mapError { ex -> CreateFullArticleResponseException.Database("Failed to extract ID from article: ${ex.message}", ex) }
            .bind()

        val ownerOverview = actualOwner?.let {
            principalMapper.toOverview(actualOwner, authenticationOutcome)
                .mapError { ex -> CreateFullArticleResponseException.Database("Failed to map owner to overview: ${ex.message}", ex) }
                .bind()
        }

        FullArticleResponse(
            id = articleId,
            key = article.key,
            createdAt = article.createdAt,
            publishedAt = article.publishedAt,
            updatedAt = article.updatedAt,
            owner = ownerOverview,
            path = article.path,
            state = article.state,
            colors = article.colors,
            image = image,
            trusted = article.trusted,
            access = access,
            locale = articleLang,
            title = translation.title,
            summary = translation.summary,
            content = translation.content,
            tags = tags
        )
    }

    suspend fun createOverview(article: Article, authenticationOutcome: AuthenticationOutcome, locale: Locale?): Result<ArticleOverviewResponse, CreateFullArticleResponseException> {
        return createFullResponse(article, authenticationOutcome, locale).map(::createOverview)
    }
}
