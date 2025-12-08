package io.stereov.singularity.principal.settings.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.download.model.StreamedFile
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.principal.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.principal.settings.dto.request.ChangePrincipalRequest
import io.stereov.singularity.principal.settings.dto.response.ChangeEmailResponse
import io.stereov.singularity.principal.settings.exception.ChangeEmailException
import io.stereov.singularity.principal.settings.exception.ChangePasswordException
import io.stereov.singularity.principal.settings.exception.DeleteUserAvatarException
import io.stereov.singularity.principal.settings.exception.SetUserAvatarException
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import java.util.*
import kotlin.getOrElse
import kotlin.runCatching

/**
 * Service responsible for managing and updating principal settings, including email, password,
 * avatar updates, and principal-specific changes. Handles interactions with associated services
 * such as email verification, user management, file storage, and security alerts.
 */
@Service
class PrincipalSettingsService(
    private val emailVerificationService: EmailVerificationService,
    private val userService: UserService,
    private val hashService: HashService,
    private val fileStorage: FileStorage,
    private val emailProperties: EmailProperties,
    private val securityAlertProperties: SecurityAlertProperties,
    private val securityAlertService: SecurityAlertService,
    private val imageStore: ImageStore,
    private val principalService: PrincipalService
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Changes the email address associated with a user account.
     * If email verification is enabled, a verification
     * email is sent to the new address, and the change will only be finalized upon verification.
     * If email verification is disabled, the email change is applied immediately.
     *
     * @param payload The request containing the new email address to associate with the user account.
     * @param user The user whose email is being updated.
     * @param locale The locale to use for the email verification process, if applicable.
     * @return A [Result] containing either a [ChangeEmailResponse] with details about the change
     *  or a [ChangeEmailException] describing the failure.
     */
    suspend fun changeEmail(
        payload: ChangeEmailRequest,
        user: User,
        locale: Locale?
    ): Result<ChangeEmailResponse, ChangeEmailException> = coroutineBinding {
        logger.debug { "Changing email for user" }

        val exists = userService.existsByEmail(payload.newEmail)
            .mapError { ex -> ChangeEmailException.Database("Failed to check existence of user with email ${payload.newEmail}: ${ex.message}", ex) }
            .bind()

        if (exists) {
            Err(ChangeEmailException.EmailTaken("User with email ${payload.newEmail} already exists"))
                .bind()
        }

        if (emailProperties.enable) {
            val cooldown = emailVerificationService.sendVerificationEmail(user, locale, payload.newEmail)
                .onFailure { ex -> logger.error(ex) { "Failed to send verification email for user ${user.id}"} }
                .getOrElse { 0 }
            ChangeEmailResponse(true, cooldown)
        } else {
            user.sensitive.email = payload.newEmail
            user.sensitive.security.email.verified = true
            userService.save(user)
                .mapError { ex -> when (ex) {
                    is SaveEncryptedDocumentException.PostCommitSideEffect -> ChangeEmailException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                    else -> ChangeEmailException.Database("Failed to save updated user: ${ex.message}", ex)
                } }
                .bind()
            ChangeEmailResponse(false, 0)
        }
    }

    suspend fun changePassword(
        payload: ChangePasswordRequest,
        user: User,
        locale: Locale?
    ): Result<User, ChangePasswordException> = coroutineBinding {
        logger.debug { "Changing password" }

        val passwordProvider = user.sensitive.identities.password
            .toResultOr { ChangePasswordException.NoPasswordSet("Cannot change password: user did not set up password authentication") }
            .bind()

        passwordProvider.password = hashService.hashBcrypt(payload.newPassword)
            .mapError { ex -> ChangePasswordException.Hashing("Failed to hash new password: ${ex.message}", ex) }
            .bind()

        val updatedUser = userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> ChangePasswordException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> ChangePasswordException.Database("Failed to save updated user: ${ex.message}", ex)
            } }
            .bind()

        if (emailProperties.enable && securityAlertProperties.passwordChanged) {
            securityAlertService.send(updatedUser, locale, SecurityAlertType.PASSWORD_CHANGED)
                .mapError { ex -> ChangePasswordException.PostCommitSideEffect("Failed to send security alert for user ${updatedUser.id}: ${ex.message}", ex) }
                .bind()
        }

        updatedUser
    }

    /**
     * Updates the current principal with new data provided in the payload and saves the updated principal.
     *
     * @param payload The request payload containing the data to update the principal.
     * @param principal The current principal containing the role and sensitive data to be updated.
     * @return A [Result] containing the updated [Principal] if the operation is successful,
     *  or a [SaveEncryptedDocumentException] if the operation fails.
     */
    suspend fun changePrincipal(
        payload: ChangePrincipalRequest,
        principal: Principal<out Role, out SensitivePrincipalData>
    ): Result<Principal<out Role, out SensitivePrincipalData>, SaveEncryptedDocumentException> {
        if (payload.name != null) principal.sensitive.name = payload.name

        return principalService.save(principal)
    }

    /**
     * Updates the avatar for a specified user.
     *
     * @param file The file part representing the new avatar image to be uploaded.
     * @param user The user whose avatar is being set.
     * @param authentication The authentication context of the user performing the operation.
     * @return A [Result] containing the updated [User] if successful, or a [SetUserAvatarException] if an error occurs.
     */
    suspend fun setAvatar(
        file: FilePart,
        user: User,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<User, SetUserAvatarException> = coroutineBinding {
        val currentAvatar = user.sensitive.avatarFileKey

        if (currentAvatar != null) {
            fileStorage.remove(currentAvatar)
                .onFailure { ex -> logger.debug(ex) { "Failed to remove old image" } }
        }

        val allowedMediaTypes = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)

        val contentType = file.headers().contentType
            .toResultOr { SetUserAvatarException.UnsupportedMediaType("Media type is not set") }
            .bind()

        if (contentType !in allowedMediaTypes) {
            Err(SetUserAvatarException.UnsupportedMediaType("Unsupported file type: $contentType"))
                .bind()
        }

        val fileStoragePath = user.fileStoragePath
            .mapError { ex -> SetUserAvatarException.Database("Failed to get file storage path for user: ${ex.message}", ex) }
            .bind()

        val avatarKey = imageStore
            .upload(authentication, file, "$fileStoragePath/avatar", true)
            .mapError { ex -> SetUserAvatarException.File("Failed to upload new avatar: ${ex.message}") }
            .bind()
            .key

        user.sensitive.avatarFileKey = avatarKey

        userService.save(user)
            .mapError { ex ->
                when (ex) {
                    is SaveEncryptedDocumentException.PostCommitSideEffect -> SetUserAvatarException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                    else -> {
                        fileStorage.remove(avatarKey)
                            .onFailure { ex -> logger.debug(ex) { "Failed to remove uploaded image after failed save: ${ex.message}" } }
                        SetUserAvatarException.Database("Failed to save updated user to database: ${ex.message}", ex)
                    }
                }
            }
            .bind()
    }

    /**
     * Sets the avatar for a given user using the specified file.
     *
     * This method uploads the provided avatar file, removes the previous avatar if it exists,
     * and updates the user's avatar in the database.
     * In case of failure, the appropriate cleanup
     * and error handling are performed.
     *
     * @param file The file representing the new avatar to be uploaded.
     * @param user The user whose avatar is being updated.
     * @param authentication The authentication context required to authorize the operation.
     * @return A [Result] containing the updated [User] object on success, or a [SetUserAvatarException] on failure.
     */
    suspend fun setAvatar(
        file: StreamedFile,
        user: User,
        authentication: AuthenticationOutcome.Authenticated
    ): Result<User, SetUserAvatarException> = coroutineBinding {
        logger.debug { "Setting avatar for user ${user.id} from URL ${file.url}" }

        val currentAvatar = user.sensitive.avatarFileKey

        if (currentAvatar != null) {
            runCatching { fileStorage.remove(currentAvatar) }
                .getOrElse { ex -> logger.debug(ex) { "Failed to remove old image" } }
        }

        val fileStoragePath = user.fileStoragePath
            .mapError { ex -> SetUserAvatarException.Database("Failed to get file storage path for user: ${ex.message}", ex) }
            .bind()

        val avatarKey = imageStore
            .upload(authentication, file, "$fileStoragePath/avatar", true)
            .mapError { ex -> SetUserAvatarException.File("Failed to upload new avatar: ${ex.message}") }
            .bind()
            .key

        user.sensitive.avatarFileKey = avatarKey

        userService.save(user)
            .mapError { ex ->
                when (ex) {
                    is SaveEncryptedDocumentException.PostCommitSideEffect -> SetUserAvatarException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                    else -> {
                        fileStorage.remove(avatarKey)
                            .onFailure { ex -> logger.debug(ex) { "Failed to remove uploaded image after failed save: ${ex.message}" } }
                        SetUserAvatarException.Database("Failed to save updated user to database: ${ex.message}", ex)
                    }
                }
            }
            .bind()
    }

    /**
     * Deletes the avatar of a given user by removing the avatar file from storage
     * and updating the user record to unset the avatar file key.
     *
     * @param user The user whose avatar is to be deleted.
     * @return A [Result] containing the updated user object if the operation succeeds,
     * or a [DeleteUserAvatarException] if an error occurs.
     */
    suspend fun deleteAvatar(user: User): Result<User, DeleteUserAvatarException> = coroutineBinding {
        val currentAvatar = user.sensitive.avatarFileKey

        if (currentAvatar != null) {
            fileStorage.remove(currentAvatar)
                .mapError { ex -> DeleteUserAvatarException.File("Failed to remove avatar: ${ex.message}", ex) }
                .bind()
        }

        user.sensitive.avatarFileKey = null

        userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> DeleteUserAvatarException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> DeleteUserAvatarException.Database("Failed to save updated user to database: ${ex.message}", ex)
            } }
            .bind()
    }
}
