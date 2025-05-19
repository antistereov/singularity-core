package io.stereov.singularity.content.common.tag.exception.model

import io.stereov.singularity.content.common.tag.exception.TagException

class TagKeyExistsException(key: String) : TagException("Tag with key \"$key\" exists already")
