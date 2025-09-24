package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.request.*
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.core.dto.*
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.global.util.toSlug
import io.stereov.singularity.translate.exception.model.TranslationForLangMissingException
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleManagementService(
    override val contentService: ArticleService,
    override val authorizationService: AuthorizationService,
    override val invitationService: InvitationService,
    private val fileStorage: FileStorage,
    private val translateService: TranslateService,
    private val uiProperties: UiProperties,
    override val userService: UserService,
    override val userMapper: UserMapper,
    private val articleMapper: ArticleMapper,
    private val appProperties: AppProperties
) : ContentManagementService<Article> {

    override val logger = KotlinLogging.logger {}
    override val acceptPath = "/articles/invite/accept"

    suspend fun create(req: CreateArticleRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Creating article with title ${req.title}" }

        contentService.requireEditorGroupMembership()
        val user = authorizationService.getCurrentUser()

        val key = getUniqueKey(req.title.toSlug())

        val article = articleMapper.createArticle(req, key, user.id)
        val savedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(savedArticle, locale,user)
    }

    suspend fun setTrustedState(key: String, trusted: Boolean): Article {
        logger.debug { "Setting trusted state" }
        authorizationService.requireRole(Role.ADMIN)

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        article.trusted = trusted
        return contentService.save(article)
    }

    suspend fun inviteUser(key: String, req: InviteUserToContentRequest, locale: Locale?): ExtendedContentAccessDetailsResponse {
        logger.debug { "Inviting user with email \"${req.email}\" to role ${req.role} on article with key \"$key\"" }

        val actualLocale = locale ?: appProperties.locale

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val inviteToRole = translateService.translateResourceKey(
            TranslateKey(
                "invitation.role.${req.role.toString().lowercase()}"
            ), "i18n/content/article", actualLocale)
        val action = translateService.translateResourceKey(TranslateKey("invitation.action"), "i18n/content/article", actualLocale)
        val articleTitle = translateService.translate(article, locale).translation.title
        val articleRef = "<a href=\"${uiProperties.baseUrl}/${article.path.removePrefix("/")}\" style=\"color: black;\">$articleTitle</a>"

        val invitedTo = "$inviteToRole $action $articleRef"

        return inviteUser(key, req, invitedTo, locale)
    }

    suspend fun acceptInvitationAndGetFullArticle(req: AcceptInvitationToContentRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Accepting invitation" }

        val article = acceptInvitation(req)

        return contentService.fullArticledResponseFrom(article, locale)
    }

    suspend fun changeHeader(key: String, req: ChangeArticleHeaderRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing header of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val translation = article.translations[req.locale]
            ?: throw TranslationForLangMissingException(req.locale)
        val uniqueKey = getUniqueKey(req.title.toSlug(), article.id)
        article.key = uniqueKey
        article.path = "${Article.basePath}/$uniqueKey"
        translation.title = req.title
        article.colors = req.colors

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, locale)
    }

    suspend fun changeSummary(key: String, req: ChangeArticleSummaryRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing summary of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val translation = article.translations[req.locale]
            ?: throw TranslationForLangMissingException(req.locale)
        translation.summary = req.summary

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, locale)
    }

    suspend fun changeContent(key: String, req: ChangeArticleContentRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing content of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val translation = article.translations[req.locale]
            ?: throw TranslationForLangMissingException(req.locale)

        translation.content = req.content

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, locale)
    }

    suspend fun changeImage(key: String, file: FilePart, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing image of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        val userId = authorizationService.getCurrentUserId()

        val currentImage = article.imageKey

        if (currentImage != null) {
            fileStorage.remove(currentImage)
        }

        val allowedMediaTypes = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)

        val contentType = file.headers().contentType
            ?: throw UnsupportedMediaTypeException("Media type is not set")

        if (contentType !in allowedMediaTypes) {
            throw UnsupportedMediaTypeException("Unsupported file type: $contentType")
        }

        val imageKey = Article.basePath.replace("/", "") + "/" + article.key

        val image = fileStorage.upload(userId, file, imageKey, true)

        article.imageKey = image.key

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, locale)
    }

    suspend fun changeState(key: String, req: ChangeArticleStateRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing satte of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        article.state = req.state

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, locale)
    }

    suspend fun changeTags(key: String, req: ChangeContentTagsRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing tags of article with key \"$key\"" }

        val article = changeTags(key, req)

        return contentService.fullArticledResponseFrom(article, locale)
    }

    suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing visibility of article with key \"$key\"" }

        val article = changeVisibility(key, req)

        return contentService.fullArticledResponseFrom(article, locale)
    }

    private suspend fun getUniqueKey(baseKey: String, id: ObjectId? = null): String {
        val existingArticle = contentService.findByKeyOrNull(baseKey) ?: return baseKey

        return if (id != existingArticle.id) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey
    }

}
