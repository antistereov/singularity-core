package io.stereov.singularity.auth.core.model

interface TokenType {
    val cookieName: String
    val header: String
}
