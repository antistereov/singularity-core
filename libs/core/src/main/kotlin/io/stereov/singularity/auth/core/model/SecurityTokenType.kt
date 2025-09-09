package io.stereov.singularity.auth.core.model

interface SecurityTokenType {
    val cookieName: String
    val header: String
}
