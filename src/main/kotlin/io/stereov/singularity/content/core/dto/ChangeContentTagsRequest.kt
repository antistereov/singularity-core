package io.stereov.singularity.content.core.dto

data class ChangeContentTagsRequest(
    val tags: MutableSet<String>
)
