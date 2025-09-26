package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.request.UpdateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.exception.model.InvalidArticleRequestException
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.ChangeContentVisibilityRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.global.util.toSlug
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.util.*

@Service
class ArticleManagementService(
    override val contentService: ArticleService,
    override val authorizationService: AuthorizationService,
    override val invitationService: InvitationService,
    override val translateService: TranslateService,
    override val userService: UserService,
    override val userMapper: UserMapper,
    private val fileStorage: FileStorage,
    private val uiProperties: UiProperties,
    private val articleMapper: ArticleMapper,
    private val imageStore: ImageStore,
) : ContentManagementService<Article>() {

    override val logger = KotlinLogging.logger {}
    override val contentKey = "articles"

    suspend fun create(req: CreateArticleRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Creating article with title ${req.title}" }

        contentService.requireEditorGroupMembership()
        val user = authorizationService.getUser()

        val key = getUniqueKey(req.title.toSlug())

        val article = articleMapper.createArticle(req, key, user.id)
        val savedArticle = contentService.save(article)

        return articleMapper.createFullResponse(savedArticle, locale,user)
    }

    override suspend fun setTrustedState(key: String, trusted: Boolean, locale: Locale?): FullArticleResponse {
        return articleMapper.createFullResponse(doSetTrustedState(key, trusted), locale)
    }

    override suspend fun inviteUser(key: String, req: InviteUserToContentRequest, locale: Locale?): ExtendedContentAccessDetailsResponse {
        logger.debug { "Inviting user with email \"${req.email}\" to role ${req.role} on article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        val title = translateService.translate(article, locale).translation.title
        val url = "${uiProperties.baseUrl.removeSuffix("/")}/${article.path.removePrefix("/")}"

        return doInviteUser(key, req, title, url, locale)
    }

    override suspend fun acceptInvitation(req: AcceptInvitationToContentRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Accepting invitation" }

        val article = doAcceptInvitation(req)
        return articleMapper.createFullResponse(article, locale)
    }

    suspend fun updateArticle(key: String, req: UpdateArticleRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing header of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        article.updateTranslation(req, locale)

        val uniqueKey = translateService
            .translate(article, translateService.defaultLocale)
            .translation.title

        article.key = uniqueKey
        article.path = "${Article.basePath}/$uniqueKey"
        req.colors?.let { colors ->
            colors.text?.let { article.colors.text = colors.text }
            colors.background?.let { article.colors.background = colors.background }
        }

        val updatedArticle = contentService.save(article)

        return articleMapper.createFullResponse(updatedArticle, locale)
    }

    private fun Article.updateTranslation(req: UpdateArticleRequest, locale: Locale?) {
        if (req.title == "") throw InvalidArticleRequestException("The title cannot be an empty string")

        val translation = this.translations[req.locale]
        if (translation  != null) {
            req.title?.let { translation.title = it }
            req.summary?.let { translation.summary = it }
            req.content?.let { translation.content = it }
        } else {
            if (req.title == null)
                throw InvalidArticleRequestException("Failed to create translation $locale: " +
                        "when adding a new translation, the article title must not be null")

            this.translations[req.locale] = ArticleTranslation(
                req.title,
                req.summary ?: "",
                req.content ?: ""
            )
        }
    }

    suspend fun changeImage(key: String, file: FilePart, locale: Locale?, exchange: ServerWebExchange): FullArticleResponse {
        logger.debug { "Changing image of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        val userId = authorizationService.getUserId()

        val currentImage = article.imageKey

        if (currentImage != null) {
            fileStorage.remove(currentImage)
        }
        val imageKey = Article.basePath.removePrefix("/") + "/" + article.key

        val image = imageStore.upload(userId, file, imageKey,  true, exchange,)
        article.imageKey = image.key

        val updatedArticle = contentService.save(article)
        return articleMapper.createFullResponse(updatedArticle, locale)
    }

    suspend fun changeState(key: String, req: ChangeArticleStateRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing satte of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        article.state = req.state

        val updatedArticle = contentService.save(article)

        return articleMapper.createFullResponse(updatedArticle, locale)
    }

    override suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest, locale: Locale?): FullArticleResponse {
        logger.debug { "Changing visibility of article with key \"$key\"" }

        val article = doChangeVisibility(key, req)
        return articleMapper.createFullResponse(article, locale)
    }

    private suspend fun getUniqueKey(baseKey: String, id: ObjectId? = null): String {
        val existingArticle = contentService.findByKeyOrNull(baseKey) ?: return baseKey

        return if (id != existingArticle.id) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey
    }

}
