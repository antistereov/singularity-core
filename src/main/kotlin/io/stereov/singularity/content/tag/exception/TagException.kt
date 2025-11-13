package io.stereov.singularity.content.tag.exception

import io.stereov.singularity.global.exception.SingularityException

open class TagException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
