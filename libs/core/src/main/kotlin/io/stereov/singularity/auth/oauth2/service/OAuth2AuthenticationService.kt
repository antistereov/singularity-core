package io.stereov.singularity.auth.oauth2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.util.*

@Service
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
class OAuth2AuthenticationService(
    private val userService: UserService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val identityProviderService: IdentityProviderService,
    private val accessTokenService: AccessTokenService,
) {

    enum class OAuth2Action {
        LOGIN, CONNECTION, REGISTRATION, GUEST_CONVERSION
    }

    private val logger = KotlinLogging.logger {}

     suspend fun findOrCreateUser(
         oauth2Authentication: OAuth2AuthenticationToken,
         oauth2ProviderConnectionTokenValue: String?,
         stepUpTokenValue: String?,
         exchange: ServerWebExchange,
         stepUp: Boolean,
         locale: Locale?
     ): Pair<UserDocument, OAuth2Action> {
         logger.debug { "Finding or creating user after OAuth2 authentication" }

         val provider = oauth2Authentication.authorizedClientRegistrationId
         val oauth2User = oauth2Authentication.principal

         val principalId = oauth2User.attributes["sub"]?.toString()
             ?: throw OAuth2FlowException(OAuth2ErrorCode.SUB_CLAIM_MISSING,
                 "No sub claim provided by OAuth2 provider.")

         val email = try {
             oauth2User.attributes["email"] as String
         } catch (e: Exception) {
             throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_CLAIM_MISSING,"No email provided from OAuth2 provider.", e)
         }

         val name = oauth2User.attributes["name"] as? String ?: "User"

         val authenticated = runCatching { accessTokenService.extract(exchange) }.isSuccess

         val existingUser = userService.findByIdentityOrNull(provider, principalId)
         if (existingUser != null) return handleLogin(existingUser, authenticated, stepUp) to OAuth2Action.LOGIN

         return when (oauth2ProviderConnectionTokenValue != null) {
             true -> handleConnection(email, provider, principalId, oauth2ProviderConnectionTokenValue, stepUpTokenValue, exchange, locale)
             false -> handleRegistration(name, email, provider, principalId, authenticated) to OAuth2Action.REGISTRATION
         }
    }

    private suspend fun handleLogin(user: UserDocument, authenticated: Boolean, stepUp: Boolean): UserDocument {
        logger.debug { "Handling OAuth2 login for user ${user.id}" }

        if (authenticated && !stepUp)
            throw OAuth2FlowException(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED, "Login via OAuth2 provider failed: user is already authenticated")

        return user
    }

    private suspend fun handleRegistration(
        name: String,
        email: String,
        provider: String,
        principalId: String,
        authenticated: Boolean
    ): UserDocument {
        logger.debug { "Handling registration after OAuth2 registration" }

        if (authenticated)
            throw OAuth2FlowException(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED, "Registration via OAuth2 provider failed: user is already authenticated")

        if (userService.existsByEmail(email))
            throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_ALREADY_REGISTERED,
                "Failed to convert guest to user via OAuth2 provider: email is already registered")

        val user = UserDocument.ofIdentityProvider(
            name = name,
            email = email,
            provider = provider,
            principalId = principalId,
            mailTwoFactorCodeExpiresIn = twoFactorEmailCodeProperties.expiresIn
        )

        return userService.save(user)
    }

    private suspend fun handleConnection(
        email: String,
        provider: String,
        principalId: String,
        oauth2ProviderConnectionToken: String,
        stepUpTokenValue: String?,
        exchange: ServerWebExchange,
        locale: Locale?
    ): Pair<UserDocument, OAuth2Action> {
        logger.debug { "Handling connection" }

        return identityProviderService.connect(
            email,
            provider,
            principalId,
            oauth2ProviderConnectionToken,
            stepUpTokenValue,
            exchange,
            locale
        )
    }
}
