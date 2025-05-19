package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.common.content.dto.ChangeContentTagsRequest
import io.stereov.singularity.content.common.content.dto.ChangeContentVisibilityRequest
import io.stereov.singularity.content.common.content.service.ContentManagementService
import io.stereov.singularity.content.common.content.util.toSlug
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.global.language.exception.model.TranslationForLangMissingException
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.user.model.Role
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleManagementService(
    private val articleService: ArticleService,
    private val authenticationService: AuthenticationService,
) : ContentManagementService<Article>(articleService, authenticationService) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateArticleRequest, lang: Language): FullArticleResponse {
        logger.debug { "Creating article with title ${req.title}" }

        validatePermissions()
        val user = authenticationService.getCurrentUser()

        val key = getUniqueKey(req.title.toSlug())

        val article = Article.create(req, key,user.id)
        val savedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(savedArticle, lang,user)
    }

    suspend fun setTrustedState(key: String, trusted: Boolean): Article {
        authenticationService.validateAuthorization(Role.ADMIN)

        val article = validatePermissionsAndGetByKey(key)

        article.trusted = trusted
        return articleService.save(article)
    }

    suspend fun changeTitle(key: String, req: ChangeArticleTitleRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing title of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)
        translation.title = req.title
        val key = getUniqueKey(req.title.toSlug())
        article.key = key
        article.path = "${Article.basePath}/$key"

        val updatedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeSummary(key: String, req: ChangeArticleSummaryRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing summary of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)
        translation.summary = req.summary

        val updatedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeContent(key: String, req: ChangeArticleContentRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing content of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)

        translation.content = req.content

        val updatedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeColors(key: String, req: ChangeArticleColorsRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing colors of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)
        article.colors = req.colors

        val updatedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeImage(key: String, req: ChangeArticleImageRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing image of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)
        article.image = req.image

        val updatedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeState(key: String, req: ChangeArticleStateRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing satte of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)
        article.state = req.state

        val updatedArticle = articleService.save(article)

        return articleService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeTags(key: String, req: ChangeContentTagsRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing tags of article with key \"$key\"" }

        val article = changeTags(key, req)

        return articleService.fullArticledResponseFrom(article, lang)
    }

    suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing visibility of article with key \"$key\"" }

        val article = changeVisibility(key, req)

        return articleService.fullArticledResponseFrom(article, lang)
    }

    private suspend fun getUniqueKey(baseKey: String): String {
        return if (articleService.existsByKey(baseKey)) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey
    }

}
