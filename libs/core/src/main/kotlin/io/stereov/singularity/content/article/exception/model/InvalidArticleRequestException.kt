package io.stereov.singularity.content.article.exception.model

import io.stereov.singularity.content.article.exception.ArticleException

class InvalidArticleRequestException(msg: String, cause: Throwable? = null) : ArticleException(msg, cause)
