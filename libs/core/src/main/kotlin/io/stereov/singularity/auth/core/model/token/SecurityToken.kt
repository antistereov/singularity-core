package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.global.model.Token

abstract class SecurityToken<T: SecurityTokenType> : Token() {
    abstract val type: T
}
