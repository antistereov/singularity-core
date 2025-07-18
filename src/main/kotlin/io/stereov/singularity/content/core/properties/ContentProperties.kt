package io.stereov.singularity.content.core.properties

import io.stereov.singularity.content.core.tag.dto.CreateTagMultiLangRequest
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.content")
data class ContentProperties(
    val tags: List<CreateTagMultiLangRequest>?
)
