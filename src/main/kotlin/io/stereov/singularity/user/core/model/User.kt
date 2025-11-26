package io.stereov.singularity.user.core.model

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.database.hash.model.Hash
import io.stereov.singularity.user.core.exception.PrincipalException
import io.stereov.singularity.user.core.exception.UserException
import io.stereov.singularity.user.core.model.identity.UserIdentities
import io.stereov.singularity.user.core.model.identity.UserIdentity
import io.stereov.singularity.user.core.model.sensitve.SensitiveUserData
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*


/**
 * Represents a user in the system, implementing the [Principal] interface. A user can have different roles and settings,
 * including two-factor authentication and administrative privileges.
 *
 * @property _id The unique identifier of the user.
 * @property createdAt The timestamp when the user was created.
 * @property lastActive The timestamp when the user was last active.
 * @property isAdmin Indicates whether the user has administrative privileges.
 * @property groups A mutable set of group identifiers that the user is associated with.
 * @property sensitive Contains sensitive data related to the user, such as personal information and security details.
 * @property roles The set of roles assigned to the user, updated dynamically based on their status.
 * @property logger A logger instance specific to this class.
 * @property email Retrieves the user's email from sensitive data.
 * @property id Resolves the user's unique identifier as a [Result].
 * @property fileStoragePath Resolves the user's storage path as a [Result].
 * @property twoFactorEnabled Indicates whether two-factor authentication is enabled for the user.
 * @property twoFactorMethods Lists the available two-factor authentication methods for the user.
 * @property password Retrieves the user's hashed password if available as a [Result].
 * @property preferredTwoFactorMethod Retrieves the preferred two-factor authentication method as a [Result].
 */
