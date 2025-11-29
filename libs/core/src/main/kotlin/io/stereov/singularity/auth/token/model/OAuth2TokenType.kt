package io.stereov.singularity.auth.token.model

interface OAuth2TokenType {

    object ProviderConnection : SecurityTokenType {
        const val HEADER = "X-OAuth2-Provider-Connection-EmailVerificationTokenCreation"
        const val COOKIE_NAME = "oauth2_provider_connection_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
}
