package io.stereov.web.user.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.AuthException
import io.stereov.web.auth.exception.InvalidCredentialsException
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.user.dto.LoginRequest
import io.stereov.web.user.dto.RegisterUserRequest
import io.stereov.web.user.exception.EmailAlreadyExistsException
import io.stereov.web.user.model.UserDocument
import org.springframework.stereotype.Service

@Service
class UserSessionService(
    private val userService: UserService,
    private val hashService: HashService,
    private val authenticationService: AuthenticationService,
    private val deviceService: UserDeviceService
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
