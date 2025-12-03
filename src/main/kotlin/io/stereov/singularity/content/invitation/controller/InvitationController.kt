package io.stereov.singularity.content.invitation.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.exception.AcceptContentInvitationException
import io.stereov.singularity.content.core.exception.InviteUserException
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.content.invitation.exception.AcceptInvitationException
import io.stereov.singularity.content.invitation.exception.ContentManagementException
import io.stereov.singularity.content.invitation.exception.DeleteInvitationByIdException
import io.stereov.singularity.content.invitation.exception.InvitationTokenExtractionException
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.content.invitation.service.InvitationTokenService
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.exception.FindUserByIdException
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("api/content")
@Tag(name = "Invitations", description = "Operations related to invitations.")
class InvitationController(
    private val service: InvitationService,
    private val context: ApplicationContext,
    private val authorizationService: AuthorizationService,
    private val invitationTokenService: InvitationTokenService,
    private val userService: UserService,
) {

    @PostMapping("/{contentType}/invitations/{key}")
    @Operation(
        summary = "Invite User",
        description = """
            Invite a user to access or edit a content object.
            
            You can find more information about invitations and content [here](https://singularity.stereov.io/docs/guides/content/invitations).
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this object is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/invitations"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The content object the user was invited to. The response depends on the content object. " +
                        "For [articles](https://singularity.stereov.io/docs/guides/content/articles) " +
                        "it will return [`FullArticleResponse`](https://singularity.stereov.io/docs/api/schemas/fullarticleresponse)" +
                        "and for [file metadata](https://singularity.stereov.io/docs/guides/file-storage/metadata) it will return " +
                        "[`FileMetadataResponse`](https://singularity.stereov.io/docs/api/schemas/filemetadataresponse).",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindUserByIdException::class,
        ContentManagementException::class,
        InviteUserException::class
    ])
    suspend fun inviteUserToContentObject(
        @PathVariable key: String,
        @PathVariable contentType: String,
        @RequestBody req: InviteUserToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }
        val inviter = userService.findById(authenticationOutcome.principalId)
            .getOrThrow { FindUserByIdException.from(it) }

        val response = context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .inviteUser(key, req, inviter, authenticationOutcome, locale)
            .getOrThrow { when (it) { is InviteUserException -> it } }

        return ResponseEntity.ok(response)
    }

    @PostMapping("/{contentType}/invitations/accept")
    @Operation(
        summary = "Accept Invitation",
        description = """
            Accept an invitation from a user to access or edit a content object.
            
            You can find more information about invitations and content [here](https://singularity.stereov.io/docs/guides/content/invitations).
            
            ### Locale
            
            A locale can be specified for this request. 
            The given object will be returned in the specified `locale`.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/invitations"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The content object the user was invited to. The response depends on the content object. " +
                        "For [articles](https://singularity.stereov.io/docs/guides/content/articles) " +
                        "it will return [`FullArticleResponse`](https://singularity.stereov.io/docs/api/schemas/fullarticleresponse)" +
                        "and for [file metadata](https://singularity.stereov.io/docs/guides/file-storage/metadata) it will return " +
                        "[`FileMetadataResponse`](https://singularity.stereov.io/docs/api/schemas/filemetadataresponse).",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        InvitationTokenExtractionException::class,
        ContentManagementException::class,
        AcceptInvitationException::class
    ])
    suspend fun acceptInvitationToContentObject(
        @PathVariable contentType: String,
        @RequestBody req: AcceptInvitationToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<out ContentResponse<*>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }

        val token = invitationTokenService.extract(req.token)
            .getOrThrow { when (it) { is InvitationTokenExtractionException -> it } }

        val response = context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .acceptInvitation(token, authenticationOutcome, locale)
            .getOrThrow { when (it) { is AcceptContentInvitationException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/invitations/{id}")
    @Operation(
        summary = "Delete Invitation",
        description = """
            Delete and invalidate an existing invitation.
            
            You can find more information about invitations and content [here](https://singularity.stereov.io/docs/guides/content/invitations).
            
            >**Note:** Expired invitations will be deleted automatically.
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this object is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/invitations"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        DeleteInvitationByIdException::class
    ])
    suspend fun deleteInvitationToContentObjectById(@PathVariable id: ObjectId): ResponseEntity<SuccessResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }

        service.deleteInvitationById(id, authenticationOutcome)
            .getOrThrow { when (it) { is DeleteInvitationByIdException -> it } }
        return ResponseEntity.ok(SuccessResponse(true))
    }


}
