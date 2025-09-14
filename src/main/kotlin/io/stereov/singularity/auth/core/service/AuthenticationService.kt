package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
import io.stereov.singularity.auth.core.dto.request.StepUpRequest
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.InvalidCredentialsException
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.exception.model.EmailAlreadyExistsException
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
    private val appProperties: AppProperties,
    private val twoFactorProperties: TwoFactorAuthProperties
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
     *
     * @throws EmailAlreadyExistsException If the email already exists in the system.
     * @throws io.stereov.singularity.auth.core.exception.AuthException If the user document does not contain an ID.
     */
    suspend fun register(payload: RegisterUserRequest, sendEmail: Boolean, lang: Language): UserDocument {
        logger.debug { "Registering user ${payload.email}" }

        if (userService.existsByEmail(payload.email)) {
            throw EmailAlreadyExistsException("Failed to register user ${payload.email}")
        }

        val userDocument = UserDocument.ofPassword(
            email = payload.email,
            password = hashService.hashBcrypt(payload.password),
            name = payload.name,
            mailEnabled = appProperties.enableMail,
            mailTwoFactorCodeExpiresIn = twoFactorProperties.mailTwoFactorCodeExpiresIn
        )

        val savedUserDocument = userService.save(userDocument)

        if (sendEmail && appProperties.enableMail) emailVerificationService.sendVerificationEmail(savedUserDocument, lang)

        return savedUserDocument
    }

    /**
     * Logs out the user from the specified session and returns the updated user document.
     *
     * @param sessionId The ID of the session to log out from.
     *
     * @return The [UserDocument] of the logged-out user.
     */
    suspend fun logout(sessionId: UUID): UserDocument {
        logger.debug { "Logging out user" }

        val userId = authorizationService.getCurrentUserId()
        val tokenId = authorizationService.getCurrentTokenId()

        accessTokenCache.removeTokenId(userId, tokenId)

        return sessionService.deleteSession(sessionId)
    }

    suspend fun stepUp(req: StepUpRequest): UserDocument {
        logger.debug { "Executing step up" }

        val user = authorizationService.getCurrentUser()
        val sessionId = authorizationService.getCurrentSessionId()

        if (!user.sensitive.sessions.containsKey(sessionId)) {
            throw AuthException("Step up failed: trying to execute for step up for invalid session, user logged out out revoked session")
        }

        val password = user.validateLoginTypeAndGetPassword()
        if (!hashService.checkBcrypt(req.password, password)) {
            throw InvalidCredentialsException()
        }

        return user
    }
}
