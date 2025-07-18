package io.stereov.singularity.content.core.tag.exception.model

import io.stereov.singularity.content.core.tag.exception.TagException

class InvalidUpdateTagRequest(msg: String, cause: Throwable? = null) : TagException(msg, cause)
