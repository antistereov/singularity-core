package io.stereov.singularity.auth.group.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class GroupMemberService(
    private val userService: UserService,
    private val groupService: GroupService,
    private val authService: AuthorizationService
) {

    private val logger = KotlinLogging.logger {}

    suspend fun add(userId: ObjectId, groupKey: String): UserDocument {
        logger.debug { "Adding user \"$userId\" to group \"$groupKey\"" }

        authService.requireRole(Role.ADMIN)
        if (!groupService.existsByKey(groupKey)) {
            throw DocumentNotFoundException("Group with key \"$groupKey\" does not exist")
        }

        val user = userService.findById(userId)
        user.sensitive.groups.add(groupKey)

        return userService.save(user)
    }

    suspend fun remove(userId: ObjectId, groupKey: String): UserDocument {
        logger.debug { "Removing user \"$userId\" from group \"$groupKey\""}

        authService.requireRole(Role.ADMIN)

        if (!groupService.existsByKey(groupKey)) {
            throw DocumentNotFoundException("Group with key \"$groupKey\" does not exist")
        }
        val user = userService.findById(userId)

        user.sensitive.groups.remove(groupKey)

        return userService.save(user)
    }
}
