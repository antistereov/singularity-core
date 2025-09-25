package io.stereov.singularity.content.core.dto.request

data class ChangeContentTagsRequest(
    val tags: MutableSet<String>
)
