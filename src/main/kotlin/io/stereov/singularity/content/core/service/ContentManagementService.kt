package io.stereov.singularity.content.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.request.UpdateOwnerRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.dto.response.UserContentAccessDetails
import io.stereov.singularity.content.core.exception.*
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentAccessSubject
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.invitation.mapper.InvitationMapper
import io.stereov.singularity.content.invitation.model.Invitation
import io.stereov.singularity.content.invitation.model.InvitationToken
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.database.core.exception.DocumentException
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.principal.core.exception.FindUserByEmailException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import org.bson.types.ObjectId
import java.util.*

abstract class ContentManagementService<T: ContentDocument<T>>() {

    abstract val userService: UserService
    abstract val contentService: ContentService<T>
    abstract val authorizationService: AuthorizationService
    abstract val invitationService: InvitationService
    abstract val principalMapper: PrincipalMapper
    abstract val translateService: TranslateService
    abstract val groupService: GroupService
    abstract val invitationMapper: InvitationMapper

    abstract val contentType: String
    abstract val logger: KLogger

    abstract suspend fun updateAccess(
        key: String,
        req: UpdateContentAccessRequest,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<ContentResponse<T>, UpdateContentAccessException>
    protected suspend fun doUpdateAccess(
        key: String,
        req: UpdateContentAccessRequest,
        authenticationOutcome: AuthenticationOutcome
    ): Result<T, UpdateContentAccessException> = coroutineBinding {
        logger.debug { "Updating access of key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .mapError { UpdateContentAccessException.from(it) }
            .bind()

        req.sharedGroups.forEach { (group, _) ->
            val groupExists = groupService.existsByKey(group)
                .mapError { ex -> UpdateContentAccessException.Database("Failed to check existence of group with key $group: ${ex.message}", ex) }
                .bind()
            if (!groupExists) {
                Err(UpdateContentAccessException.GroupNotFound("No group with key $group found."))
                    .bind()
            }
        }
        req.sharedUsers.forEach { (user, _) ->
            val userExists = userService.existsById(user)
                .mapError { ex -> UpdateContentAccessException.Database("Failed to check existence of user with ID $user: ${ex.message}", ex) }
                .bind()

            if (!userExists) {
                Err(UpdateContentAccessException.UserNotFound("No user with ID $user found."))
                    .bind()
            }
        }

        content.access.update(req)

        contentService.save(content)
            .mapError { ex -> UpdateContentAccessException.Database("Failed to save updated content: ${ex.message}", ex) }
            .bind()
    }

    abstract suspend fun inviteUser(
        key: String,
        req: InviteUserToContentRequest,
        inviter: User,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<ExtendedContentAccessDetailsResponse, InviteUserException>
    protected suspend fun doInviteUser(
        key: String,
        req: InviteUserToContentRequest,
        inviter: User,
        title: String,
        url: String,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?,
    ): Result<T, InviteUserException> = coroutineBinding {
        logger.debug { "Inviting user with email \"${req.email}\" to content with key \"$key\" as ${req.role}" }

        val actualLocale = locale ?: translateService.defaultLocale

        val inviteToRole = translateService.translateResourceKey(
            TranslateKey(
                "role.${req.role.toString().lowercase()}"
            ), "i18n/content/invitation", actualLocale)
        val resource = translateService.translateResourceKey(TranslateKey("resource.${contentType}"), "i18n/content/article", actualLocale)
        val content = contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .mapError { InviteUserException.from(it) }
            .bind()

        val ref = "<a href=\"$url\" style=\"color: black;\">$title</a>"
        val invitedTo = "$inviteToRole $resource $ref"

         val invitation = invitationService.invite(
             contentType = contentType,
             contentKey = key,
             email = req.email,
             inviterName = inviter.sensitive.name,
             invitedTo = invitedTo,
             claims = mapOf("key" to key, "role" to req.role),
             locale = locale
         )
            .mapError { InviteUserException.from(it) }
             .bind()

        val invitationId = invitation.id
            .mapError { ex -> InviteUserException.Database("Failed to extract ID from invitation: ${ex.message}", ex) }
            .bind()

        content.addInvitation(invitationId)

        contentService.save(content)
            .mapError { ex -> InviteUserException.Database("Failed to save updated document to database: ${ex.message}", ex) }
            .bind()
    }

    abstract suspend fun acceptInvitation(token: InvitationToken, authenticationOutcome: AuthenticationOutcome, locale: Locale?): Result<ContentResponse<T>, AcceptContentInvitationException>
    protected suspend fun doAcceptInvitation(token: InvitationToken): Result<T, AcceptContentInvitationException> = coroutineBinding {
        logger.debug { "Accepting invitation" }

        val invitation = invitationService.accept(token)
            .mapError { ex -> AcceptContentInvitationException.from(ex) }
            .bind()

        val key = (invitation.sensitive.claims["key"] as? String)
            .toResultOr { AcceptContentInvitationException.InvalidInvitation("No key claim found in invitation") }
            .bind()
        val roleString = (invitation.sensitive.claims["role"] as? String)
            .toResultOr { AcceptContentInvitationException.InvalidInvitation("No role claim found in invitation") }
            .bind()
        val invitedContentType = (invitation.sensitive.claims["contentType"] as? String)
            .toResultOr { AcceptContentInvitationException.InvalidInvitation("No content type claim found in invitation") }
            .bind()

        if (invitedContentType != contentType) {
            Err(AcceptContentInvitationException.InvalidInvitation("Invitation does not belong to requested content type"))
                .bind()
        }

        val role = ContentAccessRole.fromString(roleString)
            .mapError { ex -> AcceptContentInvitationException.InvalidInvitation("Role claim found in invitation is invalid: $roleString", ex) }
            .bind()

        val email = invitation.sensitive.email

        val user = userService.findByEmail(email)
            .mapError { ex -> when (ex) {
                is FindUserByEmailException.UserNotFound -> AcceptContentInvitationException.UserNotFound("No user with email $email found")
                else -> AcceptContentInvitationException.Database("Failed to find user with email $email: ${ex.message}", ex) }
            }
            .bind()
        val content = contentService.findByKey(key)
            .mapError { ex -> when (ex) {
                is FindDocumentByKeyException.NotFound -> AcceptContentInvitationException.ContentNotFound("No content with key $key found")
                else -> AcceptContentInvitationException.Database("Failed to find content with key $key: ${ex.message}", ex)
            }}
            .bind()

        content.share(ContentAccessSubject.USER, user.id.toString(), role)
        content.removeInvitation(token.invitationId)

        contentService.save(content)
            .mapError { ex -> AcceptContentInvitationException.Database("Failed to save content after accepting invitation: ${ex.message}", ex) }
            .bind()
    }

    abstract suspend fun setTrustedState(
        key: String,
        authenticationOutcome: AuthenticationOutcome,
        trusted: Boolean,
        locale: Locale?
    ): Result<ContentResponse<T>, SetContentTrustedStateException>
    protected suspend fun doSetTrustedState(
        key: String,
        trusted: Boolean,
    ): Result<T, SetContentTrustedStateException> = coroutineBinding {
        logger.debug { "Setting trusted state" }

        val content = contentService.findByKey(key)
            .mapError { ex -> SetContentTrustedStateException.from(ex) }
            .bind()

        content.trusted = trusted

        contentService.save(content)
            .mapError { ex -> SetContentTrustedStateException.Database("Failed to save updated content: ${ex.message}", ex) }
            .bind()
    }

    abstract suspend fun updateOwner(
        key: String,
        req: UpdateOwnerRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<ContentResponse<T>, UpdateContentOwnerException>
    protected suspend fun doUpdateOwner(
        key: String,
        req: UpdateOwnerRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated
    ): Result<T, UpdateContentOwnerException> = coroutineBinding {
        logger.debug { "Updating owner of ${contentService.collectionClazz.simpleName} with key $key to ${req.newOwnerId}" }

        val content = contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .mapError { UpdateContentOwnerException.from(it) }
            .bind()


        if (content.access.ownerId != authenticationOutcome.principalId) {
            Err(UpdateContentOwnerException.NotAuthorized("Only the owner can update the owner of this resource"))
                .bind()
        }
        val newOwnerId = ObjectId(req.newOwnerId)

        val userExists = userService.existsById(newOwnerId)
            .mapError { ex -> UpdateContentOwnerException.Database("Failed to check existence of user with ID ${req.newOwnerId}: ${ex.message}", ex) }
            .bind()
        if (!userExists) {
            Err(UpdateContentOwnerException.UserNotFound("No user with ID ${req.newOwnerId} found"))
                .bind()
        }

        content.access.share(ContentAccessSubject.USER, content.access.ownerId.toString(), ContentAccessRole.MAINTAINER)
        content.access.ownerId = newOwnerId

        contentService.save(content)
            .mapError { ex -> UpdateContentOwnerException.Database("Failed to save updated content to database: ${ex.message}", ex) }
            .bind()
    }

    open suspend fun deleteByKey(
        key: String,
        authenticationOutcome: AuthenticationOutcome
    ): Result<Unit, DeleteContentByKeyException> = coroutineBinding {
        logger.debug { "Deleting content with key \"$key\"" }

        contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .mapError { ex -> DeleteContentByKeyException.from(ex) }
            .bind()

        contentService.deleteByKey(key)
            .mapError { ex -> DeleteContentByKeyException.Database("Failed to delete content with key \"$key\": ${ex.message}", ex) }
            .bind()
    }

    suspend fun deleteInvitation(
        contentKey: String,
        invitationId: ObjectId,
        authenticationOutcome: AuthenticationOutcome
    ): Result<T, DeleteContentInvitationException> = coroutineBinding {
        val content = contentService.findAuthorizedByKey(contentKey, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .mapError { ex -> DeleteContentInvitationException.from(ex) }
            .bind()

        content.access.invitations.remove(invitationId)

        contentService.save(content)
            .mapError { ex -> DeleteContentInvitationException.Database("Failed to delete invitation from content with key \"$contentKey\": ${ex.message}", ex) }
            .bind()
    }

    suspend fun extendedContentAccessDetails(
        content: T,
        authenticationOutcome: AuthenticationOutcome
    ): Result<ExtendedContentAccessDetailsResponse, GenerateExtendedContentAccessDetailsException> = coroutineBinding {
        logger.debug { "Fetching extended content access details for article with key \"${content.key}\"" }

        val invitations = mutableListOf<Invitation>()
        val invitationIds = mutableListOf<ObjectId>()

        content.access.invitations.forEach { invitationId ->
            invitationService.findById(invitationId)
                .map { invitation ->
                    invitations.add(invitation)
                    invitationIds.add(invitationId)
                }
                .recoverIf(
                    { it is FindEncryptedDocumentByIdException.NotFound },
                    { false }
                )
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to fetch invitation with ID $invitationId: ${ex.message}", ex) }
                .bind()
        }
        content.access.invitations.removeIf { invitationIds.none { invite -> invite == it } }

        val users = mutableListOf<UserContentAccessDetails>()

        content.access.users.maintainer.forEach { id ->
            val objectId = runCatching { ObjectId(id) }
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.InvalidDocument("Failed to parse user ID $id: ${ex.message}", ex) }
                .bind()

            userService.findById(objectId)
                .map { user ->
                    val overview = principalMapper.toOverview(user, authenticationOutcome)
                        .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to map user to overview: ${ex.message}", ex) }
                        .bind()
                    users.add(UserContentAccessDetails(overview, ContentAccessRole.MAINTAINER))
                }
                .recoverIf(
                    { it is FindEncryptedDocumentByIdException.NotFound },
                    { content.access.users.maintainer.remove(id) }
                )
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to fetch user with ID $id: ${ex.message}", ex) }
                .bind()
        }

        content.access.users.editor.forEach { id ->
            val objectId = runCatching { ObjectId(id) }
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.InvalidDocument("Failed to parse user ID $id: ${ex.message}", ex) }
                .bind()

            userService.findById(objectId)
                .map { user ->
                    val overview = principalMapper.toOverview(user, authenticationOutcome)
                        .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to map user to overview: ${ex.message}", ex) }
                        .bind()
                    users.add(UserContentAccessDetails(overview, ContentAccessRole.EDITOR))
                }
                .recoverIf(
                    { it is FindEncryptedDocumentByIdException.NotFound },
                    { content.access.users.editor.remove(id) }
                )
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to fetch user with ID $id: ${ex.message}", ex) }
                .bind()
        }

        content.access.users.viewer.forEach { id ->
            val objectId = runCatching { ObjectId(id) }
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.InvalidDocument("Failed to parse user ID $id: ${ex.message}", ex) }
                .bind()

            userService.findById(objectId)
                .map { user ->
                    val overview = principalMapper.toOverview(user, authenticationOutcome)
                        .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to map user to overview: ${ex.message}", ex) }
                        .bind()
                    users.add(UserContentAccessDetails(overview, ContentAccessRole.VIEWER))
                }
                .recoverIf(
                    { it is FindEncryptedDocumentByIdException.NotFound },
                    { content.access.users.viewer.remove(id) }
                )
                .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to fetch user with ID $id: ${ex.message}", ex) }
                .bind()
        }

        val updatedContent = contentService.save(content)
            .mapError { ex -> GenerateExtendedContentAccessDetailsException.Database("Failed to save updated content to database: ${ex.message}", ex) }
            .bind()

        val invitationResponses = invitations.map {
            invitationMapper.toInvitationResponse(it)
                .mapError {ex ->  when (ex) {
                    is DocumentException.Invalid -> GenerateExtendedContentAccessDetailsException.InvalidDocument("Failed to map invitation to response: ${ex.message}", ex)
                } }
                .bind()
        }

        ExtendedContentAccessDetailsResponse.create(updatedContent.access, invitationResponses, users)
    }

    /**
     * Fetches the extended access details for content identified by the provided key.
     * This includes fetching and processing user access information, invitations, and updating the access details in the database.
     *
     * @param key The unique identifier of the content for which the extended access details are being retrieved.
     * @param authenticationOutcome The result of the authentication process for the requesting user.
     * @return A [Result] containing [ExtendedContentAccessDetailsResponse] with the extended access details if successful,
     *   or [GenerateExtendedContentAccessDetailsException] in case of failure.
     */
    suspend fun extendedContentAccessDetails(
        key: String,
        authenticationOutcome: AuthenticationOutcome
    ): Result<ExtendedContentAccessDetailsResponse, GenerateExtendedContentAccessDetailsException> = coroutineBinding {
        logger.debug { "Fetching extended content access details for article with key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, authenticationOutcome, ContentAccessRole.MAINTAINER)
            .mapError { GenerateExtendedContentAccessDetailsException.from(it) }
            .bind()

        extendedContentAccessDetails(content, authenticationOutcome).bind()
    }
}
