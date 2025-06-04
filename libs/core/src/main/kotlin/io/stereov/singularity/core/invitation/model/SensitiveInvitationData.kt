package io.stereov.singularity.core.invitation.model

data class SensitiveInvitationData(
    val email: String,
    val claims: Map<String, Any>
)
