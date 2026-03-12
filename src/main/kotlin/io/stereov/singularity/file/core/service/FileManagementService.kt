package io.stereov.singularity.file.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.request.UpdateOwnerRequest
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.exception.*
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.mapper.InvitationMapper
import io.stereov.singularity.content.invitation.model.InvitationToken
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Service
import java.util.*

@Service
class FileManagementService(
    override val userService: UserService,
    override val contentService: FileMetadataService,
    override val authorizationService: AuthorizationService,
    override val invitationService: InvitationService,
    override val principalMapper: PrincipalMapper,
    override val translateService: TranslateService,
    override val groupService: GroupService,
    override val invitationMapper: InvitationMapper,
    private val fileStorage: FileStorage
) : ContentManagementService<FileMetadataDocument>() {

    override val logger = KotlinLogging.logger {}
    override val contentType = FileMetadataDocument.CONTENT_TYPE

    override suspend fun updateAccess(
        key: DocumentKey,
        req: UpdateContentAccessRequest,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<FileMetadataResponse, UpdateContentAccessException> = coroutineBinding {
        val content = doUpdateAccess(key, req, authenticationOutcome).bind()

        fileStorage.metadataResponseByKey(content.key, authenticationOutcome)
            .mapError { ex ->
                UpdateContentAccessException.ResponseMapping(
                    "Failed to map file metadata to response: ${ex.message}",
                    ex.cause
                )
            }
            .bind()
    }

    override suspend fun inviteUser(
        key: DocumentKey,
        req: InviteUserToContentRequest,
        inviter: User,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<ExtendedContentAccessDetailsResponse, InviteUserException> =
        coroutineBinding {
            val event = contentService.findByKey(key)
                .mapError { ex ->
                    when (ex) {
                        is FindDocumentByKeyException.NotFound -> InviteUserException.ContentNotFound("Failed to find file with key '$key': ${ex.message}")
                        is FindDocumentByKeyException.Database -> InviteUserException.Database("Failed to find file with key '$key': ${ex.message}")
                    }
                }
                .bind()

            val title = event.key.value

            val url = contentService.getUri(key)
                .mapError { ex ->
                    InviteUserException.Configuration(
                        "Failed to retrieve URL for file with key '$key': ${ex.message}",
                        ex
                    )
                }
                .bind()
                .toString()

            val content = doInviteUser(key, req, inviter, title, url, authenticationOutcome, locale).bind()

            extendedContentAccessDetails(content, authenticationOutcome)
                .mapError { InviteUserException.from(it) }
                .bind()
        }

    override suspend fun acceptInvitation(
        token: InvitationToken,
        authenticationOutcome: AuthenticationOutcome,
        locale: Locale?
    ): Result<FileMetadataResponse, AcceptContentInvitationException> = coroutineBinding {
        val content = doAcceptInvitation(token).bind()

        fileStorage.metadataResponseByKey(content.key, authenticationOutcome)
            .mapError { ex ->
                AcceptContentInvitationException.ResponseMapping(
                    "Failed to map file metadata to response: ${ex.message}",
                    ex.cause
                )
            }
            .bind()
    }

    override suspend fun setTrustedState(
        key: DocumentKey,
        authenticationOutcome: AuthenticationOutcome,
        trusted: Boolean,
        locale: Locale?
    ): Result<FileMetadataResponse, SetContentTrustedStateException> = coroutineBinding {
        val content = doSetTrustedState(key, trusted).bind()

        fileStorage.metadataResponseByKey(content.key, authenticationOutcome)
            .mapError { ex ->
                SetContentTrustedStateException.ResponseMapping(
                    "Failed to map file metadata to response: ${ex.message}",
                    ex.cause
                )
            }
            .bind()
    }

    override suspend fun updateOwner(
        key: DocumentKey,
        req: UpdateOwnerRequest,
        authenticationOutcome: AuthenticationOutcome.Authenticated,
        locale: Locale?
    ): Result<FileMetadataResponse, UpdateContentOwnerException> = coroutineBinding {
        val content = doUpdateOwner(key, req, authenticationOutcome).bind()

        fileStorage.metadataResponseByKey(content.key, authenticationOutcome)
            .mapError { ex ->
                UpdateContentOwnerException.ResponseMapping(
                    "Failed to map file metadata to response: ${ex.message}",
                    ex.cause
                )
            }
            .bind()
    }

}