package io.stereov.singularity.principal.core.dto.response

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId

data class PrincipalResponse(
    val id: ObjectId,
    val name: String,
    val email: String?,
    val identityProviders: List<String>,
    val roles: Set<Role> = setOf(Role.User.USER),
    val emailVerified: Boolean = false,
    val lastActive: String,
    val twoFactorAuthEnabled: Boolean,
    val preferredTwoFactorMethod: TwoFactorMethod?,
    val twoFactorMethods: List<TwoFactorMethod>,
    val avatar: FileMetadataResponse?,
    val createdAt: String,
    val groups: Set<String>,
)
