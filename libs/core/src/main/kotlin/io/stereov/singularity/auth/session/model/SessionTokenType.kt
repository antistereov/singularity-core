package io.stereov.singularity.auth.session.model

import io.stereov.singularity.auth.core.model.TokenType
import org.springframework.http.HttpHeaders

interface SessionTokenType {

    object Access : TokenType("access_token", HttpHeaders.AUTHORIZATION)
    object Refresh : TokenType("refresh_token", HttpHeaders.AUTHORIZATION)
}