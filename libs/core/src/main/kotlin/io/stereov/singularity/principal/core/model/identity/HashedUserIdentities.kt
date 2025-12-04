package io.stereov.singularity.principal.core.model.identity

/**
 * Represents a collection of hashed user identities for a user.
 *
 * This data class is used to manage both password-based identities and provider-based identities
 * associated with a user in a hashed format. It includes the user's hashed password (if available)
 * and a mapping of external providers to their respective hashed identities.
 *
 * @property password The hashed password-based user identity, represented as an optional instance
 * of [HashedUserIdentity.Password]. This property contains the hashed password information if a
 * password-based identity exists for the user.
 *
 * @property providers A map of provider-based hashed user identities where the key is the
 * provider's name, and the value is an instance of [HashedUserIdentity.Provider]. This map
 * allows the system to associate multiple provider-based hashed identities with the user.
 */
data class HashedUserIdentities(
    val password: HashedUserIdentity.Password?,
    val providers: Map<String, HashedUserIdentity.Provider>,
)
