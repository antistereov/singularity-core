package io.stereov.singularity.content.core.dto.request

import org.bson.types.ObjectId

data class UpdateOwnerRequest(
    val newOwnerId: ObjectId
)
