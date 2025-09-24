package io.stereov.singularity.content.core.component

import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentAccessPermissions
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
    private final val isAdminUsersField = "$userPermissionsField.${ContentAccessPermissions::admin.name}"

    private final val groupPermissionsField = "$accessField.${ContentAccessDetails::groups.name}"
    private final val canViewGroupsField = "$groupPermissionsField.${ContentAccessPermissions::viewer.name}"
    private final val canEditGroupsField = "$groupPermissionsField.${ContentAccessPermissions::editor.name}"
    private final val isAdminGroupsField = "$groupPermissionsField.${ContentAccessPermissions::admin.name}"

    private final val isPublic = Criteria.where(visibilityField).`is`(AccessType.PUBLIC.toString())
    private final val isShared = Criteria.where(visibilityField).`is`(AccessType.SHARED.toString())

    private fun isOwner(userId: ObjectId) = Criteria.where(ownerIdField).`is`(userId)

    private fun canViewUser(userId: ObjectId) = Criteria().andOperator(Criteria.where(canViewUsersField).`in`(userId.toHexString()), isShared)
    private fun canEditUser(userId: ObjectId) = Criteria().andOperator(Criteria.where(canEditUsersField).`in`(userId.toHexString()), isShared)
    private fun isAdminUser(userId: ObjectId) = Criteria.where(isAdminUsersField).`in`(userId.toHexString())

    private fun canViewGroup(groups: Set<String>) = Criteria.where(canViewGroupsField).`in`(groups)
    private fun canEditGroup(groups: Set<String>) = Criteria.where(canEditGroupsField).`in`(groups)
    private fun isAdminGroup(groups: Set<String>) = Criteria.where(isAdminGroupsField).`in`(groups)

    suspend fun getViewCriteria(): Criteria {
        val userId = authorizationService.getUserIdOrNull()

        return if (userId != null) {
            val groups = authorizationService.getGroups()

            Criteria().orOperator(
                isPublic,
                canViewUser(userId),
                canEditUser(userId),
                isAdminUser(userId),
                canViewGroup(groups),
                canEditGroup(groups),
                isAdminGroup(groups),
                isOwner(userId)
            )
        } else isPublic
    }
}
