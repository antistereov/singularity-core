package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.InvalidCredentialsException
import io.stereov.singularity.auth.core.exception.model.UserAlreadyAuthenticatedException
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.exception.model.MissingRequestParameterException
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import java.util.*

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
    private val registrationAlertService: RegistrationAlertService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Logs in a user and returns the user document.
     *
     * @param payload The login request containing the user's email and password.
     *
     * @return The [UserDocument] of the logged-in user.
     *
     * @throws InvalidCredentialsException If the email or password is invalid.
     * @throws io.stereov.singularity.auth.core.exception.AuthException If the user document does not contain an ID.
     */
    suspend fun login(payload: LoginRequest): UserDocument {
        logger.debug { "Logging in user ${payload.email}" }

        if (authorizationService.isAuthenticated())
            throw UserAlreadyAuthenticatedException("Login failed: user is already authenticated")

        val user = userService.findByEmailOrNull(payload.email)
            ?: throw InvalidCredentialsException()

        val password = user.validateLoginTypeAndGetPassword()

        if (!hashService.checkBcrypt(payload.password, password)) {
            throw InvalidCredentialsException()
        }

        return userService.save(user)
    }

    /**
     * Registers a new user and returns the user document.
     *
     * @param payload The registration request containing the user's email, password, and name.
     *
     * @return The [UserDocument] of the registered user.
     */
    suspend fun register(payload: RegisterUserRequest, sendEmail: Boolean, locale: Locale?) {
        logger.debug { "Registering user ${payload.email}" }

        if (authorizationService.isAuthenticated())
            throw UserAlreadyAuthenticatedException("Register failed: user is already authenticated")

        val user = userService.findByEmailOrNull(payload.email)
        if (user != null) {
            if (sendEmail && emailProperties.enable) {
                registrationAlertService.send(user, locale)
                return
            }
        }

        val userDocument = UserDocument.ofPassword(
            email = payload.email,
            password = hashService.hashBcrypt(payload.password),
            name = payload.name,
            email2faEnabled = emailProperties.enable && twoFactorEmailProperties.enableByDefault,
            mailTwoFactorCodeExpiresIn = factorMailCodeProperties.expiresIn
        )

        val savedUserDocument = userService.save(userDocument)

        if (sendEmail && emailProperties.enable) emailVerificationService.sendVerificationEmail(savedUserDocument, locale)

        return
    }

    suspend fun logout(): UserDocument {
        logger.debug { "Logging out user" }

        val userId = authorizationService.getUserId()
        val tokenId = authorizationService.getTokenId()
        val sessionId = authorizationService.getSessionId()

        accessTokenCache.invalidateToken(userId, sessionId, tokenId)

        return sessionService.deleteSession(sessionId)
    }

    suspend fun stepUp(req: StepUpRequest?): UserDocument {
        logger.debug { "Executing step up" }

        val user = authorizationService.getUser()
        val sessionId = authorizationService.getSessionId()

        if (!user.sensitive.sessions.containsKey(sessionId)) {
            throw AuthException("Step up failed: trying to execute for step up for invalid session, user logged out or revoked session")
        }

        if (user.isGuest) return user

        val password = user.validateLoginTypeAndGetPassword()
        if (req?.password == null)
            throw MissingRequestParameterException("User is not GUEST, therefore a password is required")
        if (!hashService.checkBcrypt(req.password, password)) {
            throw InvalidCredentialsException()
        }

        return user
    }
}
