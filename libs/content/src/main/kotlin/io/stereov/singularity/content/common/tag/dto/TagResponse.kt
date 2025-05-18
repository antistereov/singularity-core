package io.stereov.singularity.content.common.tag.dto

import org.bson.types.ObjectId

data class TagResponse(
    val id: ObjectId,
    val name: String,
    val description: String
)
