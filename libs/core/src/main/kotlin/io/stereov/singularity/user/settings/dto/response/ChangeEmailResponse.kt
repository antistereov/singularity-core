package io.stereov.singularity.user.settings.dto.response

data class ChangeEmailResponse(
    val verificationRequired: Boolean,
    val cooldown: Long
)