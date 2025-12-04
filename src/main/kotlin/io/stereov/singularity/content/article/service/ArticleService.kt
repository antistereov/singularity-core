package io.stereov.singularity.content.article.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.exception.GetArticleResponseByKeyException
import io.stereov.singularity.content.article.exception.GetArticleResponsesException
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.database.core.util.CriteriaBuilder
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.mapContent
import io.stereov.singularity.translate.service.TranslatableCrudService
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = true)
class ArticleService(
    override val repository: ArticleRepository,
    override val authorizationService: AuthorizationService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    override val accessCriteria: AccessCriteria,
    override val translateService: TranslateService,
    private val articleMapper: ArticleMapper,
    override val contentProperties: ContentProperties,
    override val appProperties: AppProperties
) : ContentService<Article>(), TranslatableCrudService<ArticleTranslation, Article> {

    override val logger: KLogger = KotlinLogging.logger {}
    override val collectionClazz: Class<Article> = Article::class.java
    override val contentType = Article.CONTENT_TYPE

    suspend fun getResponseByKey(
        key: String,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<FullArticleResponse, GetArticleResponseByKeyException> = coroutineBinding {
        val article = findAuthorizedByKey(key, authenticationOutcome,ContentAccessRole.VIEWER)
            .mapError { GetArticleResponseByKeyException.from(it) }
            .bind()
        articleMapper.createFullResponse(article, authenticationOutcome, locale)
            .mapError { GetArticleResponseByKeyException.from(it) }
            .bind()
    }

    suspend fun getArticles(
        pageable: Pageable,
        authenticationOutcome: AuthenticationOutcome,
        title: String?,
        content: String?,
        state: String?,
        tags: List<String>,
        roles: Set<String>,
        createdAtBefore: Instant?,
        createdAtAfter: Instant?,
        updatedAtBefore: Instant?,
        updatedAtAfter: Instant?,
        publishedAtBefore: Instant?,
        publishedAtAfter: Instant?,
        locale: Locale?
    ): Result<Page<ArticleOverviewResponse>, GetArticleResponsesException> = coroutineBinding {
        val actualLocale = locale ?: translateService.defaultLocale

        val criteria = CriteriaBuilder(accessCriteria.generate(roles))
            .compare(Article::createdAt, createdAtBefore, createdAtAfter)
            .compare(Article::updatedAt, updatedAtBefore, updatedAtAfter)
            .compare(Article::publishedAt, publishedAtBefore, publishedAtAfter)
            .fieldContains(ArticleTranslation::title, title, actualLocale)
            .fieldContains(ArticleTranslation::content, content, actualLocale)
            .isEqualTo(Article::state, state?.let { ArticleState.fromString(it) })
            .isIn(Article::tags, tags)
            .build()

        val articles = findAllPaginated(pageable, criteria, actualLocale)
            .mapError { GetArticleResponsesException.from(it) }
            .bind()

        articles.mapContent {
            articleMapper.createOverview(it, authenticationOutcome, actualLocale)
                .mapError { GetArticleResponsesException.from(it) }
                .bind()
        }
    }
}
