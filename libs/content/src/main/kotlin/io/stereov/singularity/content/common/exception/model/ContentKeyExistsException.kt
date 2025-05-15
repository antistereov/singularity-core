package io.stereov.singularity.content.common.exception.model

import io.stereov.singularity.content.common.exception.ContentException

class ContentKeyExistsException(key: String, contentClassName: String) : ContentException(msg = "$contentClassName with key \"$key\" exists already")
