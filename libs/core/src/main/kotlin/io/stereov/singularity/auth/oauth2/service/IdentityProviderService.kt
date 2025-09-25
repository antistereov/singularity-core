package io.stereov.singularity.auth.oauth2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.model.SecurityAlertType
import io.stereov.singularity.auth.core.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.SecurityAlertService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.guest.exception.model.GuestCannotPerformThisActionException
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.oauth2.dto.request.AddPasswordAuthenticationRequest
import io.stereov.singularity.auth.oauth2.exception.model.CannotDisconnectIdentityProviderException
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.exception.model.PasswordIdentityAlreadyAddedException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.model.identity.UserIdentity
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.util.*

@Service
class IdentityProviderService(
    private val userService: UserService,
    private val oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
    private val authorizationService: AuthorizationService,
    private val hashService: HashService,
    private val accessTokenCache: AccessTokenCache,
    private val accessTokenService: AccessTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val emailProperties: EmailProperties,
    private val securityAlertService: SecurityAlertService,
    private val securityAlertProperties: SecurityAlertProperties
) {

    private val logger = KotlinLogging.logger {}

    suspend fun connect(req: AddPasswordAuthenticationRequest): UserDocument {
        logger.debug { "Creating password identity" }

        authorizationService.requireStepUp()

        val user = authorizationService.getUser()

        if (user.isGuest)
            throw GuestCannotPerformThisActionException("Guests cannot add a password identity this way. They need to be converted to a user.")

        val identities = user.sensitive.identities

        if (identities.containsKey(IdentityProvider.PASSWORD))
            throw PasswordIdentityAlreadyAddedException("The user already added password")

        identities[IdentityProvider.PASSWORD] = UserIdentity.ofPassword(hashService.hashBcrypt(req.password))

        return userService.save(user)
    }

    suspend fun connect(
        email: String,
        provider: String,
        principalId: String,
        oauth2ProviderConnectionTokenValue: String,
        stepUpTokenValue: String?,
        exchange: ServerWebExchange,
        locale: Locale?
    ): Pair<UserDocument, OAuth2AuthenticationService.OAuth2Action> {
        logger.debug { "Connecting a new OAuth2 provider $provider to user" }

        val accessToken = accessTokenService.extractOrOAuth2FlowException(exchange)

        val sessionId = accessToken.sessionId
        if (stepUpTokenValue == null)
            throw OAuth2FlowException(
                OAuth2ErrorCode.STEP_UP_MISSING,
                "Failed to connect a new provider to the current user. Step-up authentication missing. Provide it as cookie or request param."
            )

        runCatching { stepUpTokenService.extract(stepUpTokenValue, accessToken.userId, sessionId) }
            .getOrElse { exception ->
                when (exception) {
                    is TokenExpiredException -> throw OAuth2FlowException(
                        OAuth2ErrorCode.STEP_UP_TOKEN_EXPIRED,
                        "Failed to connect a new provider to the current user. StepUpToken expired.",
                        exception
                    )
                    else -> throw OAuth2FlowException(
                        OAuth2ErrorCode.INVALID_STEP_UP_TOKEN,
                        "Failed to connect a new provider to the current user: StepUpToken is invalid",
                        exception
                    )
                }
            }

        val user = userService.findById(accessToken.userId)

        if (user.sensitive.identities.containsKey(provider))
            throw OAuth2FlowException(OAuth2ErrorCode.PROVIDER_ALREADY_CONNECTED,
                "The user already connected the provider $provider")

        val connectionToken = try {
            oAuth2ProviderConnectionTokenService.extract(oauth2ProviderConnectionTokenValue, user)
        } catch(e: Exception) {
            when (e) {
                is TokenExpiredException -> throw OAuth2FlowException(OAuth2ErrorCode.CONNECTION_TOKEN_EXPIRED,
                    "The provided OAuth2ProviderConnectionToken is expired.")
                else -> throw OAuth2FlowException(OAuth2ErrorCode.INVALID_CONNECTION_TOKEN,
                    "The provided OAuth2ProviderConnectionToken cannot be decoded.")
            }
        }

        if (connectionToken.sessionId != sessionId)
            throw OAuth2FlowException(
                OAuth2ErrorCode.INVALID_CONNECTION_TOKEN,
                "The session contained in the OAuth2ConnectionToken does not match the current session"
            )

        if (provider != connectionToken.provider)
            throw OAuth2FlowException(OAuth2ErrorCode.CONNECTION_TOKEN_PROVIDER_MISMATCH,
                "The provided OAuth2ProviderConnectionToken does not match the requested provider.")

        user.sensitive.identities[provider] = UserIdentity.ofProvider(principalId)
        val isGuest = user.isGuest

        if (isGuest) {
            if (userService.existsByEmail(email))
                throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "Failed to convert guest to user via OAuth2 provider: email is already registered")
            user.sensitive.email = email
            user.roles.clear()
            user.roles.add(Role.USER)

            accessTokenCache.invalidateAllTokens(user.id)
        }

        val savedUser = userService.save(user)

        if (emailProperties.enable && securityAlertProperties.oauth2ProviderConnected && !isGuest) {
            securityAlertService.send(savedUser, locale, SecurityAlertType.OAUTH_CONNECTED, provider)
        }
        val action = when (isGuest) {
            true -> OAuth2AuthenticationService.OAuth2Action.GUEST_CONVERSION
            false -> OAuth2AuthenticationService.OAuth2Action.CONNECTION
        }

        return savedUser to action
    }

    suspend fun disconnect(provider: String, locale: Locale?): UserDocument {
        logger.debug { "Disconnecting provider $provider from user" }

        authorizationService.requireStepUp()

        val user = authorizationService.getUser()
        val identities = user.sensitive.identities

        if (provider == IdentityProvider.PASSWORD)
            throw CannotDisconnectIdentityProviderException("Password provider cannot be disconnected from user")

        if (!identities.containsKey(provider))
            throw DocumentNotFoundException("Provider $provider is not connected with user")

        if (identities.size == 1)
            throw CannotDisconnectIdentityProviderException("Provider $provider is the only identity provider")

        identities.remove(provider)

        val savedUser = userService.save(user)

        if (emailProperties.enable && securityAlertProperties.oauth2ProviderDisconnected) {
            securityAlertService.send(savedUser, locale, SecurityAlertType.OAUTH_DISCONNECTED, provider)
        }

        return savedUser
    }
}
