package io.stereov.singularity.content.common.content.exception.model

import io.stereov.singularity.content.common.content.exception.ContentException

class ContentKeyExistsException(key: String, contentClassName: String) : ContentException(msg = "$contentClassName with key \"$key\" exists already")
