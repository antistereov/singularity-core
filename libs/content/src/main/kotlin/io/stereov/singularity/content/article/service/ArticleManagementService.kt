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
import io.stereov.singularity.core.global.service.file.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.core.global.service.file.service.FileStorage
import io.stereov.singularity.core.user.model.Role
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleManagementService(
    private val articleService: ArticleService,
    private val authenticationService: AuthenticationService,
    private val fileStorage: FileStorage
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

    suspend fun changeHeader(key: String, req: ChangeArticleHeaderRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing header of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)
        val uniqueKey = getUniqueKey(req.title.toSlug(), article.id)
        article.key = uniqueKey
        article.path = "${Article.basePath}/$uniqueKey"
        translation.title = req.title
        article.colors = req.colors

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

    suspend fun changeImage(key: String, file: FilePart, lang: Language): FullArticleResponse {
        logger.debug { "Changing image of article with key \"$key\"" }

        val article = validatePermissionsAndGetByKey(key)
        val userId = authenticationService.getCurrentUserId()

        val currentImage = article.image

        if (currentImage != null) {
            fileStorage.removeFileIfExists(currentImage.key)
        }

        val allowedMediaTypes = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)

        val contentType = file.headers().contentType
            ?: throw UnsupportedMediaTypeException("Media type is not set")

        if (contentType !in allowedMediaTypes) {
            throw UnsupportedMediaTypeException("Unsupported file type: $contentType")
        }

        val imageKey = Article.basePath.replace("/", "") + "/" + article.key

        val image = fileStorage.upload(userId, file, imageKey, true)

        article.image = image

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

    private suspend fun getUniqueKey(baseKey: String, id: ObjectId? = null): String {
        val existingArticle = articleService.findByKeyOrNull(baseKey) ?: return baseKey

        return if (id != existingArticle.id) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey
    }

}
