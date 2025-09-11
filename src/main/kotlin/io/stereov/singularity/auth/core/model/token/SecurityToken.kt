package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.global.model.Token

interface SecurityToken<T: SecurityTokenType> : Token {
    val type: T
}
