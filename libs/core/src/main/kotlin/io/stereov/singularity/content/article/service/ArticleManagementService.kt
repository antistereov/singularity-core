package io.stereov.singularity.content.article.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.request.UpdateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.exception.*
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.request.UpdateOwnerRequest
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.exception.*
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.mapper.InvitationMapper
import io.stereov.singularity.content.invitation.model.InvitationToken
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.util.toSlug
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.translate.service.TranslateService
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = true)
class ArticleManagementService(
    override val contentService: ArticleService,
    override val authorizationService: AuthorizationService,
    override val invitationService: InvitationService,
    override val translateService: TranslateService,
    override val userService: UserService,
    override val principalMapper: PrincipalMapper,
    private val fileStorage: FileStorage,
    private val articleMapper: ArticleMapper,
    private val imageStore: ImageStore,
    override val groupService: GroupService,
    override val invitationMapper: InvitationMapper
) : ContentManagementService<Article>() {

    override val logger = logger {}
    override val contentType = Article.CONTENT_TYPE

    suspend fun create(
        req: CreateArticleRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<FullArticleResponse, CreateArticleException> = coroutineBinding {
        logger.debug { "Creating article with title ${req.title}" }

        if (req.title.isBlank()) {
            Err(CreateArticleException.InvalidRequest(
                "Title cannot be blank"
            )).bind()
        }

        val key = getUniqueKey(req.title.toSlug(), null)
            .mapError { CreateArticleException.from(it) }
            .bind()

        val article = createArticle(req, key, authenticationOutcome.principalId)
            .bind()

        val savedArticle = contentService.save(article)
            .mapError { ex -> CreateArticleException.Database("Failed to save article: ${ex.message}", ex) }
            .bind()

        articleMapper.createFullResponse(savedArticle, authenticationOutcome, locale, authenticationOutcome.principalId)
            .mapError { ex -> CreateArticleException.ResponseMapping("Failed to create response from article: ${ex.message}", ex) }
            .bind()
    }

    private fun createArticle(
        req: CreateArticleRequest,
        key: String,
        ownerId: ObjectId
    ): Result<Article, CreateArticleException> = binding {
        val translations = mutableMapOf(req.locale to ArticleTranslation(req.title, req.summary, req.content))
        val path = contentService.getUri(key)
            .mapError { ex -> CreateArticleException.InvalidRequest("Failed to create URI: ${ex.message}", ex) }
            .bind()
            .path

        Article(
            _id = null,
            key = key,
            createdAt = Instant.now(),
            publishedAt = null,
            updatedAt = Instant.now(),
            path = path,
            state = ArticleState.DRAFT,
            colors = ArticleColors(),
            imageKey = null,
            trusted = false,
            access = ContentAccessDetails(ownerId),
            translations = translations,
        )
    }

    override suspend fun setTrustedState(
        key: String,
        authenticationOutcome: AuthenticationOutcome,
        trusted: Boolean,
        locale: Locale?
    ): Result<FullArticleResponse, SetContentTrustedStateException> = coroutineBinding{
        val article = doSetTrustedState(key, trusted).bind()

        articleMapper.createFullResponse(article, authenticationOutcome, locale)
            .mapError { ex -> SetContentTrustedStateException.ResponseMapping("Failed to map response: ${ex.message}", ex) }
            .bind()
    }

    override suspend fun inviteUser(
        key: String,
        req: InviteUserToContentRequest,
        inviter: User,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<ExtendedContentAccessDetailsResponse, InviteUserException> = coroutineBinding {
        logger.debug { "Inviting user with email \"${req.email}\" to role ${req.role} on article with key \"$key\"" }

        val article = contentService.findByKey(key)
            .mapError { ex ->  when (ex) {
                is FindDocumentByKeyException.NotFound -> InviteUserException.ContentNotFound("Failed to find article with key $key: ${ex.message}")
                is FindDocumentByKeyException.Database -> InviteUserException.Database("Failed to find article with key $key: ${ex.message}")
            } }
            .bind()

        val title = translateService.translate(article, locale)
            .mapError { ex -> InviteUserException.Database("Failed to translate article: ${ex.message}", ex) }
            .bind()
            .translation.title

        val content = doInviteUser(key, req, inviter, title, contentService.getUri(key).toString(), authenticationOutcome, locale).bind()

        extendedContentAccessDetails(content, authenticationOutcome)
            .mapError { InviteUserException.from(it) }
            .bind()
    }

    override suspend fun acceptInvitation(
        token: InvitationToken,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<FullArticleResponse, AcceptContentInvitationException> = coroutineBinding {
        logger.debug { "Accepting invitation" }

        val article = doAcceptInvitation(token)
            .bind()

        articleMapper.createFullResponse(article, authenticationOutcome, locale)
            .mapError { ex -> AcceptContentInvitationException.ResponseMapping("Failed to map article to response: ${ex.message}", ex) }
            .bind()
    }

    suspend fun updateArticle(
        key: String,
        req: UpdateArticleRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<FullArticleResponse, UpdateArticleException> = coroutineBinding {
        logger.debug { "Changing header of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.EDITOR)
            .mapError { ex -> UpdateArticleException.from(ex) }
            .bind()

        article.updateTranslation(req)

        val articleId = article.id
            .mapError { ex -> UpdateArticleException.InvalidDocument("Failed to extract ID from article: ${ex.message}", ex) }
            .bind()

        val uniqueKey = translateService
            .translate(article, translateService.defaultLocale)
            .mapError { ex -> UpdateArticleException.NoTranslations("Failed to translate article: ${ex.message}", ex) }
            .bind()
            .translation.title
            .toSlug()
            .let { getUniqueKey(it, articleId) }
            .mapError { ex -> UpdateArticleException.from(ex) }
            .bind()

        article.key = uniqueKey
        article.path = contentService.getUri(uniqueKey)
            .mapError { ex -> UpdateArticleException.InvalidDocument("Failed to create URI: ${ex.message}", ex) }
            .bind()
            .path
        req.colors?.let { colors ->
            colors.text?.let { article.colors.text = colors.text }
            colors.background?.let { article.colors.background = colors.background }
        }
        req.tags?.let { article.tags = it }

        val updatedArticle = contentService.save(article)
            .mapError { ex -> UpdateArticleException.Database("Failed to save updated article: ${ex.message}", ex) }
            .bind()

        articleMapper.createFullResponse(updatedArticle, authenticationOutcome, locale)
            .mapError { ex -> UpdateArticleException.ResponseMapping("Failed to map article to response: ${ex.message}", ex) }
            .bind()
    }

    private fun Article.updateTranslation(
        req: UpdateArticleRequest
    ): Result<Article, UpdateArticleException> = binding {
        if (req.title == "") {
            Err(UpdateArticleException.InvalidRequest("Title cannot be blank"))
                .bind()
        }

        val translation = translations[req.locale]

        if (translation  != null) {
            req.title?.let { translation.title = it }
            req.summary?.let { translation.summary = it }
            req.content?.let { translation.content = it }
        } else {
            if (req.title == null)
                Err(UpdateArticleException.InvalidRequest("Failed to create translation ${req.locale}: " +
                        "when adding a new translation, the article title must not be null")).bind()

            translations[req.locale] = ArticleTranslation(
                req.title,
                req.summary ?: "",
                req.content ?: ""
            )
        }

        this@updateTranslation
    }

    suspend fun changeImage(
        key: String,
        file: FilePart,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<FullArticleResponse, ChangeArticleImageException> = coroutineBinding {
        logger.debug { "Changing image of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, authenticationOutcome,ContentAccessRole.EDITOR)
            .mapError { ex -> ChangeArticleImageException.from(ex) }
            .bind()

        val currentImage = article.imageKey

        if (currentImage != null) {
            fileStorage.remove(currentImage)
                .onFailure { ex ->
                    logger.debug(ex) { "Failed to remove old image" }
                }
        }

        val imageKey = contentService.getUri(key)
            .mapError { ex -> ChangeArticleImageException.InvalidDocument("Failed to create URI: ${ex.message}", ex) }
            .bind()
            .path.removePrefix("/") + "/" + article.key

        val image = imageStore.upload(authenticationOutcome, file, imageKey,  true)
            .mapError { ex -> ChangeArticleImageException.File("Failed to upload image: ${ex.message}", ex) }
            .bind()

        article.imageKey = image.key

        val updatedArticle = contentService.save(article)
            .mapError { ex -> ChangeArticleImageException.Database("Failed to save updated article: ${ex.message}", ex) }
            .bind()

        articleMapper.createFullResponse(updatedArticle, authenticationOutcome, locale)
            .mapError { ex -> ChangeArticleImageException.ResponseMapping("Failed to map article to response: ${ex.message}", ex) }
            .bind()
    }

    suspend fun changeState(
        key: String,
        req: ChangeArticleStateRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<FullArticleResponse, ChangeArticleStateException> = coroutineBinding {
        logger.debug { "Changing satte of article with key \"$key\"" }

        val article = contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.EDITOR)
            .mapError { ex -> ChangeArticleStateException.from(ex) }
            .bind()

        article.state = req.state

        val updatedArticle = contentService.save(article)
            .mapError { ex -> ChangeArticleStateException.Database("Failed to save updated article: ${ex.message}", ex) }
            .bind()

        articleMapper.createFullResponse(updatedArticle, authenticationOutcome, locale)
            .mapError { ex -> ChangeArticleStateException.ResponseMapping("Failed to map article to response: ${ex.message}", ex) }
            .bind()
    }

    override suspend fun updateAccess(
        key: String,
        req: UpdateContentAccessRequest,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<FullArticleResponse, UpdateContentAccessException> = coroutineBinding {
        logger.debug { "Changing visibility of article with key \"$key\"" }

        val article = doUpdateAccess(key, req, authenticationOutcome).bind()
        articleMapper.createFullResponse(article, authenticationOutcome, locale)
            .mapError { ex -> UpdateContentAccessException.ResponseMapping("Failed to create response: ${ex.message}", ex) }
            .bind()
    }

    private suspend fun getUniqueKey(baseKey: String, id: ObjectId?): Result<String, GetUniqueArticleKeyException> {
        return contentService.findByKey(baseKey)
            .flatMapEither(
                success = { article ->
                    article.id
                        .map { existingArticleId ->
                            if (id != existingArticleId) {
                                "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
                            } else baseKey
                        }
                        .mapError { ex ->
                            GetUniqueArticleKeyException.InvalidDocument(
                                "Failed to extract ID from article: ${ex.message}",
                                ex
                            )
                        }
                },
                failure = { ex ->
                    when (ex) {
                        is FindDocumentByKeyException.NotFound -> Ok(baseKey)
                        is FindDocumentByKeyException.Database -> Err(GetUniqueArticleKeyException.Database("Failed to find article: ${ex.message}", ex))
                    }
                }
            )
    }

    override suspend fun updateOwner(
        key: String,
        req: UpdateOwnerRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<FullArticleResponse, UpdateContentOwnerException> = coroutineBinding {
        val article = doUpdateOwner(key, req, authenticationOutcome)
            .bind()

        articleMapper.createFullResponse(article, authenticationOutcome, locale)
            .mapError { ex -> UpdateContentOwnerException.ResponseMapping("Failed to map response: ${ex.message}", ex) }
            .bind()
    }

    override suspend fun deleteByKey(
        key: String,
        authenticationOutcome: AuthenticationOutcome
    ): Result<Unit, DeleteContentByKeyException> = coroutineBinding {
        val article = contentService.findByKey(key)
            .mapError { when (it) {
                is FindDocumentByKeyException.NotFound -> DeleteContentByKeyException.ContentNotFound(it.message)
                is FindDocumentByKeyException.Database -> DeleteContentByKeyException.Database(it.message)
            } }
            .bind()

        article.imageKey?.let {
            fileStorage.remove(it)
                .onFailure { ex -> logger.error(ex) { "Failed to remove image with key $it" } }
        }
        super.deleteByKey(key, authenticationOutcome).bind()
    }

}
