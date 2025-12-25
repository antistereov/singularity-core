package io.stereov.singularity.principal.settings.controller

import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.mapError
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.token.exception.CookieException
import io.stereov.singularity.auth.token.exception.StepUpTokenExtractionException
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.database.encryption.exception.DeleteEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.exception.SingularityException
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse
import io.stereov.singularity.principal.core.exception.FindPrincipalByIdException
import io.stereov.singularity.principal.core.exception.FindUserByIdException
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.principal.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.principal.settings.dto.request.ChangePrincipalRequest
import io.stereov.singularity.principal.settings.dto.response.ChangeEmailResponse
import io.stereov.singularity.principal.settings.exception.ChangeEmailException
import io.stereov.singularity.principal.settings.exception.ChangePasswordException
import io.stereov.singularity.principal.settings.exception.DeleteUserAvatarException
import io.stereov.singularity.principal.settings.exception.SetUserAvatarException
import io.stereov.singularity.principal.settings.service.PrincipalSettingsService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("/api/users/me")
@Tag(
    name = "Profile Management",
    description = "Operations related to principal settings."
)
class PrincipalSettingsController(
    private val principalMapper: PrincipalMapper,
    private val principalSettingsService: PrincipalSettingsService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService,
    private val principalService: PrincipalService,
    private val userService: UserService,
    private val accessTokenCache: AccessTokenCache
) {

    @GetMapping
    @Operation(
        summary = "Get Principal",
        description = """
            Retrieves the principle profile information of the currently authenticated principle.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Principal information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindPrincipalByIdException::class,
        PrincipalMapperException::class
    ])
    suspend fun getAuthorizedPrincipal(): ResponseEntity<PrincipalResponse> {
        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val principal = principalService.findById(principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }
        val response = principalMapper.toResponse(principal)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }

    @PutMapping("/email")
    @Operation(
        summary = "Change Email",
        description = """
            Change the email of the currently authenticated user.
            
            If [email is enabled](https://singularity.stereov.io/docs/guides/email/configuration),
            the user needs to verify this email address with a token that is sent to the new email address.
            Only then the email is changed in the database.
            If email is disabled, the email will be changed immediately.
            
            ### Requirements
            - The `email` should be a valid email address (e.g., "test@example.com")
              that is not associated to an existing account.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "If email is enabled, `verificationRequired` will be `true` indicating that this request did not update the user, otherwise `verificationRequired` is `false`. Since this request sends an email, the remaining cooldown in seconds will be returned.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindUserByIdException::class,
        ChangeEmailException::class
    ])
    suspend fun changeEmailOfAuthorizedUser(
        @RequestBody @Valid payload: ChangeEmailRequest,
        @RequestParam locale: Locale?,
        exchange: ServerWebExchange,
    ): ResponseEntity<ChangeEmailResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        val user = userService.findById(authentication.principalId)
            .getOrThrow { FindUserByIdException.from(it) }

        val cooldown = principalSettingsService.changeEmail(payload, user, locale)
            .getOrThrow { when (it) { is ChangeEmailException -> it } }
        return ResponseEntity.ok().body(cooldown)
    }

    @Operation(
        summary = "Change Password",
        description = """
            Change the password of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Requirements
            - The `password` must be at least 8 characters long and include at least one uppercase letter, 
              one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\:;'"<>,.?/).
              
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            )
        ]
    )
    @PutMapping("/password")
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindUserByIdException::class,
        ChangePasswordException::class,
        PrincipalMapperException::class
    ])
    suspend fun changePasswordOfAuthorizedUser(
        @RequestBody @Valid payload: ChangePasswordRequest,
        @RequestParam locale: Locale?,
        exchange: ServerWebExchange,
    ): ResponseEntity<PrincipalResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        var user = userService.findById(authentication.principalId)
            .getOrThrow { FindUserByIdException.from(it) }

        user = principalSettingsService.changePassword(payload, user, locale)
            .getOrThrow { when (it) { is ChangePasswordException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok().body(response)
    }

    @PutMapping
    @Operation(
        summary = "Update Principal",
        description = """
            Update the principal information of the currently authenticated principal.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Principal information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindPrincipalByIdException::class,
        SaveEncryptedDocumentException::class,
        PrincipalMapperException::class
    ])
    suspend fun updateAuthorizedUser(@RequestBody payload: ChangePrincipalRequest): ResponseEntity<PrincipalResponse> {
        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        var principal = principalService.findById(principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }

        principal = principalSettingsService.changePrincipal(payload, principal)
            .getOrThrow { when (it) { is SaveEncryptedDocumentException -> it } }

        val response = principalMapper.toResponse(principal)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok().body(response)
    }

    @PutMapping("/avatar")
    @Operation(
        summary = "Update Avatar",
        description = """
            Update the avatar of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindUserByIdException::class,
        SetUserAvatarException::class,
        PrincipalMapperException::class
    ])
    suspend fun setAvatarOfAuthorizedUser(
        @RequestPart file: FilePart,
    ): ResponseEntity<PrincipalResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
        val userId = authentication.principalId
        var user = userService.findById(userId)
            .getOrThrow { FindUserByIdException.from(it) }
        user = principalSettingsService.setAvatar(file, user, authentication)
            .getOrThrow { when (it) { is SetUserAvatarException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok().body(response)
    }

    @DeleteMapping("/avatar")
    @Operation(
        summary = "Delete Avatar",
        description = """
            Delete the avatar of the currently authenticated user.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindUserByIdException::class,
        DeleteUserAvatarException::class,
        PrincipalMapperException::class
    ])
    suspend fun deleteAvatarOfAuthorizedUser(): ResponseEntity<PrincipalResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
        val userId = authentication.principalId
        var user = userService.findById(userId)
            .getOrThrow { FindUserByIdException.from(it) }

        user = principalSettingsService.deleteAvatar(user)
            .getOrThrow { when (it) { is DeleteUserAvatarException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok().body(response)
    }

    @DeleteMapping
    @Operation(
        summary = "Delete Principal",
        description = """
            Delete the currently authenticated principal.
            
            You can find more information about profile management [here](https://singularity.stereov.io/docs/guides/principals/profile-management).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/principals/profile-management"),
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.STEP_UP_TOKEN_COOKIE)
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
        StepUpTokenExtractionException::class,
        FindPrincipalByIdException::class,
        SingularityException.PostCommitSideEffect::class,
        CookieException.Creation::class,
        DeleteEncryptedDocumentByIdException::class,
    ])
    suspend fun deleteAuthorizedPrincipal(exchange: ServerWebExchange): ResponseEntity<SuccessResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        val principalId = authentication.principalId

        principalService.findById(principalId)
            .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }

        accessTokenCache.invalidateAllTokens(principalId)
            .mapError { ex -> SingularityException.PostCommitSideEffect("Failed to invalidate access tokens after successful deletion: ${ex.message}", ex) }
            .getOrThrow { when (it) { is SingularityException.PostCommitSideEffect -> it } }

        val clearAccessTokenCookie = cookieCreator.clearCookie(SessionTokenType.Access)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearRefreshTokenCookie = cookieCreator.clearCookie(SessionTokenType.Refresh)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }
        val clearStepUpTokenCookie = cookieCreator.clearCookie(SessionTokenType.StepUp)
            .getOrThrow { when (it) { is CookieException.Creation -> it } }

        principalService.deleteById(principalId)
            .getOrThrow { when (it) { is DeleteEncryptedDocumentByIdException -> it } }

        return ResponseEntity.ok()
            .header("Set-Cookie", clearAccessTokenCookie.value)
            .header("Set-Cookie", clearRefreshTokenCookie.value)
            .header("Set-Cookie", clearStepUpTokenCookie.value)
            .body(SuccessResponse())
    }
}
