package io.stereov.singularity.user.core.model

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.model.WrongLoginTypeException
import io.stereov.singularity.auth.core.model.LoginType
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.database.core.model.SensitiveDocument
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.database.hash.model.SecureHash
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.exception.model.MissingFunctionParameterException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Transient
import java.time.Instant

data class UserDocument(
    private var _id: ObjectId? = null,
    val loginType: String,
    var password: SecureHash?,
    val created: Instant = Instant.now(),
    var lastActive: Instant = Instant.now(),
    override var sensitive: SensitiveUserData,
) : SensitiveDocument<SensitiveUserData> {

    constructor(
        id: ObjectId? = null,
        loginType: String,
        password: SecureHash?,
        created: Instant = Instant.now(),
        lastActive: Instant = Instant.now(),
        name: String,
        email: String,
        roles: MutableSet<Role> = mutableSetOf(Role.USER),
        groups: MutableSet<String> = mutableSetOf(),
        mailEnabled: Boolean,
        mailTwoFactorCodeExpiresIn: Long,
        sessions: MutableList<SessionInfo> = mutableListOf(),
        avatarFileKey: String? = null,
    ): this(
        id,
        loginType,
        password,
        created,
        lastActive,
        SensitiveUserData(
            name,
            email,
            roles,
            groups,
            UserSecurityDetails(mailEnabled, mailTwoFactorCodeExpiresIn),
            sessions,
            avatarFileKey
        )
    )

    @get:Transient
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Return the [_id] and throw a [InvalidDocumentException] if the [_id] is null.
     *
     * @throws InvalidDocumentException If [_id] is null
     */
    @get:Transient
    val id: ObjectId
        get() = this._id ?: throw InvalidDocumentException("No ID found in UserDocument")

    /**
     * Returns the path where user-specific information is stored.
     *
     * @throws InvalidDocumentException If [_id] is null.
     */
    @get:Transient
    val fileStoragePath: String
        get() = "users/$id"

    @get:Transient
    val twoFactorEnabled: Boolean
        get() = sensitive.security.twoFactor.enabled

    @get:Transient
    val twoFactorMethods: List<TwoFactorMethod>
        get() = sensitive.security.twoFactor.methods

    /**
     * Check if user authenticated using password and return [SecureHash] if so.
     *
     * @throws WrongLoginTypeException If the user authenticated using a different method.
     * @throws InvalidDocumentException If the user authenticated using password but no password is set.
     */
    fun validateLoginTypeAndGetPassword(): SecureHash {
        if (loginType != LoginType.PASSWORD) {
            throw WrongLoginTypeException("Authentication via password failed: user authenticated using $loginType")
        }

        return password ?: throw InvalidDocumentException("No password is set for user $id")
    }


    override fun toEncryptedDocument(
        encryptedSensitiveData: Encrypted<SensitiveUserData>,
        otherValues: List<Any>
    ): EncryptedUserDocument {
        val hashedEmail = otherValues[0] as? SearchableHash
            ?: throw MissingFunctionParameterException("Please provide the hashed email as parameter.")

        return EncryptedUserDocument(
            _id, hashedEmail, loginType, password, created, lastActive, encryptedSensitiveData
        )
    }

    /**
     * Set up two-factor authentication for the user.
     *
     * This method enables two-factor authentication and sets the secret and recovery code.
     *
     * @param secret The secret key for the user.
     * @param recoveryCodes The recovery codes for the user.
     *
     * @return The updated [UserDocument].
     */
    fun setupTotp(secret: String, recoveryCodes: List<SecureHash>): UserDocument {
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
     * @return The updated [UserDocument].
     */
    fun disableTotp(): UserDocument {
        logger.debug { "Disabling two factor authentication" }

        this.sensitive.security.twoFactor.totp.enabled = false
        this.sensitive.security.twoFactor.totp.secret = null
        this.sensitive.security.twoFactor.totp.recoveryCodes = mutableListOf()

        this.sensitive.security.twoFactor.preferred = TwoFactorMethod.MAIL

        return this
    }

    /**
     * Add or update a session for the user.
     *
     * This method adds a new session or updates an existing session in the user's session list.
     *
     * @param sessionInfo The session information to add or update.
     *
     * @return The updated [UserDocument].
     */
    fun addOrUpdatesession(sessionInfo: SessionInfo): UserDocument {
        logger.debug { "Adding or updating session ${sessionInfo.id}" }

        removeSession(sessionInfo.id)
        this.sensitive.sessions.add(sessionInfo)

        return this
    }

    /**
     * Remove a session from the user's session list.
     *
     * This method removes a session from the user's session list based on the session ID.
     *
     * @param sessionId The ID of the session to remove.
     *
     * @return The updated [UserDocument].
     */
    fun removeSession(sessionId: String): UserDocument {
        logger.debug { "Removing session $sessionId" }

        this.sensitive.sessions.removeAll { session -> session.id == sessionId }

        return this
    }

    /**
     * Clear all sessions from the user's session list.
     *
     * This method removes all sessions from the user's session list.
     */
    fun clearSessions() {
        logger.debug { "Clearing sessions" }

        this.sensitive.sessions.clear()
    }

    /**
     * Add a role to the user.
     *
     * This method adds a new role to the user's role list.
     *
     * @param role The role to add.
     *
     * @return The updated list of roles.
     */
    fun addRole(role: Role): UserDocument {
        this.sensitive.roles.add(role)
        return this
    }

    /**
     * Update the last active timestamp of the user.
     *
     * This method sets the last active timestamp to the current time.
     *
     * @return The updated [UserDocument].
     */
    fun updateLastActive(): UserDocument {
        logger.debug { "Updating last active" }

        this.lastActive = Instant.now()
        return this
    }
}
