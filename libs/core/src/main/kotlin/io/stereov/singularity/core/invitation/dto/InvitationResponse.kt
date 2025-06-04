package io.stereov.singularity.core.invitation.dto

import org.bson.types.ObjectId
import java.time.Instant

data class InvitationResponse(
    val id: ObjectId,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val email: String,
    val claims: Map<String, Any>
)
