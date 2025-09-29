package io.stereov.singularity.content.core.exception.model

import io.stereov.singularity.content.core.exception.ContentException

class ContentTypeNotFoundException(contentType: String): ContentException("No type of content with key $contentType found.")