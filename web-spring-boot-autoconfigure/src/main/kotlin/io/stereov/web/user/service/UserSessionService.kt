package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.exception.InvalidCredentialsException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailVerificationCooldownService
import io.stereov.web.global.service.mail.exception.MailVerificationCooldownException
import io.stereov.web.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.web.properties.MailProperties
import io.stereov.web.user.dto.*
import io.stereov.web.user.exception.EmailAlreadyExistsException
import io.stereov.web.user.exception.InvalidUserDocumentException
import io.stereov.web.user.model.UserDocument
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class UserSessionService(
    private val userService: UserService,
    private val hashService: HashService,
    private val jwtService: JwtService,
    private val authenticationService: AuthenticationService,
    private val mailService: MailService,
    private val mailProperties: MailProperties,
    private val mailVerificationCooldownService: MailVerificationCooldownService,
    private val twoFactorAuthService: TwoFactorAuthService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun checkCredentialsAndGetUser(payload: LoginRequest): UserDocument {
        logger.debug { "Logging in user ${payload.email}" }
        val user = userService.findByEmailOrNull(payload.email)
            ?: throw InvalidCredentialsException()

        if (!hashService.checkBcrypt(payload.password, user.password)) {
            throw InvalidCredentialsException()
        }

        if (user.id == null) {
            throw AuthException("Login failed: UserDocument contains no id")
        }

        return user
    }

    suspend fun validateTwoFactorCode(userId: String, code: Int): UserDto {
        val user = userService.findById(userId)
        val secret = user.twoFactorSecret

        requireNotNull(secret) { throw InvalidUserDocumentException("No 2FA secret found") }

        if (!twoFactorAuthService.validateCode(secret, code)) {
            throw AuthException("Invalid 2FA code")
        }

        return user.toDto()
    }

    suspend fun registerAndGetUser(payload: RegisterUserDto): UserDocument {
        logger.debug { "Registering user ${payload.email}" }

        if (userService.existsByEmail(payload.email)) {
            throw EmailAlreadyExistsException("Failed to register user ${payload.email}")
        }

        val verificationUuid = UUID.randomUUID().toString()

        val userDocument = UserDocument(
            email = payload.email,
            password = hashService.hashBcrypt(payload.password),
            name = payload.name,
            verificationUuid = verificationUuid,
        )

        val savedUserDocument = userService.save(userDocument)

        if (savedUserDocument.id == null) {
            throw AuthException("Login failed: UserDocument contains no id")
        }

        if (mailProperties.enableEmailVerification) {
            mailService.sendVerificationEmail(savedUserDocument)
        }

        return savedUserDocument
    }

    suspend fun setUpTwoFactorAuth(): TwoFactorSetupResponseDto {
        logger.debug { "Setting up two factor authentication" }

        val user = authenticationService.getCurrentUser()

        val secret = twoFactorAuthService.generateSecretKey()
        val otpAuthUrl = twoFactorAuthService.getOtpAuthUrl(user.email, secret)

        val updatedUser = user.copy(
            twoFactorEnabled = true,
            twoFactorSecret = secret
        )

        userService.save(updatedUser)

        return TwoFactorSetupResponseDto(secret, otpAuthUrl)
    }

    suspend fun verifyEmail(token: String): UserDto {
        val verificationToken = jwtService.validateAndExtractVerificationToken(token)
        val user = userService.findByEmail(verificationToken.email)

        return if (user.verificationUuid == verificationToken.uuid) {
            userService.save(user.copy(emailVerified = true)).toDto()
        } else {
            user.toDto()
        }
    }

    suspend fun getRemainingEmailVerificationCooldown(): Long {
        val userId = authenticationService.getCurrentUserId()
        return mailVerificationCooldownService.getRemainingEmailVerificationCooldown(userId)
    }

    suspend fun resendEmailVerificationToken() {
        val userId = authenticationService.getCurrentUserId()
        val remainingCooldown = mailVerificationCooldownService.getRemainingEmailVerificationCooldown(userId)

        if (remainingCooldown > 0) throw MailVerificationCooldownException(
            remainingCooldown
        )

        val user = userService.findById(userId)

        return mailService.sendVerificationEmail(user)
    }

    suspend fun logout(deviceId: String): UserDocument {
        val userId = authenticationService.getCurrentUserId()
        val user = userService.findById(userId)
        val updatedDevices = user.devices.filterNot { it.id == deviceId }

        return userService.save(user.copy(devices = updatedDevices, lastActive = Instant.now()))
    }
}
