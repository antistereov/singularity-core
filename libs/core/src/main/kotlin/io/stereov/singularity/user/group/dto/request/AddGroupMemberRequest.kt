package io.stereov.singularity.user.group.dto.request

import org.bson.types.ObjectId

data class AddGroupMemberRequest(
    val userId: ObjectId
)
