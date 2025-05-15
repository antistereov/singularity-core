package io.stereov.singularity.content.article.exception

import io.stereov.singularity.content.common.exception.ContentException

open class ArticleException(msg: String, cause: Throwable? = null) : ContentException(msg, cause)
