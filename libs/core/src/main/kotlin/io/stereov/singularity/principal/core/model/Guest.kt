package io.stereov.singularity.principal.core.model

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.principal.core.model.sensitve.SensitiveGuestData
import org.bson.types.ObjectId
import java.time.Instant

/**
 * A data class that represents a Guest entity within the system. This class implements the
 * [Principal] interface with a specific role type of [Role.Guest.GUEST] and sensitive data
 * of type [SensitiveGuestData].
 *
 * The guest entity does not belong to any groups and has limited permissions defined by
 * its role. It holds metadata such as creation time, last activity time, and sensitive
 * information specific to the guest user.
 *
 * @property _id The unique identifier of the guest document. May be null if the guest
 * document has not been persisted yet.
 * @property createdAt The timestamp indicating when the guest was created. Defaults to
 * the current instant if not explicitly provided.
 * @property lastActive The timestamp indicating the guest's most recent activity. Defaults
 * to the current instant if not explicitly provided.
 * @property sensitive The sensitive data associated with the guest, including their name
 * and session-related information.
 */
data class Guest(
    override var _id: ObjectId? = null,
    override val createdAt: Instant = Instant.now(),
    override var lastActive: Instant = Instant.now(),
    override val sensitive: SensitiveGuestData
) : Principal<Role.Guest, SensitiveGuestData> {

    override val logger = KotlinLogging.logger {}

    override val groups = emptySet<String>()
    override val roles = setOf(Role.Guest.GUEST)
}
