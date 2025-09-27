package io.stereov.singularity.content.article.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.content.articles")
data class ArticleProperties(
    val enable: Boolean = true,
)