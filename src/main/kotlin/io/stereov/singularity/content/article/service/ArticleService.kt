package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.global.util.CriteriaBuilder
import io.stereov.singularity.global.util.mapContent
import io.stereov.singularity.global.util.withLocalizedSort
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
) : ContentService<Article>() {

    override val logger: KLogger = KotlinLogging.logger {}
    override val collectionClazz: Class<Article> = Article::class.java

    suspend fun getResponseByKey(key: String, locale: Locale?): FullArticleResponse {
        return articleMapper.createFullResponse(findAuthorizedByKey(key, ContentAccessRole.VIEWER), locale)
    }

    suspend fun getArticles(
        pageable: Pageable,
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
    ): Page<ArticleOverviewResponse> {
        val actualLocale = locale ?: translateService.defaultLocale

        val localizedPageable = pageable.withLocalizedSort(actualLocale, listOf(
            ArticleTranslation::title,
            ArticleTranslation::content,
            ArticleTranslation::summary,
        ))

        val criteria = CriteriaBuilder(accessCriteria.getAccessCriteria(roles))
            .compare(Article::createdAt, createdAtBefore, createdAtAfter)
            .compare(Article::updatedAt, updatedAtBefore, updatedAtAfter)
            .compare(Article::publishedAt, publishedAtBefore, publishedAtAfter)
            .fieldContains(ArticleTranslation::title, title, actualLocale)
            .fieldContains(ArticleTranslation::content, content, actualLocale)
            .isEqualTo(Article::state, state?.let { ArticleState.fromString(it) })
            .isIn(Article::tags, tags)
            .build()

        return findAllPaginated(localizedPageable, criteria).mapContent { articleMapper.createOverview(it, actualLocale) }
    }
}
