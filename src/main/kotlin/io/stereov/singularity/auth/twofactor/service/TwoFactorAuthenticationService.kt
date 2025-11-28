package io.stereov.singularity.auth.twofactor.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.token.model.TwoFactorAuthenticationToken
import io.stereov.singularity.auth.twofactor.dto.request.ChangePreferredTwoFactorMethodRequest
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorAuthenticationRequest
import io.stereov.singularity.auth.twofactor.exception.ChangePreferredTwoFactorMethodException
import io.stereov.singularity.auth.twofactor.exception.SendEmailAuthenticationException
import io.stereov.singularity.auth.twofactor.exception.ValidateTwoFactorException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for handling two-factor authentication (2FA) operations such as initiation, validation,
 * and updating a user's preferred 2FA method.
 *
 * This service supports multiple 2FA methods, including TOTP (Time-based One-Time Password) and email-based verification.
 * It integrates with supporting services such as `UserService`, `TotpAuthenticationService`, and `EmailAuthenticationService`.
 *
 * The service ensures secure handling of 2FA for users to enhance account protection.
 */
@Service
class TwoFactorAuthenticationService(
    private val userService: UserService,
    private val totpAuthenticationService: TotpAuthenticationService,
    private val emailAuthenticationService: EmailAuthenticationService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Handles the initiation of the two-factor authentication process based on the user's preferred method.
     *
     * If the user's preferred two-factor authentication method is email, an authentication email will be sent.
     * Otherwise, no action is taken, and the operation completes successfully.
     *
     * @param user The user for whom the two-factor authentication process is handled.
     * @param locale An optional locale used for localization in sending the two-factor email, if applicable.
     * @return A [Result] containing [Unit] if the operation succeeds or a [SendEmailAuthenticationException] if there is an error in sending the email.
     */
    suspend fun handleTwoFactor(user: User, locale: Locale?): Result<Long, SendEmailAuthenticationException> {

        return if (user.sensitive.security.twoFactor.preferred == TwoFactorMethod.EMAIL) {
            emailAuthenticationService.sendMail(user, locale)
        } else Ok(0)
    }

    /**
     * Validates a two-factor authentication request using the provided token and user-supplied data.
     *
     * @param token The two-factor authentication token associated with the user.
     * @param req The request containing the data needed for two-factor authentication validation, such as TOTP or email code.
     * @return A [Result] containing the authenticated [User] if validation succeeds, or a [ValidateTwoFactorException] if validation fails.
     */
    suspend fun validateTwoFactor(
        token: TwoFactorAuthenticationToken,
        req: TwoFactorAuthenticationRequest
    ): Result<User, ValidateTwoFactorException> = coroutineBinding {
        logger.debug { "Validating two factor code" }

        val user = userService.findById(token.userId)
            .mapError { when (it) {
                is FindEncryptedDocumentByIdException.NotFound -> ValidateTwoFactorException.UserNotFound("User not found")
                else -> ValidateTwoFactorException.Database("Failed to get user with id ${token.userId}: ${it.message}", it)
            } }
            .bind()

        if (user.sensitive.identities.password == null) {
            Err(ValidateTwoFactorException.NoPasswordSet("Failed to validate two factor code: user did not set up authentication using a password."))
                .bind()
        }
        if (!user.twoFactorEnabled) {
            Err(ValidateTwoFactorException.TwoFactorDisabled("Failed to validate two factor code: user does not have two factor enabled."))
                .bind()
        }

        val result = if (user.sensitive.security.twoFactor.totp.enabled) {
            req.totp?.let { totpAuthenticationService.validateCode(user, it) }
        } else if (user.sensitive.security.twoFactor.email.enabled) {
            req.email?.let { emailAuthenticationService.validateCode(user, it) }
        } else {
            Err(ValidateTwoFactorException.InvalidDocument("Failed to validate two factor code: user does not have any two factor methods enabled."))
        }

        if (result == null) {
            Err(ValidateTwoFactorException.InvalidRequest("Failed to validate two factor code: at least one of the two factor methods is required."))
                .bind()
        }

        result.bind()
    }

    /**
     * Updates the user's preferred two-factor authentication method.
     *
     * @param req The request object containing the preferred two-factor method to set.
     * @param user The user for whom the preferred two-factor method is being updated.
     * @return A [Result] containing the updated [User] if the operation succeeds,
     * or a [ChangePreferredTwoFactorMethodException] if the operation fails.
     */
    suspend fun changePreferredMethod(
        req: ChangePreferredTwoFactorMethodRequest,
        user: User
    ): Result<User, ChangePreferredTwoFactorMethodException> = coroutineBinding {
        logger.debug { "Changing preferred 2FA method to ${req.method}" }

        if (user.sensitive.identities.password == null) {
            Err(ChangePreferredTwoFactorMethodException.NoPasswordSet("Cannot update preferred method: user did not set up authentication using a password."))
                .bind()
        }

        when (req.method) {
            TwoFactorMethod.EMAIL -> if (!user.sensitive.security.twoFactor.email.enabled) {
                Err(ChangePreferredTwoFactorMethodException.TwoFactorDisabled("Cannot set ${TwoFactorMethod.EMAIL} as preferred method: method is disabled"))
                    .bind()
            }
            TwoFactorMethod.TOTP -> if (!user.sensitive.security.twoFactor.totp.enabled) {
                Err(ChangePreferredTwoFactorMethodException.TwoFactorDisabled("Cannot set ${TwoFactorMethod.TOTP} as preferred method: method is disabled"))
                    .bind()
            }
        }

        user.sensitive.security.twoFactor.preferred = req.method

        userService.save(user)
            .mapError { when (it) {
                is SaveEncryptedDocumentException.PostCommitSideEffect ->
                    ChangePreferredTwoFactorMethodException.PostCommitSideEffect("An error occurred after successfully saving the updated use to the database: ${it.message}", it)
                else -> ChangePreferredTwoFactorMethodException.Database("Failed to save updated user to database: ${it.message}", it)
            } }
            .bind()
    }

}
