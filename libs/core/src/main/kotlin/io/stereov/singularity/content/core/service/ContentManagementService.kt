package io.stereov.singularity.content.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.ChangeContentVisibilityRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.dto.response.UserContentAccessDetails
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentAccessSubject
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.invitation.exception.model.InvalidInvitationException
import io.stereov.singularity.content.invitation.model.InvitationDocument
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import java.util.*

abstract class ContentManagementService<T: ContentDocument<T>>() {

    abstract val userService: UserService
    abstract val contentService: ContentService<T>
    abstract val authorizationService: AuthorizationService
    abstract val invitationService: InvitationService
    abstract val userMapper: UserMapper
    abstract val contentKey: String
    abstract val translateService: TranslateService

    abstract val logger: KLogger

    abstract suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest, locale: Locale?): ContentResponse<T>
    protected suspend fun doChangeVisibility(key: String, req: ChangeContentVisibilityRequest): T {
        logger.debug { "Changing visibility of key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.MAINTAINER)

        content.access.update(req)

        return contentService.save(content)
    }

    abstract suspend fun inviteUser(key: String, req: InviteUserToContentRequest, locale: Locale?): ExtendedContentAccessDetailsResponse
    protected suspend fun doInviteUser(
        key: String,
        req: InviteUserToContentRequest,
        title: String,
        url: String,
        locale: Locale?,
    ): ExtendedContentAccessDetailsResponse {
        logger.debug { "Inviting user with email \"${req.email}\" to content with key \"$key\" as ${req.role}" }

        val actualLocale = locale ?: translateService.defaultLocale
        val acceptPath = "/api/content/invitations/$contentKey/accept"

        val inviteToRole = translateService.translateResourceKey(
            TranslateKey(
                "role.${req.role.toString().lowercase()}"
            ), "i18n/content/invitation", actualLocale)
        val resource = translateService.translateResourceKey(TranslateKey("resource.${contentKey}"), "i18n/content/article", actualLocale)
        val user = authorizationService.getUser()
        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.MAINTAINER)
        val ref = "<a href=\"$url\" style=\"color: black;\">$title</a>"
        val invitedTo = "$inviteToRole $resource $ref"

         val invitation = invitationService.invite(
            email = req.email,
            inviterName = user.sensitive.name,
            invitedTo = invitedTo,
            acceptPath = acceptPath,
            claims = mapOf("key" to key, "role" to req.role),
            locale = locale
        )

        content.addInvitation(invitation)

        return extendedContentAccessDetails(contentService.save(content))
    }

    abstract suspend fun acceptInvitation(req: AcceptInvitationToContentRequest, locale: Locale?): ContentResponse<T>
    protected suspend fun doAcceptInvitation(req: AcceptInvitationToContentRequest): T {
        logger.debug { "Accepting invitation" }

        val invitation = invitationService.accept(req.token)

        val key = invitation.sensitive.claims["key"] as? String
            ?: throw InvalidInvitationException("No key claim found in invitation")
        val roleString = invitation.sensitive.claims["role"] as? String
            ?: throw InvalidInvitationException("No role claim found in invitation")
        val role = try {
            ContentAccessRole.fromString(roleString)
        } catch (_: Exception) {
            throw InvalidInvitationException(" Role claim found in invitation is invalid: $roleString")
        }

        val email = invitation.sensitive.email

        val user = userService.findByEmailOrNull(email)
            ?: throw InvalidInvitationException("No user exists with email from invitation")
        val content = contentService.findByKeyOrNull(key)
            ?: throw InvalidInvitationException("No content matched key from invitation")

        content.share(ContentAccessSubject.USER, user.id.toString(), role)
        content.removeInvitation(invitation.id)

        return contentService.save(content)
    }

    abstract suspend fun setTrustedState(key: String, trusted: Boolean, locale: Locale?): ContentResponse<T>
    protected suspend fun doSetTrustedState(key: String, trusted: Boolean): T {
        logger.debug { "Setting trusted state" }
        authorizationService.requireRole(Role.ADMIN)

        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)

        content.trusted = trusted
        return contentService.save(content)
    }

    suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting content with key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.MAINTAINER)

        contentService.deleteById(content.id)
    }

    suspend fun extendedContentAccessDetails(key: String): ExtendedContentAccessDetailsResponse {
        logger.debug { "Fetching extended content access details for article with key \"$key\"" }

        val content = contentService.findByKey(key)

        return extendedContentAccessDetails(content)
    }

    suspend fun extendedContentAccessDetails(content: T): ExtendedContentAccessDetailsResponse {
        logger.debug { "Fetching extended content access details for article with key \"${content.key}\"" }

        val invitations = mutableListOf<InvitationDocument>()

        content.access.invitations.forEach { invitationId ->
            val foundInvite = invitationService.findByIdOrNull(invitationId)

            if (foundInvite != null) {
                invitations.add(foundInvite)
            }
        }
        content.access.invitations.removeIf { invitations.none { invite -> invite.id == it } }

        val users = mutableListOf<UserContentAccessDetails>()

        content.access.users.maintainer.forEach { id ->
            val foundUser = userService.findByIdOrNull(ObjectId(id))

            if (foundUser != null) {
                users.add(UserContentAccessDetails(userMapper.toOverview(foundUser), ContentAccessRole.MAINTAINER))
            } else {
                content.access.users.maintainer.remove(id)
            }
        }

        content.access.users.editor.forEach { id ->
            val foundUser = userService.findByIdOrNull(ObjectId(id))

            if (foundUser != null) {
                users.add(UserContentAccessDetails(userMapper.toOverview(foundUser), ContentAccessRole.EDITOR))
            } else {
                content.access.users.editor.remove(id)
            }
        }

        content.access.users.viewer.forEach { id ->
            val foundUser = userService.findByIdOrNull(ObjectId(id))

            if (foundUser != null) {
                users.add(UserContentAccessDetails(userMapper.toOverview(foundUser), ContentAccessRole.VIEWER))
            } else {
                content.access.users.viewer.remove(id)
            }
        }

        val updatedContent = contentService.save(content)

        return ExtendedContentAccessDetailsResponse.create(updatedContent.access, invitations, users)
    }
}
