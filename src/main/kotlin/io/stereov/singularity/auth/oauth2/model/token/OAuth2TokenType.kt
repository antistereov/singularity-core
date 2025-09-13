package io.stereov.singularity.auth.oauth2.model.token

import io.stereov.singularity.auth.core.model.token.SecurityTokenType

interface OAuth2TokenType {

    object ProviderConnection : SecurityTokenType {
        const val HEADER = "X-OAuth2-Provider-Connection-Token"
        const val COOKIE_NAME = "oauth2_provider_connection_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
}