data class User(
    var _id: ObjectId? = null,
    override val createdAt: Instant = Instant.now(),
    override var lastActive: Instant = Instant.now(),
    var isAdmin: Boolean = false,
    override val groups: MutableSet<String>,
    override var sensitive: SensitiveUserData,
) : Principal<Role.User, SensitiveUserData> {

    override var roles = buildSet {
        add(Role.User.USER)
        if (isAdmin) add(Role.User.ADMIN)
    }

    override val logger: KLogger
        get() = KotlinLogging.logger {}

    val email
        get() = sensitive.email

    override val id: Result<ObjectId, PrincipalException.InvalidDocument>
        get() = this._id.toResultOr { PrincipalException.InvalidDocument("The user document does not have an ID") }

    val fileStoragePath: Result<String, PrincipalException.InvalidDocument>
        get() = id.map { "users/$it" }

    val twoFactorEnabled: Boolean
        get() = sensitive.security.twoFactor.enabled

    val twoFactorMethods: List<TwoFactorMethod>
        get() = sensitive.security.twoFactor.methods

    val password: Result<Hash, UserException.NoPassword>
        get() = sensitive.identities.password
            .toResultOr { UserException.NoPassword("The user did not set up password authentication") }
            .map { it.password }

    val preferredTwoFactorMethod: Result<TwoFactorMethod, UserException.TwoFactorDisabled>
        get() = if (twoFactorEnabled) {
            Ok(sensitive.security.twoFactor.preferred)
        } else {
            Err(UserException.TwoFactorDisabled("Two-factor authentication is disabled"))
        }

    /**
     * Assigns the administrator role to the user.
     *
     * This method updates the user's roles to include both the administrator ([Role.User.ADMIN])
     * and regular user ([Role.User.USER]) roles, and sets the [isAdmin] property to `true`.
     *
     * @return The updated [User] instance with administrative privileges.
     */
    fun addAdminRole(): User {
        isAdmin = true
        roles = setOf(Role.User.ADMIN, Role.User.USER)
        return this
    }

    /**
     * Revokes the administrator role from the user.
     *
     * This method updates the user's roles to only include the regular user role ([Role.User.USER])
     * and sets the [isAdmin] property to `false`.
     *
     * @return The updated [User] instance without the administrator role.
     */
    fun revokeAdminRole(): User {
        isAdmin = false
        roles = setOf(Role.User.USER)
        return this
    }

    /**
     * Set up two-factor authentication for the user.
     *
     * This method enables two-factor authentication and sets the secret and recovery code.
     *
     * @param secret The secret key for the user.
     * @param recoveryCodes The recovery codes for the user.
     *
     * @return The updated [User].
     */
    fun setupTotp(secret: String, recoveryCodes: List<Hash>): User {
        logger.debug { "Setting up two factor authentication" }

        this.sensitive.security.twoFactor.totp.enabled = true
        this.sensitive.security.twoFactor.totp.secret = secret
        this.sensitive.security.twoFactor.totp.recoveryCodes = recoveryCodes.toMutableList()

        this.sensitive.security.twoFactor.preferred = TwoFactorMethod.TOTP

        return this
    }

    /**
     * Disable two-factor authentication for the user.
     *
     * This method disables two-factor authentication and clears the secret and recovery code.
     *
     * @return The updated [User].
     */
    fun disableTotp(): User {
        logger.debug { "Disabling two factor authentication" }

        this.sensitive.security.twoFactor.totp.enabled = false
        this.sensitive.security.twoFactor.totp.secret = null
        this.sensitive.security.twoFactor.totp.recoveryCodes = mutableListOf()

        this.sensitive.security.twoFactor.preferred = TwoFactorMethod.EMAIL

        return this
    }

    companion object {

        /**
         * Creates a new instance of the [User] class with the provided parameters and password-based identity.
         *
         * @param id Optional unique identifier for the user. Defaults to null.
         * @param password The hashed password for the user, encapsulated in a `Hash`.
         * @param created The timestamp when the user was created. Defaults to the current time.
         * @param lastActive The timestamp of the user's last activity. Defaults to the current time.
         * @param name The name of the user.
         * @param email The primary email address associated with the user.
         * @param isAdmin Indicates whether the user has administrative privileges. Defaults to false.
         * @param groups A set of group names or identifiers that the user belongs to. Defaults to an empty mutable set.
         * @param email2faEnabled Indicates whether two-factor authentication via email is enabled.
         * @param mailTwoFactorCodeExpiresIn The expiration duration in seconds for email-based two-factor authentication codes.
         * @param sessions A mutable map of session information, keyed by session UUID. Defaults to an empty mutable map.
         * @param avatarFileKey Optional key for the user's avatar file in storage. Defaults to null.
         */
        fun ofPassword(
            id: ObjectId? = null,
            password: Hash,
            created: Instant = Instant.now(),
            lastActive: Instant = Instant.now(),
            name: String,
            email: String,
            isAdmin: Boolean = false,
            groups: MutableSet<String> = mutableSetOf(),
            email2faEnabled: Boolean,
            mailTwoFactorCodeExpiresIn: Long,
            sessions: MutableMap<UUID, SessionInfo> = mutableMapOf(),
            avatarFileKey: String? = null,
        ) = User(
            id,
            created,
            lastActive,
            isAdmin,
            groups,
            SensitiveUserData(
                name,
                email,
                UserIdentities(UserIdentity.ofPassword(password)),
                UserSecurityDetails(email2faEnabled, mailTwoFactorCodeExpiresIn),
                sessions,
                avatarFileKey
            )
        )

        /**
         * Creates a new [User] instance based on the provided arguments.
         *
         * @param id Optional unique identifier for the user. Defaults to `null` if not provided.
         * @param provider The name of the authentication provider (e.g., Google, Facebook).
         * @param principalId The unique identifier of the user within the authentication provider.
         * @param created The timestamp when the user was created. Defaults to the current time if not provided.
         * @param lastActive The timestamp of when the user was last active. Defaults to the current time if not provided.
         * @param name The name of the user.
         * @param email The email address of the user.
         * @param isAdmin Indicates whether the user has administrative privileges. Defaults to `false`.
         * @param groups A mutable set of groups the user belongs to. Defaults to an empty set if not provided.
         * @param mailTwoFactorCodeExpiresIn The expiration time (in seconds) for two-factor authentication codes sent via email.
         * @param sessions A mutable map of the user's active sessions, keyed by UUID. Defaults to an empty map if not provided.
         * @param avatarFileKey Optional key representing the path to the user's avatar stored in the file system. Defaults to `null` if not provided.
         * @return A [User] instance with the specified properties.
         */
        fun ofProvider(
            id: ObjectId? = null,
            provider: String,
            principalId: String,
            created: Instant = Instant.now(),
            lastActive: Instant = Instant.now(),
            name: String,
            email: String,
            isAdmin: Boolean = false,
            groups: MutableSet<String> = mutableSetOf(),
            mailTwoFactorCodeExpiresIn: Long,
            sessions: MutableMap<UUID, SessionInfo> = mutableMapOf(),
            avatarFileKey: String? = null,
        ) = User(
            id,
            created,
            lastActive,
            isAdmin,
            groups,
            SensitiveUserData(
                name,
                email,
                UserIdentities(providers = mutableMapOf(provider to UserIdentity.ofProvider(principalId))),
                UserSecurityDetails(false, mailTwoFactorCodeExpiresIn, true),
                sessions,
                avatarFileKey
            )
        )
    }
}
