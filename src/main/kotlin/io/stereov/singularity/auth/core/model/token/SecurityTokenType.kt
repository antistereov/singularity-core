package io.stereov.singularity.auth.core.model.token

interface SecurityTokenType {
    val cookieName: String
    val header: String
}
