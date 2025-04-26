package io.stereov.singularity.stereovio.content.article.model

import io.stereov.singularity.core.global.service.file.model.FileMetaData
import kotlinx.serialization.Serializable

@Serializable
data class Slide(
    val header: SlideHeader,
    val backgroundImage: FileMetaData,
    val summary: String,
)
