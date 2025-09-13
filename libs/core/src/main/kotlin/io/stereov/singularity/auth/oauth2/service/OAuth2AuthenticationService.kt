package io.stereov.singularity.auth.oauth2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service

@Service
class OAuth2AuthenticationService(
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val twoFactorAuthProperties: TwoFactorAuthProperties,
    private val identityProviderService: IdentityProviderService
) {

    private val logger = KotlinLogging.logger {}

     suspend fun findOrCreateUser(
         oauth2Token: OAuth2AuthenticationToken,
         oauth2ProviderConnectionTokenValue: String?
     ): UserDocument {
         logger.debug { "Finding or creating user after OAuth2 authentication" }

         val provider = oauth2Token.authorizedClientRegistrationId
         val oauth2User = oauth2Token.principal

         val principalId = oauth2User.attributes["id"]?.toString()
             ?: throw OAuth2Exception("No ID provided in OAuth2 request")
         val email = runCatching { oauth2User.attributes["email"] as String }
             .getOrElse { throw OAuth2Exception("No email provided in OAuth2 request") }
         val name = oauth2User.attributes["name"] as? String ?: "GitHub User"

         return userService.findByIdentityOrNull(provider, principalId)
             ?: handleExistingUser(name, email, provider, principalId, oauth2ProviderConnectionTokenValue)
    }

    private suspend fun handleExistingUser(
        name: String,
        email: String,
        provider: String,
        principalId: String,
        oauth2ProviderConnectionToken: String?
    ): UserDocument{
        logger.debug { "Handling existing user" }

        val existingUser = userService.findByIdentityOrNull(provider, principalId)
        if (existingUser != null) return existingUser

        return when (userService.existsByEmail(email)) {
            true -> handleConnection(provider, principalId, oauth2ProviderConnectionToken)
            false -> handleRegistration(name, email, provider, principalId)
        }
    }

    private suspend fun handleRegistration(
        name: String,
        email: String,
        provider: String,
        principalId: String
    ): UserDocument {
        logger.debug { "Handling registration after OAuth2 registration" }

        val user = UserDocument.ofIdentityProvider(
            name = name,
            email = email,
            provider = provider,
            principalId = principalId,
            mailEnabled = appProperties.enableMail,
            mailTwoFactorCodeExpiresIn = twoFactorAuthProperties.mailTwoFactorCodeExpiresIn
        )

        return userService.save(user)
    }

    private suspend fun handleConnection(
        provider: String,
        principalId: String,
        oauth2ProviderConnectionToken: String?
    ): UserDocument {
        logger.debug { "Handling connection" }

        return identityProviderService.connect(provider, principalId, oauth2ProviderConnectionToken)
    }
}
