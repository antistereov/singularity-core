package io.stereov.singularity.content.article.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class ArticleException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
