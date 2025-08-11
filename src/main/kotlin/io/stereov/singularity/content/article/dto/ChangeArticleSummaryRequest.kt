package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.translate.model.Language

data class ChangeArticleSummaryRequest(
    val lang: Language,
    val summary: String
)
