package io.stereov.singularity.auth.core.dto.request

data class SessionInfoRequest(
    val browser: String? = null,
    val os: String? = null,
)
