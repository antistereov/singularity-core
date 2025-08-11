package io.stereov.singularity.content.article.exception

import io.stereov.singularity.content.core.exception.ContentException

open class ArticleException(msg: String, cause: Throwable? = null) : ContentException(msg, cause)
