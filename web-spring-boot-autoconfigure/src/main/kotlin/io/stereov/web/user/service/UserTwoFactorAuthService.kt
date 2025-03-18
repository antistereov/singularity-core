package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.encryption.EncryptionService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.web.properties.TwoFactorAuthProperties
import io.stereov.web.user.dto.TwoFactorSetupResponseDto
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.exception.InvalidUserDocumentException
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class UserTwoFactorAuthService(
    private val userService: UserService,
    private val twoFactorAuthService: TwoFactorAuthService,
    private val encryptionService: EncryptionService,
    private val authenticationService: AuthenticationService,
    private val twoFactorAuthProperties: TwoFactorAuthProperties,
    private val hashService: HashService,
    private val cookieService: CookieService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun setUpTwoFactorAuth(): TwoFactorSetupResponseDto {
        logger.debug { "Setting up two factor authentication" }

        val user = authenticationService.getCurrentUser()

        val secret = twoFactorAuthService.generateSecretKey()
        val otpAuthUrl = twoFactorAuthService.getOtpAuthUrl(user.email, secret)
        val recoveryCode = twoFactorAuthService.generateRecoveryCode(twoFactorAuthProperties.recoveryCodeLength)

        val encryptedSecret = encryptionService.encrypt(secret)
        val hashedRecoveryCode = hashService.hashBcrypt(recoveryCode)

        userService.save(user.setupTwoFactorAuth(encryptedSecret, hashedRecoveryCode))

        return TwoFactorSetupResponseDto(secret, otpAuthUrl, recoveryCode)
    }

    suspend fun validateTwoFactorCode(exchange: ServerWebExchange, code: Int): UserDto {
        logger.debug { "Validating two factor code" }

        val userId = cookieService.validateTwoFactorSessionCookieAndGetUserId(exchange)

        val user = userService.findById(userId)
        val decryptedSecret = user.security.twoFactor.secret?.let { encryptionService.decrypt(it) }
            ?: throw InvalidUserDocumentException("No two factor authentication secret provided in UserDocument")

        if (!twoFactorAuthService.validateCode(decryptedSecret, code)) {
            throw AuthException("Invalid 2FA code")
        }

        return user.toDto()
    }

    suspend fun recoverUser(exchange: ServerWebExchange, recoveryCode: String): UserDto {
        logger.debug { "Recovering user and clearing all devices" }

        val userId = cookieService.validateTwoFactorSessionCookieAndGetUserId(exchange)

        val user = userService.findById(userId)
        val recoveryCodeHash = user.security.twoFactor.recoveryCode
            ?: throw InvalidUserDocumentException("No recovery code saved in UserDocument")

        if (!hashService.checkBcrypt(recoveryCode, recoveryCodeHash)) {
            throw AuthException("Invalid recovery code")
        }

        user.disableTwoFactorAuth()
        user.clearDevices()

        return userService.save(user).toDto()
    }
}
