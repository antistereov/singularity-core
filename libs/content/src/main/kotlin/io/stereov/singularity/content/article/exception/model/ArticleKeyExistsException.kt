package io.stereov.singularity.content.article.exception.model

import io.stereov.singularity.content.article.exception.ArticleException

class ArticleKeyExistsException(msg: String, cause: Throwable? = null) : ArticleException(msg, cause)
