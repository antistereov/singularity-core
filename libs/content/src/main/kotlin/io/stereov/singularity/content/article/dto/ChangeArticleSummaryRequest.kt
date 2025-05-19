package io.stereov.singularity.content.article.dto

import io.stereov.singularity.core.global.language.model.Language

data class ChangeArticleSummaryRequest(
    val lang: Language,
    val summary: String
)
