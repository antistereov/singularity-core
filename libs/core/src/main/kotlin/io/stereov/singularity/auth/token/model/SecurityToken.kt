package io.stereov.singularity.auth.token.model

import io.stereov.singularity.global.model.Token

abstract class SecurityToken<T: SecurityTokenType> : Token() {
    abstract val type: T
}
