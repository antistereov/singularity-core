package io.stereov.singularity.user.core.model.identity

import io.stereov.singularity.database.hash.model.Hash

/**
 * Represents the identity of a user in the system.
 * This is a sealed interface that
 * permits concrete implementations for different types of user identity.
 *
 * Password-based identities are represented by the [Password] class,
 * while provider-based identities are represented by the [Provider] class.
 */
sealed interface UserIdentity {

    /**
     * Represents a password-based user identity in the system.
     *
     * This class is a concrete implementation of the [UserIdentity] interface
     * and encapsulates a hashed password.
     *
     * @property password The hashed password associated with the user identity.
     *
     * @see Hash
     * @see UserIdentity
     */
    class Password(val password: Hash) : UserIdentity

    /**
     * Represents a provider-based user identity in the system.
     *
     * This class is a concrete implementation of the [UserIdentity] interface
     * and is used to encapsulate an external identity provided by a third-party service.
     *
     * @property principalId The unique identifier of the principal provided by the external service.
     *
     * @see UserIdentity
     */
    class Provider(val principalId: String) : UserIdentity

    companion object {

        /**
         * Constant representing the identity type for passwords in user-related operations.
         * This value is used to identify password-based user identities within mapping functions
         * and user-related processes.
         */
        const val PASSWORD_IDENTITY = "password"

        /**
         * Creates a new password-based user identity.
         *
         * @param password The hash representing the user's password.
         * @return A [UserIdentity.Password] instance containing the provided hashed password.
         */
        fun ofPassword(password: Hash): Password {
            return Password(password)
        }

        /**
         * Creates a new provider-based user identity.
         *
         * This method generates a [UserIdentity.Provider] instance using the provided unique principal identifier.
         *
         * @param principalId The unique identifier of the principal provided by an external service.
         * @return A [UserIdentity.Provider] instance containing the specified principal ID.
         */
        fun ofProvider(principalId: String): Provider {
            return Provider(principalId)
        }
    }
}
