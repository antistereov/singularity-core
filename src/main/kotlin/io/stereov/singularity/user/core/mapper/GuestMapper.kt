package io.stereov.singularity.user.core.mapper

import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.user.core.model.Guest
import io.stereov.singularity.user.core.model.sensitve.SensitiveGuestData
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * A component responsible for mapping and creating instances of the [Guest] entity.
 */
@Component
class GuestMapper() {

    /**
     * Creates a [Guest] instance with the provided properties.
     *
     * @param id The unique identifier for the guest. Defaults to null if not specified.
     * @param created The timestamp indicating when the guest was created. Defaults to the current time.
     * @param lastActive The timestamp indicating the last activity of the guest. Defaults to the current time.
     * @param name The name of the guest. This parameter is required.
     * @param sessions A map of session information associated with the guest, keyed by session UUID. Defaults to an empty mutable map.
     * @return A [Guest] object populated with the specified values.
     */
    fun createGuest(
        id: ObjectId? = null,
        created: Instant = Instant.now(),
        lastActive: Instant = Instant.now(),
        name: String,
        sessions: MutableMap<UUID, SessionInfo> = mutableMapOf(),
    ) = Guest(
        _id = id,
        createdAt = created,
        lastActive = lastActive,
        sensitive = SensitiveGuestData(
            name = name,
            sessions = sessions,
        ),
    )

}
