package io.stereov.singularity.content.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.dto.*
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentAccessSubject
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.invitation.exception.model.InvalidInvitationException
import io.stereov.singularity.content.invitation.model.InvitationDocument
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import java.util.*

interface ContentManagementService<T: ContentDocument<T>> {

    val userService: UserService
    val contentService: ContentService<T>
    val authorizationService: AuthorizationService
    val invitationService: InvitationService
    val acceptPath: String
    val userMapper: UserMapper

    val logger: KLogger

    suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest): T {
        logger.debug { "Changing visibility of key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.ADMIN)

        content.access.update(req)

        return contentService.save(content)
    }

    suspend fun changeTags(key: String, req: ChangeContentTagsRequest): T {
        logger.debug { "Changing tags of key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.EDITOR)
        content.tags = req.tags

        return contentService.save(content)
    }

    suspend fun inviteUser(
        key: String,
        req: InviteUserToContentRequest,
        invitedTo: String,
        locale: Locale?,
    ): ExtendedContentAccessDetailsResponse {
        logger.debug { "Inviting user with email \"${req.email}\" to content with key \"$key\" as ${req.role}" }

        val user = authorizationService.getUser()
        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.ADMIN)
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

    suspend fun acceptInvitation(req: AcceptInvitationToContentRequest): T {
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

    suspend fun delete(key: String) {
        logger.debug { "Deleting content with key \"$key\"" }

        val content = contentService.findAuthorizedByKey(key, ContentAccessRole.ADMIN)

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

        content.access.users.admin.forEach { id ->
            val foundUser = userService.findByIdOrNull(ObjectId(id))

            if (foundUser != null) {
                users.add(UserContentAccessDetails(userMapper.toOverview(foundUser), ContentAccessRole.ADMIN))
            } else {
                content.access.users.admin.remove(id)
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
