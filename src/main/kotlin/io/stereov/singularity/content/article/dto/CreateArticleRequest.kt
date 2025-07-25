package io.stereov.singularity.content.article.dto

import io.stereov.singularity.translate.model.Language

data class CreateArticleRequest(
    val lang: Language,
    val title: String,
    val summary: String = "",
    val content: String = "",
)
