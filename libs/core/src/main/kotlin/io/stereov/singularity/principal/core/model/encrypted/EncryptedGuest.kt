package io.stereov.singularity.principal.core.model.encrypted

import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.sensitve.SensitiveGuestData
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Represents an encrypted [Guest] entity within the system.
 *
 * This data class implements the [EncryptedPrincipal] interface, specifically for guests who
 * have the role [Role.Guest.GUEST.GUEST]. It encapsulates the encrypted sensitive
 * information of a guest, along with their roles, groups, creation time, and last
 * active time.
 *
 * The sensitive information is encrypted using the [Encrypted] wrapper over [SensitiveGuestData],
 * which contains details such as the guest's name and their active sessions.
 *
 * @property _id The unique identifier of the guest entity stored in the database.
 * @property createdAt The timestamp indicating when the guest entity was created.
 * @property lastActive The timestamp of the last recorded activity for the guest.
 * @property sensitive The encrypted sensitive data related to the guest.
 * @property roles The set of roles assigned to the guest, limited to [Role.Guest.GUEST.GUEST].
 * @property groups The set of group identifiers to which the guest belongs, which is empty for guests.
 */
@Document(collection = "principals")
data class EncryptedGuest(
    @Id override val _id: ObjectId? = null,
    override val createdAt: Instant = Instant.now(),
    override var lastActive: Instant = Instant.now(),
    override val sensitive: Encrypted<SensitiveGuestData>,
) : EncryptedPrincipal<Role.Guest, SensitiveGuestData> {

    override val roles = setOf(Role.Guest.GUEST)
    override val groups = emptySet<String>()
}
