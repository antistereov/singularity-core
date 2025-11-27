package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.IdentityProviderInfoService
import io.stereov.singularity.auth.alert.service.RegistrationAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.exception.LoginException
import io.stereov.singularity.auth.core.exception.LogoutException
import io.stereov.singularity.auth.core.exception.RegisterException
import io.stereov.singularity.auth.core.exception.StepUpException
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.principal.core.exception.FindPrincipalByIdException
import io.stereov.singularity.principal.core.exception.FindUserByEmailException
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for handling authentication-related operations such as login, registration,
 * logout, and session elevation.
 * This service interacts with other components such as user services,
 * hashing services, and email verification mechanisms to provide comprehensive authentication management.
 *
 * The service manages user authentication states, security validations, identity verification,
 * and related workflows.
 */
@Service
class AuthenticationService(
    private val userService: UserService,
    private val hashService: HashService,
    private val authorizationService: AuthorizationService,
    private val sessionService: SessionService,
    private val accessTokenCache: AccessTokenCache,
    private val emailVerificationService: EmailVerificationService,
    private val factorMailCodeProperties: TwoFactorEmailCodeProperties,
    private val emailProperties: EmailProperties,
    private val twoFactorEmailProperties: TwoFactorEmailProperties,
    private val registrationAlertService: RegistrationAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
    private val identityProviderInfoService: IdentityProviderInfoService,
    private val principalService: PrincipalService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Attempts to log in a user with the provided credentials.
     *
     * @param payload The login request containing user credentials.
     * @param locale The locale information used for localization purposes, if applicable.
     * @return A [Result] containing the [User] if the login is successful, or a [LoginException] if an error occurs.
     */
    suspend fun login(payload: LoginRequest, locale: Locale?): Result<User, LoginException> = coroutineBinding {
        logger.debug { "Logging in user ${payload.email}" }

        if (authorizationService.isAuthenticated()) {
            Err(LoginException.AlreadyAuthenticated("Login failed: user is already authenticated"))
                .bind()
        }

        val user = userService.findByEmail(payload.email)
            .mapError { when (it) {
                is FindUserByEmailException.NotFound -> LoginException.InvalidCredentials("Login failed: invalid credentials")
                else -> LoginException.Database("Login failed: database failure", it)
            } }
            .bind()

        val password = user.password
            .mapError {
                if (emailProperties.enable) {
                    identityProviderInfoService.send(user, locale)
                        .onFailure { ex -> logger.error(ex) { "Failed to send identity provider info"} }
                }

                LoginException.InvalidCredentials("Login failed: invalid credentials")
            }
            .bind()

        val passwordValid = hashService.checkBcrypt(payload.password, password)
            .mapError { LoginException.Database("Login failed: database failure", it) }
            .bind()

        if (!passwordValid) {
            Err(LoginException.InvalidCredentials("Login failed: invalid credentials"))
                .bind()
        }

        user
    }

    /**
     * Registers a new user with the provided details in the system.
     *
     * @param payload The details required to register the user, including email and password.
     * @param sendEmail If true, triggers email-related actions, such as verification or alert emails, during registration.
     * @param locale The locale to use for emailing or logging purposes, if applicable.
     * @return A [Result] containing `Unit` if the registration is successful, or a [RegisterException] describing the failure.
     */
    suspend fun register(
        payload: RegisterUserRequest,
        sendEmail: Boolean,
        locale: Locale?
    ): Result<Unit, RegisterException> = coroutineBinding {
        logger.debug { "Registering user ${payload.email}" }

        if (authorizationService.isAuthenticated()) {
            Err(RegisterException.AlreadyAuthenticated("Registration failed: user is already authenticated"))
                .bind()
        }

        val user = userService.findByEmail(payload.email)
            .recoverIf(
                { it is FindUserByEmailException.NotFound },
                { null }
            )
            .mapError { ex -> RegisterException.Database("Registration failed: database failure", ex) }
            .bind()

        if (user != null) {
            if (securityAlertProperties.registrationWithExistingEmail && emailProperties.enable ) {
                registrationAlertService.send(user, locale)
                    .onFailure { exception -> logger.error(exception) { "Failed to send registration alert"} }
            }
            if (sendEmail && emailProperties.enable) {
                emailVerificationService.startCooldown(payload.email)
                    .onFailure { exception -> logger.error(exception) { "Failed to start email cooldown"} }
            }

            return@coroutineBinding
        }

        val password = hashService.hashBcrypt(payload.password)
            .mapError { ex -> RegisterException.Hash("Failed to hash: ${ex.message}", ex) }
            .bind()

        val userDocument = User.ofPassword(
            email = payload.email,
            password = password,
            name = payload.name,
            email2faEnabled = emailProperties.enable && twoFactorEmailProperties.enableByDefault,
            mailTwoFactorCodeExpiresIn = factorMailCodeProperties.expiresIn
        )

        val registeredUser = userService.save(userDocument)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> RegisterException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> RegisterException.Database("Registration failed: database failure", ex)
            } }
            .bind()

        if (sendEmail && emailProperties.enable) {
            emailVerificationService.sendVerificationEmail(registeredUser, locale)
                .onFailure { ex -> logger.error(ex) { "Failed to send verification email"} }
        }
    }

    /**
     * Logs the user out based on their authentication outcome.
     * The method handles different scenarios
     * depending on whether the user is authenticated or has no valid authentication state.
     *
     * @param authenticationOutcome The current state of the user's authentication.
     * It determines if the user is authenticated
     * or has no valid authentication.
     * @return A [Result] that represents the success or failure of the logout operation.
     * If successful, returns [Unit].
     * If failed, returns a [LogoutException] describing the error.
     */
    suspend fun logout(
        authenticationOutcome: AuthenticationOutcome
    ): Result<Unit, LogoutException> = coroutineBinding {
        logger.debug { "Logging out user" }

        when (authenticationOutcome) {
            is AuthenticationOutcome.Authenticated -> {
                val principalId = authenticationOutcome.principalId
                val sessionId = authenticationOutcome.sessionId
                val tokenId = authenticationOutcome.tokenId

                val principal = principalService.findById(principalId)
                    .mapError { when (it) {
                        is FindPrincipalByIdException.NotFound -> LogoutException.NotFound("Logout failed: principal not found", it)
                        else -> LogoutException.Database("Logout failed: database failure", it)
                    } }
                    .bind()

                accessTokenCache.invalidateToken(principalId, sessionId, tokenId)
                    .onFailure { ex -> logger.error(ex) { "Failed to invalidate access token"} }

                sessionService.deleteSession(principal, sessionId)
                    .onFailure { ex -> logger.error(ex) { "Failed to delete session"} }
            }
            is AuthenticationOutcome.None -> {
                Err(LogoutException.AlreadyLoggedOut("Logout failed: user is already logged out"))
                    .bind()
            }
        }
    }

    /**
     * Attempts to elevate the user's session by verifying their credentials based on the provided session ID and request parameters.
     *
     * @param principal The current [Principal] representing the user or guest, including associated roles and sensitive data.
     * @param sessionId The session identifier for which the step-up operation is being performed.
     * @param req The optional request data required for the step-up process, such as user credentials.
     * @return A [Result] containing the updated [Principal] on success, or a [StepUpException] on failure.
     */
    suspend fun stepUp(
        principal: Principal<out Role, out SensitivePrincipalData>,
        sessionId: UUID,
        req: StepUpRequest?
    ): Result<Principal<out Role, out SensitivePrincipalData>, StepUpException> {
        logger.debug { "Executing step up" }


        if (!principal.sensitive.sessions.containsKey(sessionId)) {
            return Err(StepUpException.Session("Step up failed: trying to execute for step up for invalid session, user logged out or revoked session"))
        }

        return when (principal) {
            is Guest -> Ok(principal)
            is User -> coroutineBinding {
                val password = principal.password
                    .mapError { ex -> StepUpException.NoPassword("Step up failed: user did not set up a password", ex) }
                    .bind()

                if (req?.password == null) {
                    Err(StepUpException.MissingPasswordParameter("User is not GUEST, therefore a password is required"))
                        .bind()
                }

                val passwordValid = hashService.checkBcrypt(req.password, password)
                    .mapError { ex -> StepUpException.Hashing("Step up failed: hashing failed: ${ex.message}", ex) }
                    .bind()

                if (!passwordValid) {
                    Err(StepUpException.InvalidCredentials("Step up failed: invalid credentials"))
                        .bind()
                }

                principal
            }
        }
    }
}
