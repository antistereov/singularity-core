package io.stereov.singularity.principal.settings.dto.response

data class ChangeEmailResponse(
    val verificationRequired: Boolean,
    val cooldown: Long
)
