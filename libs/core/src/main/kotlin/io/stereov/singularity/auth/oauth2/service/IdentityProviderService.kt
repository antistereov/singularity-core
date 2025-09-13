package io.stereov.singularity.auth.oauth2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.ConnectPasswordIdentityRequest
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.oauth2.exception.model.CannotConnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.exception.model.CannotDisconnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2ProviderConnectedException
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.model.identity.UserIdentity
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service

@Service
class IdentityProviderService(
    private val userService: UserService,
    private val oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
    private val authorizationService: AuthorizationService,
    private val hashService: HashService
) {

    private val logger = KotlinLogging.logger {}

    suspend fun connect(req: ConnectPasswordIdentityRequest): UserDocument {
        logger.debug { "Creating password identity" }

        authorizationService.requireStepUp()

        val user = authorizationService.getCurrentUser()
        val identities = user.sensitive.identities

        if (identities.containsKey(IdentityProvider.PASSWORD))
            throw CannotConnectIdentityProviderException("The user already identified with password")

        identities[IdentityProvider.PASSWORD] = UserIdentity.ofPassword(hashService.hashBcrypt(req.password), true)

        return userService.save(user)
    }

    suspend fun connect(provider: String, principalId: String, oauth2ProviderConnectionTokenValue: String): UserDocument {
        logger.debug { "Connecting a new OAuth2 provider $provider to user" }

        authorizationService.requireStepUp()

        val user = authorizationService.getCurrentUser()
        val connectionToken = oAuth2ProviderConnectionTokenService.extract(oauth2ProviderConnectionTokenValue)

        if (provider != connectionToken.provider)
            throw InvalidTokenException("The OAuth2ProviderConnectionToken does not match the requested provider")

        val identities = user.sensitive.identities

        val providerIdentity = identities[provider]

        if (providerIdentity != null && providerIdentity.principalId != principalId)
            throw OAuth2ProviderConnectedException(provider)

        identities[provider] = UserIdentity.ofProvider(principalId, false)
        return userService.save(user)
    }

    suspend fun disconnect(provider: String): UserDocument {
        logger.debug { "Disconnecting provider $provider from user" }

        authorizationService.requireStepUp()

        val user = authorizationService.getCurrentUser()
        val identities = user.sensitive.identities

        if (provider == IdentityProvider.PASSWORD)
            throw CannotDisconnectIdentityProviderException("Password provider cannot be disconnected from user")

        if (!identities.containsKey(provider))
            throw CannotDisconnectIdentityProviderException("Provider $provider is not connected with user")

        if (identities.size == 1)
            throw CannotDisconnectIdentityProviderException("Provider $provider is the only identity provider")

        identities.remove(provider)

        return userService.save(user)
    }
}
