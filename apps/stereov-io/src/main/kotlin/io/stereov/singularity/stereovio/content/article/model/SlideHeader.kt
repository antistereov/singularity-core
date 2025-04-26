package io.stereov.singularity.stereovio.content.article.model

import kotlinx.serialization.Serializable

@Serializable
data class SlideHeader(
    val textColor: String,
    val backgroundColor: String,
    val title: String,
)
