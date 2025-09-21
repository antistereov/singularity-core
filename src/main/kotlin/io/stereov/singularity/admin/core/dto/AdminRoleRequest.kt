package io.stereov.singularity.admin.core.dto

import org.bson.types.ObjectId

data class AdminRoleRequest(
    val userId: ObjectId
)
