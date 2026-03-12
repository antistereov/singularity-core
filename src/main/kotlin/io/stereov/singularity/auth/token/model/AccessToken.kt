package io.stereov.singularity.auth.token.model

import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*

data class AccessToken(
    val userId: ObjectId,
    val sessionId: UUID,
    val tokenId: String,
    val roles: Set<Role>,
    val groups: Set<DocumentKey>,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Access>() {

    override val type = SessionTokenType.Access
}
