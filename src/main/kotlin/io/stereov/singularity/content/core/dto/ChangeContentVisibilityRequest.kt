package io.stereov.singularity.content.core.dto

import io.stereov.singularity.auth.core.model.AccessType
import io.stereov.singularity.content.core.model.ContentAccessRole
import org.bson.types.ObjectId

data class ChangeContentVisibilityRequest(
    val visibility: AccessType,
    val sharedUsers: Map<ObjectId, ContentAccessRole> = emptyMap(),
    val sharedGroups: Map<String, ContentAccessRole> = emptyMap(),
)
