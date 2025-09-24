package io.stereov.singularity.user.core.mapper

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.model.UserDocument
import org.springframework.stereotype.Service

@Service
class UserMapper(
    private val fileStorage: FileStorage
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Convert this [UserDocument] to a [UserResponse].
     *
     * This method is used to createGroup a data transfer object (DTO) for the user.
     *
     * @return A [UserResponse] containing the user information.
     */
    suspend fun toResponse(user: UserDocument): UserResponse {
        logger.debug { "Creating UserResponse for user with id \"${user.id}\"" }


        val avatarKey = user.sensitive.avatarFileKey
        val avatarMetadata = avatarKey?.let { fileStorage.metadataResponseByKeyOrNull(it) }

        return UserResponse(
            user.id,
            user.sensitive.name,
            user.sensitive.email,
            user.sensitive.identities.map { it.key },
            user.roles,
            user.sensitive.security.email.verified,
            user.lastActive.toString(),
            user.twoFactorEnabled,
            user.preferredTwoFactorMethod,
            user.twoFactorMethods,
            avatarMetadata,
            user.created.toString(),
            user.groups
        )
    }

    suspend fun toOverview(user: UserDocument): UserOverviewResponse {
        logger.debug { "Creating UserOverviewResponse for user with ID \"${user.id}\"" }

        val avatarKey = user.sensitive.avatarFileKey
        val avatarMetadata = avatarKey?.let { fileStorage.metadataResponseByKeyOrNull(it) }

        return UserOverviewResponse(
            user.id,
            user.sensitive.name,
            user.sensitive.email,
            avatarMetadata,
            user.roles
        )
    }
}
