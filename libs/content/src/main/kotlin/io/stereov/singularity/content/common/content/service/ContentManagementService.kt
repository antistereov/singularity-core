package io.stereov.singularity.content.common.content.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.content.common.content.dto.*
import io.stereov.singularity.content.common.content.exception.model.InvalidInvitationException
import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.content.common.content.model.ContentAccessSubject
import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.core.auth.exception.model.NotAuthorizedException
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.invitation.model.InvitationDocument
import io.stereov.singularity.core.invitation.service.InvitationService
import io.stereov.singularity.core.user.service.UserService

interface ContentManagementService<T: ContentDocument<T>> {

    val userService: UserService
    val contentService: ContentService<T>
    val authenticationService: AuthenticationService
    val invitationService: InvitationService
    val acceptPath: String

    val logger: KLogger

    suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest): T {
        logger.debug { "Changing visibility of key \"$key\"" }

        val content = validatePermissionsAndGetByKey(key, ContentAccessRole.ADMIN)

        content.access.update(req)

        return contentService.save(content)
    }

    suspend fun changeTags(key: String, req: ChangeContentTagsRequest): T {
        logger.debug { "Changing tags of key \"$key\"" }

        val content = validatePermissionsAndGetByKey(key, ContentAccessRole.EDITOR)
        content.tags = req.tags

        return contentService.save(content)
    }

    suspend fun inviteUser(
        key: String,
        req: InviteUserToContentRequest,
        inviteTo: String,
        lang: Language,
    ): ExtendedContentAccessDetailsResponse {
        logger.debug { "Inviting user with email \"${req.email}\" to content with key \"$key\" as ${req.role}" }

        val user = authenticationService.getCurrentUser()
        val content = validatePermissionsAndGetByKey(key, ContentAccessRole.ADMIN)
        val invitation = invitationService.invite(
            email = req.email,
            inviterName = user.sensitive.name,
            inviteTo = inviteTo,
            acceptPath = acceptPath,
            claims = mapOf("key" to key, "role" to req.role),
            lang = lang
        )

        content.addInvitation(invitation)

        return extendedContentAccessDetails(contentService.save(content))
    }

    suspend fun acceptInvitation(req: AcceptInvitationToContentRequest): T {
        logger.debug { "Accepting invitation" }

        val invitation = invitationService.accept(req.token)

        val key = invitation.sensitive.claims["key"] as? String
            ?: throw InvalidInvitationException("No key claim found in invitation")
        val role = invitation.sensitive.claims["role"] as? ContentAccessRole
            ?: throw InvalidInvitationException("No role claim found in invitation")
        val email = invitation.sensitive.email

        val user = userService.findByEmailOrNull(email)
            ?: throw InvalidInvitationException("No user exists with mail from invitation")
        val content = contentService.findByKeyOrNull(key)
            ?: throw InvalidInvitationException("No content matched key from invitation")

        content.share(ContentAccessSubject.USER, user.id.toString(), role)
        content.removeInvitation(invitation.id)

        return contentService.save(content)
    }

    suspend fun delete(key: String) {
        logger.debug { "Deleting content with key \"$key\"" }

        val content = validatePermissionsAndGetByKey(key, ContentAccessRole.ADMIN)

        contentService.deleteById(content.id)
    }

    suspend fun validateCurrentUserIsEditor() {
        authenticationService.validateCurrentUserIsEditor()
    }

    suspend fun validatePermissionsAndGetByKey(key: String, role: ContentAccessRole): T {
        validateCurrentUserIsEditor()

        val content = contentService.findByKey(key)
        val user = authenticationService.getCurrentUser()

        if (!content.hasAccess(user, role)) throw NotAuthorizedException("User does not have sufficient permission to perform this action. Required role: $role")

        return content
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

        val updatedContent = contentService.save(content)

        return ExtendedContentAccessDetailsResponse.create(updatedContent.access, invitations)
    }
}
