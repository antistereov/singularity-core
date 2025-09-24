package io.stereov.singularity.auth.oauth2.exception.model

import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception

class OAuth2ProviderConnectedException(
    provider: String
) : OAuth2Exception("The user already connected a different account for the provider $provider")
