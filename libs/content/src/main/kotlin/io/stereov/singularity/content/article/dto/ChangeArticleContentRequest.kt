package io.stereov.singularity.content.article.dto

import io.stereov.singularity.global.language.model.Language

data class ChangeArticleContentRequest(
    val lang: Language,
    val content: String
)
