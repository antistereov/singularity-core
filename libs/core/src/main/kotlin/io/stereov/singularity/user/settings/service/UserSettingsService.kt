package io.stereov.singularity.user.settings.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.exception.model.WrongLoginTypeException
import io.stereov.singularity.auth.core.model.LoginType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.exception.model.EmailAlreadyExistsException
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.user.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.user.settings.dto.request.ChangeUserRequest
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class UserSettingsService(
    private val authorizationService: AuthorizationService,
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val hashService: HashService,
    private val appProperties: AppProperties,
    private val fileStorage: FileStorage,
    private val accessTokenCache: AccessTokenCache,
    private val userMapper: UserMapper
) {

    private val logger = KotlinLogging.logger {}

    suspend fun changeEmail(payload: ChangeEmailRequest, lang: Language): UserDocument {
        logger.debug { "Changing email" }

        val user = authorizationService.getCurrentUser()

        if (user.loginType != LoginType.PASSWORD)
            throw WrongLoginTypeException("Changing email is forbidden for users that signed up using ${user.loginType}")

        authorizationService.requireStepUp()

        if (userService.existsByEmail(payload.newEmail)) {
            throw EmailAlreadyExistsException("Failed to register user ${payload.newEmail}")
        }

        if (appProperties.enableMail) {
            emailVerificationService.sendVerificationEmail(user, lang, payload.newEmail)
        } else {
            user.sensitive.email = payload.newEmail
            user.sensitive.security.mail.verified = true
        }

        return userService.save(user)
    }

    suspend fun changePassword(payload: ChangePasswordRequest): UserDocument {
        logger.debug { "Changing password" }

        val user = authorizationService.getCurrentUser()
        if (user.loginType != LoginType.PASSWORD)
            throw WrongLoginTypeException("Changing email is forbidden for users that signed up using ${user.loginType}")

        authorizationService.requireStepUp()

        user.password = hashService.hashBcrypt(payload.newPassword)

        return userService.save(user)
    }

    suspend fun changeUser(payload: ChangeUserRequest): UserDocument {
        val user = authorizationService.getCurrentUser()

        if (payload.name != null) user.sensitive.name = payload.name

        return userService.save(user)
    }

    suspend fun setAvatar(file: FilePart): UserResponse {
        val user = authorizationService.getCurrentUser()

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

        userService.save(user)

        user.sensitive.avatarFileKey = fileStorage
            .upload(user.id, file, "${user.fileStoragePath}/avatar", true)
            .key

        val savedUser = userService.save(user)
        return userMapper.toResponse(savedUser)
    }

    suspend fun deleteAvatar(): UserResponse {
        val user = authorizationService.getCurrentUser()

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
        val userId = authorizationService.getCurrentUserId()
        accessTokenCache.invalidateAllTokens(userId)

        userService.deleteById(userId)
    }
}
