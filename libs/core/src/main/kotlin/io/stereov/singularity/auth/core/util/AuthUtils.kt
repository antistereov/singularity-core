package io.stereov.singularity.auth.core.util

import io.stereov.singularity.auth.core.properties.AuthProperties
import org.springframework.web.server.ServerWebExchange

fun extractTokenFromRequest(tokenName: String, exchange: ServerWebExchange, authProperties: AuthProperties): String? {
    val cookieToken = exchange.request.cookies[tokenName]?.firstOrNull()?.value

    if (!authProperties.allowHeaderAuthentication) return cookieToken

    val headerToken = exchange.request.headers.getFirst(tokenName)

    return if (authProperties.preferHeaderAuthentication) {
        headerToken ?: cookieToken
    } else {
        cookieToken ?: headerToken
    }
}