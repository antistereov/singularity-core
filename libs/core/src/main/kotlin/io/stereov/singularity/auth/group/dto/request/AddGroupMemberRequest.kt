package io.stereov.singularity.auth.group.dto.request

import org.bson.types.ObjectId

data class AddGroupMemberRequest(
    val userId: ObjectId
)
