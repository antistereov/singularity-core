package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.ConnectPasswordIdentityRequest
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.oauth2.exception.model.CannotConnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.exception.model.CannotDisconnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.model.identity.UserIdentity
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

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

        if (identities.any { it.provider == IdentityProvider.PASSWORD })
            throw CannotConnectIdentityProviderException("The user already identified with password")

        identities.add(UserIdentity.Companion.ofPassword(hashService.hashBcrypt(req.password), true))

        return userService.save(user)
    }

    suspend fun connect(provider: String, principalId: String, exchange: ServerWebExchange): UserDocument {
        logger.debug { "Connecting a new OAuth2 provider $provider to user" }

        authorizationService.requireStepUp()

        val user = authorizationService.getCurrentUser()
        val connectionToken = oAuth2ProviderConnectionTokenService.extract(exchange)

        if (provider != connectionToken.provider)
            throw InvalidTokenException("The OAuth2ProviderConnectionToken does not match the requested provider")

        val identities = user.sensitive.identities

        if (identities.any { it.provider == provider })
            throw CannotConnectIdentityProviderException("The user already connected to the provider: $provider")

        identities.add(UserIdentity.Companion.ofProvider(provider, principalId, false))

        return userService.save(user)
    }

    suspend fun disconnect(provider: String): UserDocument {
        logger.debug { "Disconnecting provider $provider from user" }

        authorizationService.requireStepUp()

        val user = authorizationService.getCurrentUser()
        val identities = user.sensitive.identities

        if (provider == IdentityProvider.PASSWORD)
            throw CannotDisconnectIdentityProviderException("Password provider cannot be disconnected from user")

        if (identities.none { it.provider == provider })
            throw CannotDisconnectIdentityProviderException("Provider $provider is not connected with user")

        if (identities.size == 1)
            throw CannotDisconnectIdentityProviderException("Provider $provider is the only identity provider")

        identities.removeAll { it.provider == provider }

        return userService.save(user)
    }
}
