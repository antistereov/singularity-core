package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.content.article.dto.*
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.common.content.dto.*
import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.content.common.content.util.toSlug
import io.stereov.singularity.content.core.content.service.ContentManagementService
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.invitation.service.InvitationService
import io.stereov.singularity.content.translate.exception.model.TranslationForLangMissingException
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.translate.model.TranslateKey
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.user.model.Role
import io.stereov.singularity.user.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleManagementService(
    override val contentService: ArticleService,
    override val authenticationService: AuthenticationService,
    override val invitationService: InvitationService,
    private val fileStorage: FileStorage,
    private val translateService: TranslateService,
    private val uiProperties: UiProperties,
    override val userService: UserService,
) : ContentManagementService<Article> {

    override val logger = KotlinLogging.logger {}
    override val acceptPath = "/articles/invite/accept"

    suspend fun create(req: CreateArticleRequest, lang: Language): FullArticleResponse {
        logger.debug { "Creating article with title ${req.title}" }

        contentService.requireEditorGroupMembership()
        val user = authenticationService.getCurrentUser()

        val key = getUniqueKey(req.title.toSlug())

        val article = Article.create(req, key,user.id)
        val savedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(savedArticle, lang,user)
    }

    suspend fun setTrustedState(key: String, trusted: Boolean): Article {
        logger.debug { "Setting trusted state" }
        authenticationService.requireRole(Role.ADMIN)

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        article.trusted = trusted
        return contentService.save(article)
    }

    suspend fun inviteUser(key: String, req: InviteUserToContentRequest, lang: Language): ExtendedContentAccessDetailsResponse {
        logger.debug { "Inviting user with email \"${req.email}\" to role ${req.role} on article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val inviteToRole = translateService.translate(
            TranslateKey(
                "invitation.role.${req.role.toString().lowercase()}"
            ), "i18n/content/article", lang)
        val action = translateService.translate(TranslateKey("invitation.action"), "i18n/content/article", lang)
        val articleRef = "<a href=\"${uiProperties.baseUrl}/${article.path.removePrefix("/")}\" style=\"color: black;\">${article.translate(lang).second.title}</a>"

        val invitedTo = "$inviteToRole $action $articleRef"

        return inviteUser(key, req, invitedTo, lang)
    }

    suspend fun acceptInvitationAndGetFullArticle(req: AcceptInvitationToContentRequest, lang: Language): FullArticleResponse {
        logger.debug { "Accepting invitation" }

        val article = acceptInvitation(req)

        return contentService.fullArticledResponseFrom(article, lang)
    }

    suspend fun changeHeader(key: String, req: ChangeArticleHeaderRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing header of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)
        val uniqueKey = getUniqueKey(req.title.toSlug(), article.id)
        article.key = uniqueKey
        article.path = "${Article.basePath}/$uniqueKey"
        translation.title = req.title
        article.colors = req.colors

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeSummary(key: String, req: ChangeArticleSummaryRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing summary of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)
        translation.summary = req.summary

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeContent(key: String, req: ChangeArticleContentRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing content of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        val translation = article.translations[req.lang]
            ?: throw TranslationForLangMissingException(lang)

        translation.content = req.content

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeImage(key: String, file: FilePart, lang: Language): FullArticleResponse {
        logger.debug { "Changing image of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        val userId = authenticationService.getCurrentUserId()

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

        return contentService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeState(key: String, req: ChangeArticleStateRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing satte of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        article.state = req.state

        val updatedArticle = contentService.save(article)

        return contentService.fullArticledResponseFrom(updatedArticle, lang)
    }

    suspend fun changeTags(key: String, req: ChangeContentTagsRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing tags of article with key \"$key\"" }

        val article = changeTags(key, req)

        return contentService.fullArticledResponseFrom(article, lang)
    }

    suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest, lang: Language): FullArticleResponse {
        logger.debug { "Changing visibility of article with key \"$key\"" }

        val article = changeVisibility(key, req)

        return contentService.fullArticledResponseFrom(article, lang)
    }

    private suspend fun getUniqueKey(baseKey: String, id: ObjectId? = null): String {
        val existingArticle = contentService.findByKeyOrNull(baseKey) ?: return baseKey

        return if (id != existingArticle.id) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey
    }

}
