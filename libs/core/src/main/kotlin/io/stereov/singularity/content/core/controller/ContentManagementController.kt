package io.stereov.singularity.content.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.request.UpdateOwnerRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.exception.*
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.content.invitation.exception.ContentManagementException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.model.Role
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content")
@Tag(name = "Content Management", description = "Operations related to management of content of all types.")
@Tag(name = "Articles")
class ContentManagementController(
    private val context: ApplicationContext,
    private val authorizationService: AuthorizationService,
) {

    @PutMapping("/{contentType}/{key}/access")
    @Operation(
        summary = "Update Access of Content Objects",
        description = """
            Update the access to content object.
            
            You can find more information about content management [here](https://singularity.stereov.io/docs/guides/content/introduction).
            
            >**Note:** You have to specify the content type.
            >For [articles](https://singularity.stereov.io/docs/guides/content/articles) it is `articles`.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this content object is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/introduction"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated content object. The response depends on the content object. " +
                        "For [articles](https://singularity.stereov.io/docs/guides/content/articles) " +
                        "it will return [`FullArticleResponse`](https://singularity.stereov.io/docs/api/schemas/fullarticleresponse).",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        ContentManagementException::class,
        UpdateContentAccessException::class,
    ])
    suspend fun updateContentObjectAccess(
        @PathVariable key: String,
        @PathVariable contentType: String,
        @RequestBody req: UpdateContentAccessRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<out ContentResponse<*>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }

        val response = context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .updateAccess(key, req, authenticationOutcome, locale)
            .getOrThrow { when (it) { is UpdateContentAccessException -> it } }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{contentType}/{key}/access")
    @Operation(
        summary = "Get Access Details of Content Objects",
        description = """
            Get detail on who is allowed to access and change a content object.
            
            You can find more information about content management [here](https://singularity.stereov.io/docs/guides/content/introduction).
            
            >**Note:** You have to specify the content type.
            >For [articles](https://singularity.stereov.io/docs/guides/content/articles) it is `articles`.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this content object is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/introduction"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated content object. The response depends on the content object. " +
                        "For [articles](https://singularity.stereov.io/docs/guides/content/articles) " +
                        "it will return [`FullArticleResponse`](https://singularity.stereov.io/docs/api/schemas/fullarticleresponse).",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        ContentManagementException::class,
        GenerateExtendedContentAccessDetailsException::class,
    ])
    suspend fun getContentObjectAccessDetails(
        @PathVariable contentType: String,
        @PathVariable key: String
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }

        val response = context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .extendedContentAccessDetails(key, authenticationOutcome)
            .getOrThrow { when (it) { is GenerateExtendedContentAccessDetailsException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{contentType}/{key}")
    @Operation(
        summary = "Delete Content Object",
        description = """
            Delete a content object.
            
            You can find more information about content management [here](https://singularity.stereov.io/docs/guides/content/introduction).
            
            >**Note:** You have to specify the content type.
            >For [articles](https://singularity.stereov.io/docs/guides/content/articles) it is `articles`.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state) access on this content object is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/introduction"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        ContentManagementException::class,
        DeleteContentByKeyException::class,
    ])
    suspend fun deleteContentObjectByKey(
        @PathVariable contentType: String,
        @PathVariable key: String
    ): ResponseEntity<SuccessResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }

        context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .deleteByKey(key, authenticationOutcome)
            .getOrThrow { when (it) { is DeleteContentByKeyException -> it } }

        return ResponseEntity.ok(SuccessResponse())
    }

    @PutMapping("/{contentType}/{key}/trusted")
    @Operation(
        summary = "Update Trusted State of Content Object ",
        description = """
            Update the trusted state of a content object.
            This is critical for links and files that may harm the application.
            The trusted state can only be changed by your application-wide [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins)s.
            
            You can find more information about content management [here](https://singularity.stereov.io/docs/guides/content/introduction).
            
            >**Note:** You have to specify the content type.
            >For [articles](https://singularity.stereov.io/docs/guides/content/articles) it is `articles`.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions on the server.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/introduction"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated content object. The response depends on the content object. " +
                        "For [articles](https://singularity.stereov.io/docs/guides/content/articles) " +
                        "it will return [`FullArticleResponse`](https://singularity.stereov.io/docs/api/schemas/fullarticleresponse).",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        ContentManagementException::class,
        SetContentTrustedStateException::class,
    ])
    suspend fun updateContentObjectTrustedState(
        @PathVariable contentType: String,
        @PathVariable key: String,
        @RequestParam trusted: Boolean,
        @RequestParam locale: Locale?
    ): ResponseEntity<ContentResponse<*>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        val res = context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .setTrustedState(key, authenticationOutcome, trusted, locale)
            .getOrThrow { when (it) { is SetContentTrustedStateException -> it } }

        return ResponseEntity.ok(res)
    }

    @PutMapping("/{contentType}/{key}/owner")
    @Operation(
        summary = "Update Owner of Content Object ",
        description = """
            Update the owner of a content object.
            Only owners can perform this action. 
            
            >**Note:** You have to specify the content type.
            >For [articles](https://singularity.stereov.io/docs/guides/content/articles) it is `articles`.
            
            You can find more information about content management [here](https://singularity.stereov.io/docs/guides/content/introduction).
            
            >**Note:** The old owner will automatically be demoted to a [`MAINTAINER`](https://singularity.stereov.io/docs/guides/content/introduction#object-specific-roles-shared-state)
            >role if the action was successful.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              of the owner of this content object is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/content/introduction"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.MAINTAINER_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The updated content object. The response depends on the content object. " +
                        "For [articles](https://singularity.stereov.io/docs/guides/content/articles) " +
                        "it will return [`FullArticleResponse`](https://singularity.stereov.io/docs/api/schemas/fullarticleresponse).",
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        ContentManagementException::class,
        UpdateContentOwnerException::class,
    ])
    suspend fun updateContentObjectOwner(
        @PathVariable contentType: String,
        @PathVariable key: String,
        @RequestBody req: UpdateOwnerRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<ContentResponse<*>> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it} }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it} }

        val res = context.findContentManagementService(contentType)
            .getOrThrow { when (it) { is ContentManagementException -> it } }
            .updateOwner(key, req, authenticationOutcome, locale)
            .getOrThrow { when (it) { is UpdateContentOwnerException -> it } }
        return ResponseEntity.ok(res)
    }
}
