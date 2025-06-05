package io.stereov.singularity.content.common.content.dto

import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.auth.model.AccessType
import org.bson.types.ObjectId

data class ChangeContentVisibilityRequest(
    val visibility: AccessType,
    val sharedUsers: Map<ObjectId, ContentAccessRole> = emptyMap(),
    val sharedGroups: Map<String, ContentAccessRole> = emptyMap(),
)
