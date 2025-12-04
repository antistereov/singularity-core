package io.stereov.singularity.auth.twofactor.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.token.model.TotpSetupToken
import io.stereov.singularity.auth.token.model.TwoFactorAuthenticationToken
import io.stereov.singularity.auth.token.service.TotpSetupTokenService
import io.stereov.singularity.auth.twofactor.dto.response.TwoFactorSetupResponse
import io.stereov.singularity.auth.twofactor.exception.*
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.properties.TotpRecoveryCodeProperties
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for handling Time-based One-Time Password (TOTP) authentication setup,
 * validation, recovery, and management for users. This service encompasses methods for enabling,
 * validating, disabling, and recovering accounts with TOTP two-factor authentication support.
 *
 * The service integrates with dependencies for managing token-based authentication,
 * hashing, recovery codes, and security alert notifications, ensuring a secure and seamless
 * workflow for users.
 */
@Service
class TotpAuthenticationService(
    private val totpService: TotpService,
    private val totpRecoveryCodeProperties: TotpRecoveryCodeProperties,
    private val setupTokenService: TotpSetupTokenService,
    private val hashService: HashService,
    private val userService: UserService,
    private val accessTokenCache: AccessTokenCache,
    private val securityAlertProperties: SecurityAlertProperties,
    private val securityAlertService: SecurityAlertService,
    private val emailProperties: EmailProperties
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Sets up Time-based One-Time Password (TOTP) two-factor authentication for a user.
     *
     * This method handles the setup process for enabling TOTP authentication, including generating
     * a secret key, constructing an OTP authentication URL, generating recovery codes,
     * and creating a setup token to finalize the configuration.
     *
     * @param user The user for whom TOTP setup is being configured. The user must have a password
     *    set, and TOTP must not yet be enabled.
     * @return A [Result] object that either contains a [TwoFactorSetupResponse] with the TOTP setup details
     *   or an [GenerateTotpDetailsException] if an error occurs during the setup process.
     */
    suspend fun getTotpDetails(user: User): Result<TwoFactorSetupResponse, GenerateTotpDetailsException> = coroutineBinding {
        logger.debug { "Setting up two factor authentication" }

        if (user.sensitive.identities.password == null) {
            Err(GenerateTotpDetailsException.NoPasswordSet("Cannot set up TOTP: user did not set up authentication using a password."))
                .bind()
        }
        if (user.sensitive.security.twoFactor.totp.enabled) {
            Err(GenerateTotpDetailsException.AlreadyEnabled("Cannot set up TOTP: method is already enabled."))
                .bind()
        }

        val secret = totpService.generateSecretKey()
            .mapError { ex -> GenerateTotpDetailsException.Totp("Failed to generate TOTP secret: ${ex.message}", ex) }
            .bind()
        
        val otpAuthUrl = totpService.getOtpAuthUrl(user.email, secret)
        val recoveryCodes = List(totpRecoveryCodeProperties.count) {
            Random.generateString(totpRecoveryCodeProperties.length)
                .mapError { ex -> GenerateTotpDetailsException.InvalidConfiguration("Failed to generate recovery code: ${ex.message}", ex) }
                .bind()
        }
        
        val userId = user.id.mapError { ex -> GenerateTotpDetailsException.InvalidDocument("Failed to get user id: ${ex.message}", ex) }
            .bind()

        val setupToken = setupTokenService.create(userId, secret, recoveryCodes)
            .mapError { ex -> GenerateTotpDetailsException.TokenCreation("Failed to create setup token: ${ex.message}", ex) }
            .bind()

        TwoFactorSetupResponse(secret, otpAuthUrl, recoveryCodes, setupToken.value)
    }

    /**
     * Validates and sets up a Time-based One-Time Password (TOTP) for a user.
     *
     * @param token The TOTP setup token containing the secret and recovery codes.
     * @param code The TOTP code provided by the user for verification.
     * @param user The user for whom the TOTP setup is being validated and configured.
     * @param locale The locale in which the operation is being performed, or null if not applicable.
     * @return A [Result] containing the updated [User] upon success, or an error of type [ValidateTotpSetupException] on failure.
     */
    suspend fun validateSetup(
        token: TotpSetupToken, 
        code: Int, 
        user: User, 
        locale: Locale?
    ): Result<User, ValidateTotpSetupException> = coroutineBinding {

        if (user.sensitive.identities.password == null) {
            Err(ValidateTotpSetupException.NoPasswordSet("Cannot set up TOTP: user did not configured sign in using password."))
                .bind()
        }

        if (user.sensitive.security.twoFactor.totp.enabled) {
            Err(ValidateTotpSetupException.AlreadyEnabled("Cannot set up TOTP: method is already enabled."))
                .bind()
        }
        
        val codeIsValid = totpService.codeIsValid(token.secret, code)
            .mapError { ex -> ValidateTotpSetupException.Totp("Failed to validate TOTP code: ${ex.message}", ex) }
            .bind()
        
        if (!codeIsValid) {
            Err(ValidateTotpSetupException.WrongCode("Invalid two-factor authentication code."))
                .bind()
        }

        val encryptedSecret = token.secret
        val hashedRecoveryCodes = token.recoveryCodes.map { 
            hashService.hashBcrypt(it) 
                .mapError { ex -> ValidateTotpSetupException.Hash("Failed to hash recovery code: ${ex.message}", ex) }
                .bind()
        }

        user.setupTotp(encryptedSecret, hashedRecoveryCodes)
            .clearSessions()

        val savedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> ValidateTotpSetupException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> ValidateTotpSetupException.Database("Failed to save user: ${ex.message}", ex)
            } }
            .bind()
        
        val userId = user.id
            .mapError { ex -> ValidateTotpSetupException.InvalidDocument("Failed to get user id: ${ex.message}", ex) }
            .bind()
        
        accessTokenCache.invalidateAllTokens(userId)
            .mapError { ex -> ValidateTotpSetupException.PostCommitSideEffect("Failed to invalidate all tokens: ${ex.message}", ex) }
            .bind()
        
        if (securityAlertProperties.twoFactorAdded && emailProperties.enable) {
            securityAlertService.send(
                user,
                locale,
                SecurityAlertType.TWO_FACTOR_ADDED,
                twoFactorMethod = TwoFactorMethod.TOTP
            )
                .mapError { ex -> ValidateTotpSetupException.PostCommitSideEffect("Failed to send security alert: ${ex.message}", ex) }
                .bind()
        }

        savedUser
    }

    /**
     * Validates a provided TOTP (Time-based One-Time Password) code for a given user.
     * Ensures that two-factor authentication is enabled and configured correctly
     * for the user before attempting to validate the code.
     *
     * @param user The user for whom the TOTP code validation is performed.
     * @param code The TOTP code to validate.
     * @return A [Result] containing the validated [User] if the code is valid, or a
     *  [ValidateTwoFactorException] indicating the type of validation failure.
     */
    suspend fun validateCode(user: User, code: Int): Result<User, ValidateTwoFactorException> = coroutineBinding {
        if (!user.sensitive.security.twoFactor.totp.enabled) {
            Err(ValidateTwoFactorException.TwoFactorAuthenticationDisabled("Cannot validate code: ${TwoFactorMethod.TOTP} is disabled"))
                .bind()
        }

        val secret = user.sensitive.security.twoFactor.totp.secret 
            ?: Err(ValidateTwoFactorException.InvalidDocument("TOTP is enabled for user ${user.id} but no TOTP secret was found"))
                .bind()
        
        val codeIsValid = totpService.codeIsValid(secret, code)
            .mapError { ex -> ValidateTwoFactorException.Totp("Failed to validate TOTP code: ${ex.message}", ex) }
            .bind()

        if (codeIsValid) { user } else {
            Err(ValidateTwoFactorException.WrongCode("Invalid two-factor authentication code."))
                .bind()
        }
    }

    /**
     * Recovers a user account using a provided two-factor authentication token and a recovery code.
     * This method validates the recovery code, updates the user's recovery codes list by removing the used code,
     * and saves the updated user information.
     *
     * @param token The two-factor authentication token associated with the user.
     *              This token contains the user ID and is used to retrieve the user's information.
     * @param recoveryCode The recovery code provided for account recovery.
     *                     This code is matched against the stored recovery code hashes for validation.
     * @return A [Result] wrapping the [User] object if the recovery succeeds, or a [TotpUserRecoveryException]
     *         if an error occurs during the recovery process (e.g., user not found, invalid code, etc.).
     */
    suspend fun recoverUser(
        token: TwoFactorAuthenticationToken, 
        recoveryCode: String
    ): Result<User, TotpUserRecoveryException> = coroutineBinding {
        logger.debug { "Recovering user" }

        val userId = token.userId

        val user = userService.findById(userId)
            .mapError { when (it) {
                is FindEncryptedDocumentByIdException.NotFound -> TotpUserRecoveryException.UserNotFound("User not found")
                else -> TotpUserRecoveryException.Database("Failed to get user with id ${token.userId}: ${it.message}", it)
            } }
            .bind()
        
        if (!user.twoFactorMethods.contains(TwoFactorMethod.TOTP)) {
            Err(TotpUserRecoveryException.MethodDisabled("Cannot recover user: ${TwoFactorMethod.TOTP} is not enabled for user ${userId}"))
                .bind()
        }

        val recoveryCodeHashes = user.sensitive.security.twoFactor.totp.recoveryCodes

        val match = recoveryCodeHashes.filter { hash ->
            hashService.checkBcrypt(recoveryCode, hash)
                .mapError { ex -> TotpUserRecoveryException.Hash("Failed to check recovery code hash: ${ex.message}", ex) }
                .bind()
        }
        
        recoveryCodeHashes.removeAll(match)

        if (match.isEmpty()) {
            Err(TotpUserRecoveryException.WrongCode("Invalid two-factor recovery code."))
                .bind()
        }

        userService.save(user)
            .mapError { when (it) {
                is SaveEncryptedDocumentException.PostCommitSideEffect ->
                    TotpUserRecoveryException.PostCommitSideEffect("Failed to decrypt user document after successfully saving document to database: ${it.message}", it)
                else -> TotpUserRecoveryException.Database("Failed to save updated user document to database: ${it.message}", it)
            } }
            .bind()
    }

    /**
     * Disables the TOTP (Time-based One-Time Password) method for two-factor authentication (2FA) for the specified user.
     *
     * @param user The user for whom the TOTP method will be disabled.
     * @param locale The locale to be used when sending notifications, if applicable. This parameter may be null.
     * @return A [Result]object containing the updated [User] instance if successful, or a [DisableTotpException] in case of an error.
     */
    suspend fun disable(
        user: User, 
        locale: Locale?
    ): Result<User, DisableTotpException> = coroutineBinding {
        logger.debug { "Disabling 2FA" }
        
        if (!user.sensitive.security.twoFactor.totp.enabled) {
            Err(DisableTotpException.AlreadyDisabled("Cannot disable TOTP: method is already disabled."))
                .bind()
        }

        if (user.twoFactorMethods.size == 1 && user.sensitive.security.twoFactor.totp.enabled) {
            Err(DisableTotpException.CannotDisableOnlyTwoFactorMethod("Cannot disable TOTP: it not allowed to disable the only configured 2FA method."))
                .bind()
        }

        user.disableTotp()

        val savedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> DisableTotpException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> DisableTotpException.Database("Failed to save user: ${ex.message}", ex)
            }}
            .bind()
        
        if (securityAlertProperties.twoFactorRemoved && emailProperties.enable) {
            securityAlertService.send(
                user,
                locale,
                SecurityAlertType.TWO_FACTOR_REMOVED,
                twoFactorMethod = TwoFactorMethod.TOTP
            )
                .mapError { ex -> DisableTotpException.PostCommitSideEffect("Failed to send security alert: ${ex.message}", ex) }
                .bind()
        }

        savedUser
    }

}
