package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.request.UpdateContentVisibilityRequest
import io.stereov.singularity.content.core.dto.request.UpdateOwnerRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.file.core.exception.model.DeletingMetadataIsForbiddenException
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import java.util.*

@Service
class FileMetadataManagementService(
    override val userService: UserService,
    override val contentService: FileMetadataService,
    override val authorizationService: AuthorizationService,
    override val invitationService: InvitationService,
    override val userMapper: UserMapper,
    override val translateService: TranslateService,
    private val fileStorage: FileStorage
) : ContentManagementService<FileMetadataDocument>() {

    override val contentType = "files"
    override val logger = KotlinLogging.logger {}

    override suspend fun changeVisibility(
        key: String,
        req: UpdateContentVisibilityRequest,
        locale: Locale?
    ): ContentResponse<FileMetadataDocument> {
        return fileStorage.createResponse(doChangeVisibility(key, req))
    }

    override suspend fun inviteUser(
        key: String,
        req: InviteUserToContentRequest,
        locale: Locale?
    ): ExtendedContentAccessDetailsResponse {
        val metadata = fileStorage.metadataResponseByKey(key)
        val url = metadata.renditions[FileMetadataDocument.ORIGINAL_RENDITION]?.url
            ?: metadata.renditions.values.firstOrNull()?.url
            ?: throw InvalidDocumentException("No renditions saved for file with key $key")
        return doInviteUser(key, req, metadata.key, url, locale)
    }

    override suspend fun acceptInvitation(
        req: AcceptInvitationToContentRequest,
        locale: Locale?
    ): ContentResponse<FileMetadataDocument> {
        return fileStorage.createResponse(doAcceptInvitation(req))
    }

    override suspend fun setTrustedState(
        key: String,
        trusted: Boolean,
        locale: Locale?
    ): ContentResponse<FileMetadataDocument> {
        return fileStorage.createResponse(doSetTrustedState(key, trusted))
    }

    override suspend fun updateOwner(
        key: String,
        req: UpdateOwnerRequest,
        locale: Locale?
    ): ContentResponse<FileMetadataDocument> {
        return fileStorage.createResponse(doUpdateOwner(key, req))
    }

    override suspend fun deleteByKey(key: String) {
        throw DeletingMetadataIsForbiddenException("It is not possible to delete file metadata directly. If you want to delete a file, please use the FileStorage.")
    }
}