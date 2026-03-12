package io.stereov.singularity.content.core.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import io.stereov.singularity.content.core.model.ContentAccessSubject
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.content.invitation.exception.ContentManagementException
import io.stereov.singularity.database.core.model.DocumentKey
import org.bson.types.ObjectId
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext

fun <T: ContentDocument<T>> ApplicationContext.findContentManagementService(contentType: String): Result<ContentManagementService<*>, ContentManagementException> {
    return this.getBeansOfType<ContentManagementService<T>>()
        .values
        .firstOrNull { it.contentType.contentEquals(contentType, true) }
        .toResultOr { ContentManagementException.ContentTypeNotFound("No content management service found for content type \"$contentType\"") }
}

fun MutableCollection<ContentAccessSubject.UserId>.add(id: ObjectId) = this.add(ContentAccessSubject.UserId(id))
fun MutableCollection<ContentAccessSubject.GroupKey>.add(key: DocumentKey) = this.add(ContentAccessSubject.GroupKey(key))

fun Collection<ContentAccessSubject.UserId>.contains(id: ObjectId) = this.contains(ContentAccessSubject.UserId(id))
fun Collection<ContentAccessSubject.GroupKey>.contains(key: DocumentKey) = this.contains(ContentAccessSubject.GroupKey(key))
