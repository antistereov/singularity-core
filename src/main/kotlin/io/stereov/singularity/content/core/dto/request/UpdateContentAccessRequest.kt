package io.stereov.singularity.content.core.dto.request

import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.database.core.model.DocumentKey
import org.bson.types.ObjectId

data class UpdateContentAccessRequest(
    val accessType: AccessType,
    val sharedUsers: Map<ObjectId, ContentAccessRole> = emptyMap(),
    val sharedGroups: Map<DocumentKey, ContentAccessRole> = emptyMap(),
)
