package io.stereov.singularity.content.common.content.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.common.content.dto.ChangeContentTagsRequest
import io.stereov.singularity.content.common.content.dto.ChangeContentVisibilityRequest
import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.core.auth.service.AuthenticationService

abstract class ContentManagementService<T: ContentDocument<T>>(
    private val contentService: ContentService<T>,
    private val authenticationService: AuthenticationService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun changeVisibility(key: String, req: ChangeContentVisibilityRequest): T {
        logger.debug { "Changing visibility of key \"$key\"" }

        val content = validatePermissionsAndGetByKey(key)

        content.access.update(req)

        return contentService.save(content)
    }

    suspend fun changeTags(key: String, req: ChangeContentTagsRequest): T {
        logger.debug { "Changing tags of key \"$key\"" }

        val content = validatePermissionsAndGetByKey(key)
        content.tags = req.tags

        return contentService.save(content)
    }


    suspend fun validatePermissions() {
        authenticationService.validateCurrentUserIsEditor()
    }

    suspend fun validatePermissionsAndGetByKey(key: String): T {
        validatePermissions()

        return contentService.findByKey(key)
    }
}
