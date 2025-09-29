package io.stereov.singularity.content.article.model

enum class ArticleState {
    PUBLISHED, DRAFT, ARCHIVED;

    companion object {
        fun fromString(input: String): ArticleState? {
            return when (input.lowercase()) {
                "published" -> PUBLISHED
                "draft" -> DRAFT
                "archived" -> ARCHIVED
                else -> null
            }
        }
    }
}
