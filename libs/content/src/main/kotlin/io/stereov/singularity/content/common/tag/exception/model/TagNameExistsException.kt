package io.stereov.singularity.content.common.tag.exception.model

import io.stereov.singularity.content.common.tag.exception.TagException

class TagNameExistsException(name: String) : TagException("Tag with name \"$name\" already exists")
