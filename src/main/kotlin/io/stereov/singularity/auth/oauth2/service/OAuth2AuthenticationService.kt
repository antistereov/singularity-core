package io.stereov.singularity.auth.oauth2.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.oauth2.exception.model.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
class OAuth2AuthenticationService(
    private val userService: UserService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val identityProviderService: IdentityProviderService,
    private val authorizationService: AuthorizationService,
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
             ?: throw OAuth2FlowException(OAuth2ErrorCode.PRINCIPAL_ID_MISSING,
                 "No principal ID provided from OAuth2 provider.")

         val email = try {
             oauth2User.attributes["email"] as String
         } catch (e: Exception) {
             throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_ATTRIBUTE_MISSING,"No email provided from OAuth2 provider.", e)
         }

         val name = oauth2User.attributes["name"] as? String ?: "User"

         val existingUser = userService.findByIdentityOrNull(provider, principalId)
         if (existingUser != null) {
             if (authorizationService.isAuthenticated())
                 throw OAuth2FlowException(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED, "Login via OAuth2 provider failed: user is already authenticated")

             return existingUser
         }

         return when (oauth2ProviderConnectionTokenValue != null) {
             true -> handleConnection(email, provider, principalId, oauth2ProviderConnectionTokenValue)
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
            mailTwoFactorCodeExpiresIn = twoFactorEmailCodeProperties.expiresIn
        )

        return userService.save(user)
    }

    private suspend fun handleConnection(
        email: String,
        provider: String,
        principalId: String,
        oauth2ProviderConnectionToken: String
    ): UserDocument {
        logger.debug { "Handling connection" }

        return identityProviderService.connect(
            email,
            provider,
            principalId,
            oauth2ProviderConnectionToken
        )
    }
}
