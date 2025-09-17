package io.stereov.singularity.content.article.dto.request

import java.util.*

data class ChangeArticleSummaryRequest(
    val locale: Locale,
    val summary: String
)
