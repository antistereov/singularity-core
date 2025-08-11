package io.stereov.singularity.content.invitation.model

data class SensitiveInvitationData(
    val email: String,
    val claims: Map<String, Any>
)
