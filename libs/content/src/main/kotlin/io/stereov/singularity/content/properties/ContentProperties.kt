package io.stereov.singularity.content.properties

import io.stereov.singularity.content.common.tag.dto.CreateTagMultiLangRequest
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.content")
data class ContentProperties(
    val tags: List<CreateTagMultiLangRequest>?
)
