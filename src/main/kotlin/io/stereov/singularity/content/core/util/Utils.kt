package io.stereov.singularity.content.core.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.exception.ContentManagementException
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext

fun <T: ContentDocument<T>> ApplicationContext.findContentManagementService(contentType: String): Result<ContentManagementService<*>, ContentManagementException> {
    return this.getBeansOfType<ContentManagementService<T>>()
        .values
        .firstOrNull { it.contentType.contentEquals(contentType, true) }
        .toResultOr { ContentManagementException.ContentTypeNotFound("No content management service found for content type \"$contentType\"") }
}