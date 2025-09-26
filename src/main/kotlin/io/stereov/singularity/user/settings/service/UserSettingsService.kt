package io.stereov.singularity.user.settings.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.model.WrongIdentityProviderException
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.core.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.core.service.SecurityAlertService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.exception.model.EmailAlreadyTakenException
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.util.*

@Service
class UserSettingsService(
    private val authorizationService: AuthorizationService,
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val hashService: HashService,
    private val fileStorage: FileStorage,
    private val accessTokenCache: AccessTokenCache,
    private val userMapper: UserMapper,
    private val emailProperties: EmailProperties,
    private val securityAlertProperties: SecurityAlertProperties,
    private val securityAlertService: SecurityAlertService,
    private val imageStore: ImageStore
) {

    private val logger = KotlinLogging.logger {}

    suspend fun changeEmail(payload: ChangeEmailRequest, locale: Locale?): UserDocument {
        logger.debug { "Changing email" }

        val user = authorizationService.getUser()

        authorizationService.requireStepUp()

        if (userService.existsByEmail(payload.newEmail)) {
            throw EmailAlreadyTakenException("Failed to register user ${payload.newEmail}")
        }

        if (emailProperties.enable) {
            emailVerificationService.sendVerificationEmail(user, locale, payload.newEmail)
        } else {
            user.sensitive.email = payload.newEmail
            user.sensitive.security.email.verified = true
        }

        return userService.save(user)
    }

    suspend fun changePassword(payload: ChangePasswordRequest, locale: Locale?): UserDocument {
        logger.debug { "Changing password" }

        val user = authorizationService.getUser()
        val passwordProvider = user.sensitive.identities[IdentityProvider.PASSWORD]
            ?: throw WrongIdentityProviderException("Cannot change password: user did not set up password authentication")

        authorizationService.requireStepUp()

        passwordProvider.password = hashService.hashBcrypt(payload.newPassword)

        val updatedUser = userService.save(user)

        if (emailProperties.enable && securityAlertProperties.passwordChanged) {
            securityAlertService.send(updatedUser, locale, SecurityAlertType.PASSWORD_CHANGED)
        }

        return updatedUser
    }

    suspend fun changeUser(payload: ChangeUserRequest): UserDocument {
        val user = authorizationService.getUser()

        if (payload.name != null) user.sensitive.name = payload.name

        return userService.save(user)
    }

    suspend fun setAvatar(file: FilePart, exchange: ServerWebExchange): UserResponse {
        val user = authorizationService.getUser()

        val currentAvatar = user.sensitive.avatarFileKey

        if (currentAvatar != null) {
            fileStorage.remove(currentAvatar)
        }

        val allowedMediaTypes = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)

        val contentType = file.headers().contentType
            ?: throw UnsupportedMediaTypeException("Media type is not set")

        if (contentType !in allowedMediaTypes) {
            throw UnsupportedMediaTypeException("Unsupported file type: $contentType")
        }

        user.sensitive.avatarFileKey = imageStore
            .upload(user.id, file, "${user.fileStoragePath}/avatar", true)
            .key

        val savedUser = userService.save(user)
        return userMapper.toResponse(savedUser)
    }

    suspend fun deleteAvatar(): UserResponse {
        val user = authorizationService.getUser()

        val currentAvatar = user.sensitive.avatarFileKey

        if (currentAvatar != null) {
            fileStorage.remove(currentAvatar)
        }

        user.sensitive.avatarFileKey = null

        val savedUser = userService.save(user)

        return userMapper.toResponse(savedUser)
    }

    suspend fun deleteUser() {
        logger.debug { "Deleting user" }

        authorizationService.requireStepUp()
        val userId = authorizationService.getUserId()
        accessTokenCache.invalidateAllTokens(userId)

        userService.deleteById(userId)
    }
}
