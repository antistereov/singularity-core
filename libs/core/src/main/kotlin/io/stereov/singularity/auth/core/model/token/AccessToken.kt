package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.user.core.model.Role
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*

data class AccessToken(
    val userId: ObjectId,
    val sessionId: UUID,
    val tokenId: String,
    val roles: Set<Role>,
    val groups: Set<String>,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Access> {

    override val type = SessionTokenType.Access
}
