package io.stereov.singularity.content.article.dto

import io.stereov.singularity.core.global.language.model.Language

data class ChangeArticleTitleRequest(
    val lang: Language,
    val title: String,
)
