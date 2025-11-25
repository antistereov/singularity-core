package io.stereov.singularity.auth.token.model

interface SecurityTokenType {
    val cookieName: String
    val header: String
}
