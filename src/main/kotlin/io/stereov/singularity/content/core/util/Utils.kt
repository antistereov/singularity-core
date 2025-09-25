package io.stereov.singularity.content.core.util

import io.stereov.singularity.content.core.exception.model.ContentTypeNotFoundException
import io.stereov.singularity.content.core.service.ContentManagementService
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext

fun ApplicationContext.findContentManagementService(contentType: String): ContentManagementService<*> {
    return this.getBeansOfType<ContentManagementService<*>>()
        .values
        .firstOrNull { it.contentKey.contentEquals(contentType, true) }
        ?: throw ContentTypeNotFoundException(contentType)
}