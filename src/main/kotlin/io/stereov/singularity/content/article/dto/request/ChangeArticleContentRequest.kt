package io.stereov.singularity.content.article.dto.request

import java.util.*

data class ChangeArticleContentRequest(
    val locale: Locale,
    val content: String
)
