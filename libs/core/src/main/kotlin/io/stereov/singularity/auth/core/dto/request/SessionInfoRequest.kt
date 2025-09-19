package io.stereov.singularity.auth.core.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class SessionInfoRequest(
    @field:Schema(description = "The name of the browser used.", example = "Chrome")
    val browser: String? = null,
    @field:Schema(description = "The operating system of the device.", example = "WIndows")
    val os: String? = null,
)
