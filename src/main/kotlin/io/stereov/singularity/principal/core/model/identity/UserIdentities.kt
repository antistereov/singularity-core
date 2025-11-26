package io.stereov.singularity.principal.core.model.identity

/**
 * Represents a collection of user identities.
 *
 * This data class is designed to handle multiple types of user identities,
 * including password-based identities and identities provided by third-party services.
 *
 * @property password The password-based user identity, represented as an optional instance of [UserIdentity.Password].
 * This property holds the hashed password information if the user has a password-based identity.
 *
 * @property providers A mutable map of provider-based user identities where the key is the provider's name
 * and the value is a [UserIdentity.Provider] instance. This enables the system to manage multiple
 * provider-based identities associated with a single user.
 */
data class UserIdentities(
    val password: UserIdentity.Password? = null,
    val providers: MutableMap<String, UserIdentity.Provider> = mutableMapOf(),
)
