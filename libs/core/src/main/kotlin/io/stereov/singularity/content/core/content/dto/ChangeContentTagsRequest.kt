package io.stereov.singularity.content.common.content.dto

data class ChangeContentTagsRequest(
    val tags: MutableSet<String>
)
