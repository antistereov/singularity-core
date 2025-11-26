package io.stereov.singularity.user.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.user.core.dto.request.ConvertToUserRequest
import io.stereov.singularity.user.core.exception.ConvertGuestToUserException
import io.stereov.singularity.user.core.exception.FindPrincipalByIdException
import io.stereov.singularity.user.core.model.Guest
import io.stereov.singularity.user.core.model.User
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class ConvertGuestToUserService(
    private val principalService: PrincipalService,
    private val userService: UserService,
    private val emailProperties: EmailProperties,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val accessTokenCache: AccessTokenCache,
    private val emailVerificationService: EmailVerificationService,
    private val hashService: HashService
) {

    private val logger = logger {}

    /**
     * Converts a guest user to a fully registered user by validating and processing the provided request.
     * Handles guest validation, ensures email uniqueness, encrypts the password, and updates user details.
     * Sends a verification email to the user if email verification is enabled.
     *
     * @param principalId The ID of the principal (guest) to be converted.
     * @param req The request object containing details such as email and password for the conversion.
     * @param locale The locale to be used for any localized operations, such as sending emails.
     * @return A [Result] containing the converted [User] instance if successful, or a [ConvertGuestToUserException] in case of failure.
     */
    suspend fun convertToUser(
        principalId: ObjectId,
        req: ConvertToUserRequest,
        locale: Locale?
    ): Result<User, ConvertGuestToUserException> = coroutineBinding {
        val principal = principalService.findById(principalId)
            .mapError { ex -> when (ex) {
                is FindPrincipalByIdException.NotFound -> ConvertGuestToUserException.NotFound("No guest found with principal ID $principalId")
                else -> ConvertGuestToUserException.Database("Failed to find guest with principal ID $principalId: ${ex.message}", ex)
            } }
            .bind()

        val guest = when (principal) {
            is User -> {
                Err(ConvertGuestToUserException.IsAlreadyUser("Principal with ID $principalId is already a user"))
            }
            is Guest -> Ok(principal)
        }.bind()

        userService.existsByEmail(req.email)
            .mapError { ex -> ConvertGuestToUserException.Database("Failed to check existence of user with email ${req.email}: ${ex.message}", ex) }
            .flatMap { exists ->
                if (exists) Err(ConvertGuestToUserException.EmailTaken("User with email ${req.email} already exists")) else Ok(guest)
            }
            .bind()

        val emailEnabled = emailProperties.enable

        val password = hashService.hashBcrypt(req.password)
            .mapError { ex -> ConvertGuestToUserException.Hash("Failed to convert user to guest because hashing of the password failed: ${ex.message}", ex) }
            .bind()

        val user = User.ofPassword(
            principalId,
            password,
            guest.createdAt,
            Instant.now(),
            guest.sensitive.name,
            req.email,
            false,
            mutableSetOf(),
            emailEnabled,
            twoFactorEmailCodeProperties.expiresIn,
            guest.sensitive.sessions,
            null
        )

        val savedUserDocument = userService.save(user)
            .mapError { ex -> ConvertGuestToUserException.Database("Failed to save user: ${ex.message}", ex) }
            .bind()

        accessTokenCache.invalidateAllTokens(principalId)
            .onFailure { ex -> logger.error(ex) { "Failed to invalidate all access tokens for principal $principalId" } }

        if (emailEnabled) {
            emailVerificationService.sendVerificationEmail(savedUserDocument, locale)
                .onFailure { ex -> logger.error(ex) { "Failed to send verification email for user ${savedUserDocument.id}"} }
        }

        savedUserDocument
    }
}
