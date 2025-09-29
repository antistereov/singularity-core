package io.stereov.singularity.content.core.component

import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentAccessPermissions
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component

@Component
class AccessCriteria(
    private val authorizationService: AuthorizationService
) {

    private final val accessField = ContentDocument<*>::access.name
    private final val visibilityField = "$accessField.${ContentAccessDetails::visibility.name}"

    private final val ownerIdField = "$accessField.${ContentAccessDetails::ownerId.name}"

    private final val userPermissionsField = "$accessField.${ContentAccessDetails::users.name}"
    private final val canViewUsersField: String = "$userPermissionsField.${ContentAccessPermissions::viewer.name}"
    private final val canEditUsersField = "$userPermissionsField.${ContentAccessPermissions::editor.name}"
    private final val isMaintainerUsersField = "$userPermissionsField.${ContentAccessPermissions::maintainer.name}"

    private final val groupPermissionsField = "$accessField.${ContentAccessDetails::groups.name}"
    private final val canViewGroupsField = "$groupPermissionsField.${ContentAccessPermissions::viewer.name}"
    private final val canEditGroupsField = "$groupPermissionsField.${ContentAccessPermissions::editor.name}"
    private final val isMaintainerGroupsField = "$groupPermissionsField.${ContentAccessPermissions::maintainer.name}"

    private final val isPublic = Criteria.where(visibilityField).`is`(AccessType.PUBLIC.toString())
    private final val isShared = Criteria.where(visibilityField).`is`(AccessType.SHARED.toString())

    private fun isOwner(userId: ObjectId) = Criteria.where(ownerIdField).`is`(userId)

    private fun canViewUser(userId: ObjectId) = Criteria().andOperator(Criteria.where(canViewUsersField).`in`(userId.toHexString()), isShared)
    private fun canEditUser(userId: ObjectId) = Criteria().andOperator(Criteria.where(canEditUsersField).`in`(userId.toHexString()), isShared)
    private fun isMaintainerUser(userId: ObjectId) = Criteria.where(isMaintainerUsersField).`in`(userId.toHexString())

    private fun canViewGroup(groups: Set<String>) = Criteria.where(canViewGroupsField).`in`(groups)
    private fun canEditGroup(groups: Set<String>) = Criteria.where(canEditGroupsField).`in`(groups)
    private fun isMaintainerGroup(groups: Set<String>) = Criteria.where(isMaintainerGroupsField).`in`(groups)

    suspend fun getAccessCriteria(roles: Set<String> = emptySet()): Criteria {
        val userId = authorizationService.getUserIdOrNull()
            ?: return if (roles.isEmpty() || roles.any { runCatching { ContentAccessRole.fromString(it) }.getOrNull() == ContentAccessRole.VIEWER }) {
                isPublic
            } else {
                Criteria.where("_id").`is`("")
            }

        val groups = authorizationService.getGroups()

        val allAccessCriteria = Criteria().orOperator(
            isOwner(userId),
            isMaintainerGroup(groups),
            isMaintainerUser(userId),
            canEditGroup(groups),
            canEditUser(userId),
            canViewGroup(groups),
            canViewUser(userId),
            isPublic
        )

        if (roles.isEmpty()) return allAccessCriteria

        val criteriaList = mutableSetOf<Criteria>()
        if (roles.any { runCatching { ContentAccessRole.fromString(it) }.getOrNull() == ContentAccessRole.VIEWER }) {
            criteriaList.add(canViewGroup(groups))
            criteriaList.add(canViewUser(userId))
            criteriaList.add(isPublic)
        }
        if (roles.any { runCatching { ContentAccessRole.fromString(it) }.getOrNull() == ContentAccessRole.EDITOR }) {
            criteriaList.add(canEditGroup(groups))
            criteriaList.add(canEditUser(userId))
        }
        if (roles.any { runCatching { ContentAccessRole.fromString(it) }.getOrNull() == ContentAccessRole.MAINTAINER }) {
            criteriaList.add(isMaintainerGroup(groups))
            criteriaList.add(isMaintainerUser(userId))
        }
        if (roles.any { it.equals("owner", true) }) {
            criteriaList.add(isOwner(userId))
        }

        if (criteriaList.isEmpty()) {
            return Criteria.where("_id").`is`("")
        }

        val userFilterCriteria = Criteria().orOperator(*criteriaList.toTypedArray())

        return Criteria().andOperator(allAccessCriteria, userFilterCriteria)
    }
}
