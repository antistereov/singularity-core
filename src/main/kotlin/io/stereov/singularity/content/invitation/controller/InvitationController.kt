package io.stereov.singularity.content.invitation.controller

import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("api/content/invitations")
@Tag(name = "Invitations", description = "Operations related to invitations.")
class InvitationController(
    private val service: InvitationService,
    private val context: ApplicationContext,
) {

    @PostMapping("/{contentType}/{key}")
    @Operation(
        summary = "Invite User",
        description = """
            Invite a user to access or edit a content resource.
            
            You can find more information about invitations and content [here](https://singularity.stereov.io/docs/guides/content/invitations).
            
            **Locale:**
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/access-roles#maintainer) access on this resource is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/invitations"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated content access details of the resource.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does permit [`ADMIN`](https://singularity.stereov.io/docs/guides/content/access-roles#admins) access on this resource.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun inviteUser(
        @PathVariable key: String,
        @PathVariable contentType: String,
        @RequestBody req: InviteUserToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).inviteUser(key, req, locale)
        )
    }

    @PostMapping("/{contentType}/accept")
    @Operation(
        summary = "Accept Invitation",
        description = """
            Accept an invitation from a user to access or edit a content resource.
            
            You can find more information about invitations and content [here](https://singularity.stereov.io/docs/guides/content/invitations).
            
            **Locale:**
            
            A locale can be specified for this request. 
            The given resource will be returned in the specified `locale`.
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/invitations"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The content resource, the user was invited to.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "`AccessToken` or invitation `token` are invalid or expired.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun acceptInvitation(
        @PathVariable contentType: String,
        @RequestBody req: AcceptInvitationToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<out ContentResponse<*>> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).acceptInvitation(req, locale)
        )
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Invitation",
        description = """
            Delete and invalidate an existing invitation.
            
            You can find more information about invitations and content [here](https://singularity.stereov.io/docs/guides/content/invitations).
            
            **Note:** Expired invitations will be deleted automatically.
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/access-roles#maintainer) access on this resource is required.
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
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does permit [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/access-roles#maintainer) access on this resource.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "No invitation with given `id` found.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun deleteInvitation(@PathVariable id: ObjectId): ResponseEntity<SuccessResponse> {
        service.deleteById(id)
        return ResponseEntity.ok(SuccessResponse(true))
    }


}
