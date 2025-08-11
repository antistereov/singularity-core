package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.translate.model.Language

data class ChangeArticleContentRequest(
    val lang: Language,
    val content: String
)
