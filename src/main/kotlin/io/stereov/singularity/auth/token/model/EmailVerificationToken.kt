package io.stereov.singularity.auth.token.model

import io.stereov.singularity.global.model.Token
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class EmailVerificationToken(
    val userId: ObjectId,
    val email: String,
    val secret: String,
    override val jwt: Jwt
) : Token()
