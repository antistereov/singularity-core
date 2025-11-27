package io.stereov.singularity.auth.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.response.MailCooldownResponse
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.exception.SendVerificationEmailException
import io.stereov.singularity.auth.core.exception.VerifyEmailException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.token.exception.EmailVerificationTokenExtractionException
import io.stereov.singularity.auth.token.service.EmailVerificationTokenService
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SendEmailResponse
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth/email/verification")
@Tag(
    name = "Authentication",
)
class EmailVerificationController(
    private val emailVerificationService: EmailVerificationService,
    private val authorizationService: AuthorizationService,
    private val emailVerificationTokenService: EmailVerificationTokenService,
    private val userService: UserService,
    private val principalMapper: PrincipalMapper,
) {

    @PostMapping
    @Operation(
        summary = "Verify Email",
        description = """
            Verify the user's email address using the `token` the user received a email verification email.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            When registering a user and [email is enabled in your application](https://singularity.stereov.io/docs/guides/email/configuration)
            an email containing a link is sent to the user's email address.
            This link should point to the frontend of your application.
            Your frontend should extract the token from the URL and send it to this endpoint with the
            token as request parameter.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            You can resend this email through the endpoint [`POST /api/auth/email/verification/send`](https://singularity.stereov.io/docs/api/send-email-verification-email).

            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).

            >**Note:** If email is disabled, there is no way to verify a user's email address.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        EmailVerificationTokenExtractionException::class,
        VerifyEmailException::class,
        PrincipalMapperException::class
    ])
    suspend fun verifyEmail(
        @RequestParam token: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<UserResponse> {
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

        val token = emailVerificationTokenService.extract(token)
            .getOrThrow { when (it) { is EmailVerificationTokenExtractionException -> it } }

        val user = emailVerificationService.verifyEmail(token, locale)
            .getOrThrow { when (it) { is VerifyEmailException -> it } }

        val response = principalMapper.toResponse(user, authenticationOutcome)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok()
            .body(response)
    }

    @GetMapping("/cooldown")
    @Operation(
        summary = "Get Remaining Email Verification CooldownActive",
        description = """
            Get the remaining time in seconds until you can send another email verification email.
            
            You can find more information about email verification [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The remaining cooldown.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindEncryptedDocumentByIdException::class,
        CacheException.Operation::class
    ])
    suspend fun getRemainingEmailVerificationCooldown(): ResponseEntity<MailCooldownResponse> {
        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val email = userService.findById(principalId)
            .getOrThrow { when (it) { is FindEncryptedDocumentByIdException -> it } }
            .email

        val remainingCooldown = emailVerificationService.getRemainingCooldown(email)
            .getOrThrow { when (it) { is CacheException.Operation -> it } }
            .seconds

        return ResponseEntity.ok().body(MailCooldownResponse(remainingCooldown))
    }

    @PostMapping("/send")
    @Operation(
        summary = "Send Email Verification Email",
        description = """
            Send an email verification email to the user.
            
            This endpoint is for **resending** the verification email only.
            When registering a user and [email is enabled in your application](https://singularity.stereov.io/docs/guides/email/configuration)
            an email containing a link is automatically sent to the user's email address.
            This link should point to the frontend of your application.
            You can find more information about this [here](https://singularity.stereov.io/docs/guides/auth/authentication#email-verification).
            Your frontend should extract the token from the URL and send it to this endpoint with the
            token as request parameter.
            
            You can perform the verification using the token through the endpoint [`POST /api/auth/email/verification`](https://singularity.stereov.io/docs/api/verify-email).
            
            >**Note:** If email is disabled, there is no way to verify a user's email address.
            
            If there is no account associated with the given email address, 
            a [No Principal Information](https://singularity.stereov.io/docs/guides/auth/security-alerts#no-account-information)
            email will be sent to the given email address.
            
            ### Locale
            
            A locale can be specified for this request. 
            The email will be sent in the specified locale.
            You can learn more about locale in emails [here](https://singularity.stereov.io/docs/guides/email/templates).
            
            If no locale is specified, the applications default locale will be used.
            You can learn more about configuring the default locale [here](https://singularity.stereov.io/docs/guides/configuration).

            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            
            >**Note:** After each email, a cooldown will be started.
            >You can check the status of the cooldown through the endpoint [`GET /api/auth/email/verification/cooldown`](https://singularity.stereov.io/docs/api/get-remaining-email-verification-cooldown).
            >When the cooldown is active, no new verification email can be sent.
            >The cooldown can be configured [here](https://singularity.stereov.io/docs/guides/email/configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/docs/auth/authentication#email-verification"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The number of seconds the user needs to wait before sending a new email.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindEncryptedDocumentByIdException::class,
        SendVerificationEmailException::class
    ])
    suspend fun sendEmailVerificationEmail(
        @RequestParam locale: Locale?,
    ): ResponseEntity<SendEmailResponse> {
        val principalId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val user = userService.findById(principalId)
            .getOrThrow { when (it) { is FindEncryptedDocumentByIdException -> it } }

        val cooldown = emailVerificationService.sendVerificationEmail(user, locale)
            .getOrThrow { when (it) { is SendVerificationEmailException -> it } }

        return ResponseEntity.ok().body(
            SendEmailResponse(cooldown)
        )
    }
}
