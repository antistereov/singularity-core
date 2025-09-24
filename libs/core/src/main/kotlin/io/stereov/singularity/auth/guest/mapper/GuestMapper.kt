package io.stereov.singularity.auth.guest.mapper

import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.SensitiveUserData
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.model.UserSecurityDetails
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class GuestMapper() {


    fun createGuest(
        id: ObjectId? = null,
        created: Instant = Instant.now(),
        lastActive: Instant = Instant.now(),
        name: String,
        sessions: MutableMap<UUID, SessionInfo> = mutableMapOf(),
        mailTwoFactorCodeExpiresIn: Long,
        avatarFileKey: String? = null,
    ) = UserDocument(
        _id = id,
        createdAt = created,
        lastActive = lastActive,
        roles = mutableSetOf(Role.GUEST),
        groups = mutableSetOf(),
        sensitive = SensitiveUserData(
            name = name,
            email = null,
            identities = mutableMapOf(),
            UserSecurityDetails(false, mailTwoFactorCodeExpiresIn, false),
            sessions = sessions,
            avatarFileKey
        ),
    )


}