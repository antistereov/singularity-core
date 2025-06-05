package io.stereov.singularity.invitation.model

data class SensitiveInvitationData(
    val email: String,
    val claims: Map<String, Any>
)
