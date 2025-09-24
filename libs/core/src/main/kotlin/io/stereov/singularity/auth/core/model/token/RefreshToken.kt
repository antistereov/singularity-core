package io.stereov.singularity.auth.core.model.token

import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*

data class RefreshToken(
    val userId: ObjectId,
    val sessionId: UUID,
    val tokenId: String,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Refresh>() {

    override val type = SessionTokenType.Refresh
}
