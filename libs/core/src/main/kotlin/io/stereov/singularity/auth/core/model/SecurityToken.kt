package io.stereov.singularity.auth.core.model

interface SecurityToken<T: SecurityTokenType> : Token {
    val type: T
}
