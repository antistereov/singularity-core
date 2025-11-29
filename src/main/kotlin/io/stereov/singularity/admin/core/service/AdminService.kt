package io.stereov.singularity.admin.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.admin.core.exception.InitRootAccountException
import io.stereov.singularity.admin.core.exception.RevokeAdminRoleException
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

/**
 * Service responsible for administrative operations related to user roles and root account management.
 *
 * This service is primarily used to manage administrative roles for users and
 * ensure a root account exists based on the application's configuration.
 */
@Service
class AdminService(
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val hashService: HashService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val emailProperties: EmailProperties,
) {

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun init() {
        runBlocking { initRootAccount().onFailure { ex -> logger.error(ex) { "Failed to initialize root account"} } }
    }

    /**
     * Adds the admin role to the specified user and saves the updated user.
     *
     * @param user The user to whom the admin role will be added.
     * @return A [Result] containing the updated [User] if successful,
     * or an exception of type [SaveEncryptedDocumentException] if an error occurs during the save operation.
     */
    suspend fun addAdminRole(user: User): Result<User, SaveEncryptedDocumentException> {
        logger.debug { "Adding admin role to user ${user._id}" }

        user.addAdminRole()
        return userService.save(user)
    }

    /**
     * Revokes the admin role from the specified user and saves the updated user.
     *
     * Note: If the user being updated is the last remaining admin, the operation will fail.
     *
     * @param user The user from whom the admin role will be revoked.
     * @return A [Result] containing the updated [User] if successful, or an exception of type [RevokeAdminRoleException] if an error occurs.
     */
    suspend fun revokeAdminRole(user: User): Result<User, RevokeAdminRoleException> {
        logger.debug { "Revoking admin role from user ${user._id}" }

        if (userService.findAllByRolesContaining(Role.User.ADMIN).count() == 1) {
            return Err(RevokeAdminRoleException.AtLeastOneAdminRequired("Cannot revoke admin role for last existing admin: at least one admin is required."))
        }

        user.revokeAdminRole()

        return userService.save(user)
            .mapError { ex -> when (ex) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> RevokeAdminRoleException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                else -> RevokeAdminRoleException.Database("Failed to save updated user: ${ex.message}", ex)
            } }
    }

    /**
     * Initializes the root account based on application properties. If the root account does not exist
     * and the configuration permits, it creates the root account using the provided credentials and settings.
     *
     * @return A [Result] indicating success with [Unit] or an [InitRootAccountException] if an error occurs during initialization or creation.
     */
    private suspend fun initRootAccount(): Result<Unit, InitRootAccountException> = coroutineBinding {
        val exists = userService.existsByEmail(appProperties.rootEmail)
            .mapError { ex -> InitRootAccountException.Database("Failed to check existence of root account: ${ex.message}", ex) }
            .bind()

        if (!exists && appProperties.createRootUser) {
            logger.info { "Creating root account" }

            val password = hashService.hashBcrypt(appProperties.rootPassword)
                .mapError { ex -> InitRootAccountException.Hash("Failed to hash root password: ${ex.message}", ex) }
                .bind()

            val rootUser = User.ofPassword(
                email = appProperties.rootEmail,
                password = password,
                name = "Root",
                email2faEnabled = emailProperties.enable,
                mailTwoFactorCodeExpiresIn = twoFactorEmailCodeProperties.expiresIn
            )

            userService.save(rootUser)
                .mapError { ex -> when (ex) {
                    is SaveEncryptedDocumentException.PostCommitSideEffect -> InitRootAccountException.PostCommitSideEffect("Failed to decrypt user after successful commit: ${ex.message}", ex)
                    else -> InitRootAccountException.Database("Failed to save root account: ${ex.message}", ex)
                } }
                .bind()
        } else {
            logger.info { "Root account exists, skipping creation" }
        }
    }
}
