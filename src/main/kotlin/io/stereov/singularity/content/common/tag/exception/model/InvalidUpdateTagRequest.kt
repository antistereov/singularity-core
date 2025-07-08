package io.stereov.singularity.content.common.tag.exception.model

import io.stereov.singularity.content.common.tag.exception.TagException

class InvalidUpdateTagRequest(msg: String, cause: Throwable? = null) : TagException(msg, cause)
