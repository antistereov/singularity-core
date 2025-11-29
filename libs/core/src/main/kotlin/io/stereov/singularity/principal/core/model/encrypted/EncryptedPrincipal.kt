package io.stereov.singularity.principal.core.model.encrypted

import io.stereov.singularity.database.core.model.WithId
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.model.EncryptedSensitiveDocument
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import java.time.Instant

/**
 * Represents an encrypted principal entity within the system, containing roles, group memberships,
 * timestamps for creation and last activity, and sensitive encrypted data.
 *
 * The `EncryptedPrincipal` interface is a sealed contract that outlines the structure for encrypted
 * principal objects, which encapsulate sensitive information and access-related data, such as roles
 * and groups. Specific implementations may include user or guest entities.
 *
 * Implemented classes are [EncryptedUser] and [EncryptedGuest].
 *
 * @param R Represents the type of role associated with the principal, constrained by the `Role` interface.
 * @param S Represents the type of sensitive principal data, constrained by the `SensitivePrincipalData` interface.
 * @property roles The roles granted to the principal, defining access levels and permissions.
 * @property groups The set of group identifiers to which the principal belongs.
 * @property createdAt The timestamp indicating when the principal was created.
 * @property lastActive The timestamp indicating the last recorded activity of the principal.
 * @property sensitive The sensitive information associated with the principal, stored as an encrypted object.
 */
sealed interface EncryptedPrincipal<R: Role, S: SensitivePrincipalData> : EncryptedSensitiveDocument<S>, WithId {
    val roles: Set<R>
    val groups: Set<String>
    val createdAt: Instant
    val lastActive: Instant
    override val sensitive: Encrypted<S>
}
