package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.exception.model.InvalidCredentialsException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.user.dto.ChangeEmailRequest
import io.stereov.web.user.dto.ChangePasswordRequest
import io.stereov.web.user.dto.LoginRequest
import io.stereov.web.user.dto.RegisterUserRequest
import io.stereov.web.user.exception.model.EmailAlreadyExistsException
import io.stereov.web.user.model.ApplicationInfo
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.device.UserDeviceService
import io.stereov.web.user.service.twofactor.UserTwoFactorAuthService
import org.springframework.stereotype.Service

@Service
class UserSessionService(
    private val userService: UserService,
    private val hashService: HashService,
    private val authenticationService: AuthenticationService,
    private val deviceService: UserDeviceService,
    private val userTwoFactorAuthService: UserTwoFactorAuthService
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

        return userService.save(user)
    }

    suspend fun registerAndGetUser(payload: RegisterUserRequest): UserDocument {
        logger.debug { "Registering user ${payload.email}" }

        if (userService.existsByEmail(payload.email)) {
            throw EmailAlreadyExistsException("Failed to register user ${payload.email}")
        }

        val userDocument = UserDocument(
            email = payload.email,
            password = hashService.hashBcrypt(payload.password),
            name = payload.name,
        )

        val savedUserDocument = userService.save(userDocument)

        if (savedUserDocument.id == null) {
            throw AuthException("Login failed: UserDocument contains no id")
        }

        return savedUserDocument
    }

    suspend fun changeEmail(payload: ChangeEmailRequest): UserDocument {
        logger.debug { "Changing email" }

        val user = authenticationService.getCurrentUser()

        if (!hashService.checkBcrypt(payload.password, user.password)) {
            throw InvalidCredentialsException()
        }

        if (user.security.twoFactor.enabled) {
            userTwoFactorAuthService.validateTwoFactorCode(user, payload.twoFactorCode)
        }

        user.email = payload.newEmail

        return userService.save(user)
    }

    suspend fun changePassword(payload: ChangePasswordRequest): UserDocument {
        logger.debug { "Changing password" }

        val user = authenticationService.getCurrentUser()

        if (!hashService.checkBcrypt(payload.oldPassword, user.password)) {
            throw InvalidCredentialsException()
        }

        if (user.security.twoFactor.enabled) {
            userTwoFactorAuthService.validateTwoFactorCode(user, payload.twoFactorCode)
        }

        user.password = hashService.hashBcrypt(payload.newPassword)

        return userService.save(user)
    }

    suspend fun <T: ApplicationInfo> setApplicationInfo(app: T): UserDocument {
        logger.debug { "Setting application info" }

        val user = authenticationService.getCurrentUser()

        user.app = app

        return userService.save(user)
    }

    suspend fun logout(deviceId: String): UserDocument {
        logger.debug { "Logging out user" }

        return deviceService.removeDevice(deviceId)
    }

    suspend fun deleteUser() {
        logger.debug { "Deleting user" }

        val userId = authenticationService.getCurrentUserId()

        userService.deleteById(userId)
    }
}
