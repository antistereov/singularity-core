package io.stereov.singularity.content.core.util

import io.stereov.singularity.content.core.exception.model.ContentTypeNotFoundException
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.service.ContentManagementService
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext

fun <T: ContentDocument<T>> ApplicationContext.findContentManagementService(contentType: String): ContentManagementService<*> {
    return this.getBeansOfType<ContentManagementService<T>>()
        .values
        .firstOrNull { it.contentType.contentEquals(contentType, true) }
        ?: throw ContentTypeNotFoundException(contentType)
}