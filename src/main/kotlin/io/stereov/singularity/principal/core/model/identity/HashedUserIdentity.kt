package io.stereov.singularity.principal.core.model.identity

import io.stereov.singularity.database.hash.model.Hash
import io.stereov.singularity.database.hash.model.SearchableHash

/**
 * Represents a sealed interface for hashed user identities.
 *
 * This interface is used to define user identity structures in a hashed format,
 * supporting both password-based identities and identities associated with external providers.
 *
 * Implementations of this interface include:
 * - [Password]: Represents a user identity stored using a hashed password.
 * - [Provider]: Represents a user identity associated with an external provider,
 *   identified by a [SearchableHash].
 */
sealed interface HashedUserIdentity {

    /**
     * Represents a password-based hashed user identity.
     *
     * This data class is a concrete implementation of the [HashedUserIdentity] interface
     * and encapsulates a hashed password.
     *
     * @property password The hashed password associated with the user identity.
     *
     * @see HashedUserIdentity
     * @see Hash
     */
    data class Password(val password: Hash) : HashedUserIdentity

    /**
     * Represents a provider-based hashed user identity.
     *
     * This data class is a concrete implementation of the [HashedUserIdentity] interface
     * and is used to encapsulate an external identity associated with a third-party service.
     *
     * @property principalId A [SearchableHash] representing the unique identifier of the principal
     * provided by the external service. This includes both the hashed data and its associated
     * secret identifier.
     *
     * @see HashedUserIdentity
     * @see SearchableHash
     */
    data class Provider(val principalId: SearchableHash) : HashedUserIdentity
}
