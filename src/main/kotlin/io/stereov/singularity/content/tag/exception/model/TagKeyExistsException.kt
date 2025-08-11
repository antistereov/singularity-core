package io.stereov.singularity.content.tag.exception.model

import io.stereov.singularity.content.tag.exception.TagException

class TagKeyExistsException(key: String) : TagException("Tag with key \"$key\" exists already")
