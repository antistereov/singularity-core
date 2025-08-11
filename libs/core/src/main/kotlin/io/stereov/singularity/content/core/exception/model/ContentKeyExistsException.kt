package io.stereov.singularity.content.core.exception.model

import io.stereov.singularity.content.core.exception.ContentException

class ContentKeyExistsException(key: String, contentClassName: String) : ContentException(msg = "$contentClassName with key \"$key\" exists already")
