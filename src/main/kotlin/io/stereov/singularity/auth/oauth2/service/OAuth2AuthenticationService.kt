package io.stereov.singularity.auth.oauth2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.twofactor.properties.TwoFactorMailCodeProperties
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service

@Service
class OAuth2AuthenticationService(
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val twoFactorMailCodeProperties: TwoFactorMailCodeProperties,
    private val identityProviderService: IdentityProviderService,
    private val authorizationService: AuthorizationService
) {

    private val logger = KotlinLogging.logger {}

     suspend fun findOrCreateUser(
         oauth2Authentication: OAuth2AuthenticationToken,
         oauth2ProviderConnectionTokenValue: String?
     ): UserDocument {
         logger.debug { "Finding or creating user after OAuth2 authentication" }

         val provider = oauth2Authentication.authorizedClientRegistrationId
         val oauth2User = oauth2Authentication.principal

         val principalId = oauth2User.attributes["id"]?.toString()
             ?: throw OAuth2FlowException("principal_id_missing","No principal ID provided from OAuth2 provider.")

         val email = try {
             oauth2User.attributes["email"] as String
         } catch (e: Exception) {
             throw OAuth2FlowException("email_attribute_missing","No email provided from OAuth2 provider.", e)
         }

         val name = oauth2User.attributes["name"] as? String ?: "User"

         val existingUser = userService.findByIdentityOrNull(provider, principalId)
         if (existingUser != null) {
             if (authorizationService.isAuthenticated())
                 throw OAuth2FlowException("user_already_authenticated", "Login via OAuth2 provider failed: user is already authenticated")

             return existingUser
         }

         return when (userService.existsByEmail(email)) {
             true -> handleConnection(provider, principalId, oauth2ProviderConnectionTokenValue)
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
            mailTwoFactorCodeExpiresIn = twoFactorMailCodeProperties.expiresIn
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
