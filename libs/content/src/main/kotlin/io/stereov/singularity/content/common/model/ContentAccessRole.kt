package io.stereov.singularity.content.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContentAccessRole {
    VIEWER, EDITOR, ADMIN
}
