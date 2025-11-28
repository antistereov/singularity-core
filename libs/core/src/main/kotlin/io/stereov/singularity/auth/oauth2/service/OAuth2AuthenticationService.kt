package io.stereov.singularity.auth.oauth2.service

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.flatMapEither
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.recoverIf
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.service.IdentityProviderInfoService
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.oauth2.exception.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.download.service.DownloadService
import io.stereov.singularity.principal.core.exception.FindUserByProviderIdentityException
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.settings.service.PrincipalSettingsService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.util.*
import kotlin.runCatching

/**
 * Service responsible for handling OAuth2 authentication, user registration, and login processes
 * based on the data provided by OAuth2 identity providers.
 *
 * This class supports multiple authentication scenarios, including
 * - Logging in an existing user
 * - Registering a new user
 * - Linking an OAuth2 identity to an existing account
 * - Converting guest users to full accounts using OAuth2
 *
 * It integrates with various services for user account management, token validation,
 * and processing identity-provider-specific details.
 */
@Service
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
class OAuth2AuthenticationService(
    private val userService: UserService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val identityProviderService: IdentityProviderService,
    private val accessTokenService: AccessTokenService,
    private val principalSettingsService: PrincipalSettingsService,
    private val downloadService: DownloadService,
    private val identityProviderInfoService: IdentityProviderInfoService,
    private val emailProperties: EmailProperties,
) {

    enum class OAuth2Action {
        LOGIN, CONNECTION, REGISTRATION, GUEST_CONVERSION
    }

    private val logger = KotlinLogging.logger {}

    /**
     * Finds an existing user by their OAuth2 provider identity or creates a new user depending on the authentication result.
     * Handles the process of linking or registering a user account based on the provided OAuth2 authentication details.
     *
     * @param oauth2Authentication The OAuth2 authentication token containing provider and user details.
     * @param oauth2ProviderConnectionTokenValue Optional token value used for connecting an existing account to the OAuth2 provider.
     * @param stepUpTokenValue Optional step-up token value used for additional authentication steps.
     * @param exchange The server web exchange containing the current HTTP request and response.
     * @param stepUp A flag indicating whether step-up authentication is required.
     * @param locale Optional locale to customize registration behavior based on the user's language or region.
     * @return A pair consisting of the user object and the corresponding OAuth2 action performed (LOGIN or REGISTRATION).
     * @throws OAuth2FlowException If required, OAuth2 claims are missing or there is an issue finding or creating the user.
     */
     suspend fun findOrCreateUser(
         oauth2Authentication: OAuth2AuthenticationToken,
         oauth2ProviderConnectionTokenValue: String?,
         stepUpTokenValue: String?,
         exchange: ServerWebExchange,
         stepUp: Boolean,
         locale: Locale?
     ): Pair<User, OAuth2Action> {
         logger.debug { "Finding or creating user after OAuth2 authentication" }

         val provider = oauth2Authentication.authorizedClientRegistrationId
         val oauth2User = oauth2Authentication.principal

         val principalId = oauth2User.attributes["sub"]?.toString()
             ?: oauth2User.attributes["id"]?.toString()
             ?: throw OAuth2FlowException(OAuth2ErrorCode.SUB_CLAIM_MISSING,
                 "No sub claim provided by OAuth2 provider.")

         val email = try {
             oauth2User.attributes["email"] as String
         } catch (e: Exception) {
             throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_CLAIM_MISSING,"No email provided from OAuth2 provider.", e)
         }

         val name = oauth2User.attributes["name"] as? String ?: "User"

         val authenticated = runCatching { accessTokenService.extract(exchange) }.isSuccess

         val existingUser = userService.findByProviderIdentity(provider, principalId)
             .recoverIf({ it is FindUserByProviderIdentityException.NotFound }, { null })
             .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to find user by provider identity") }

         if (existingUser != null) return handleLogin(existingUser, authenticated, stepUp) to OAuth2Action.LOGIN

         return when (oauth2ProviderConnectionTokenValue != null) {
             true -> handleConnection(email, provider, principalId, oauth2ProviderConnectionTokenValue, stepUpTokenValue, exchange, locale)
             false -> handleRegistration(name, email, provider, principalId, authenticated, oauth2User, locale) to OAuth2Action.REGISTRATION
         }
    }

    /**
     * Handles the OAuth2 login process for a given user.
     *
     * @param user The user attempting to log in.
     * @param authenticated A flag indicating whether the user is already authenticated.
     * @param stepUp A flag indicating whether step-up authentication is required.
     * @return The user after processing the login.
     * @throws OAuth2FlowException if the user is already authenticated and step-up is not required.
     */
    private suspend fun handleLogin(user: User, authenticated: Boolean, stepUp: Boolean): User {
        logger.debug { "Handling OAuth2 login for user ${user.id}" }

        if (authenticated && !stepUp)
            throw OAuth2FlowException(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED, "Login via OAuth2 provider failed: user is already authenticated")

        return user
    }

    /**
     * Handles the registration process after an OAuth2 authentication attempt.
     *
     * This method ensures that users are properly registered when authenticating via an OAuth2 provider.
     * If the user is already authenticated, it throws an exception.
     * It also checks if the email is already registered, sends identity provider information if applicable,
     * saves the user, and optionally sets the user's avatar if available in the OAuth2 user attributes.
     *
     * @param name The name of the user provided by the OAuth2 provider.
     * @param email The email of the user provided by the OAuth2 provider.
     * @param provider The name of the OAuth2 provider (e.g., Google, Facebook).
     * @param principalId The unique identifier for the user in the OAuth2 provider.
     * @param authenticated Whether the user is already authenticated in the system.
     * @param oauth2User The OAuth2User instance containing attributes provided by the OAuth2 provider.
     * @param locale The locale of the user, used for locale-sensitive information such as email notifications. Can be null.
     * @return The registered User instance after saving it to the database.
     * @throws OAuth2FlowException If the user is already authenticated, the email is already registered, or if any other errors occur during the registration process.
     */
    private suspend fun handleRegistration(
        name: String,
        email: String,
        provider: String,
        principalId: String,
        authenticated: Boolean,
        oauth2User: OAuth2User,
        locale: Locale?
    ): User {
        logger.debug { "Handling registration after OAuth2 registration" }

        if (authenticated) throw OAuth2FlowException(OAuth2ErrorCode.USER_ALREADY_AUTHENTICATED, "Registration via OAuth2 provider failed: user is already authenticated")

        val emailExists = userService.existsByEmail(email)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to check if email $email is already registered") }

        if (emailExists) {
            if (emailProperties.enable) {
                val user = userService.findByEmail(email)
                    .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to find user with email $email") }
                identityProviderInfoService.send(user, locale)
                    .onFailure { ex -> logger.error(ex) { "Failed to send identity provider info"} }
            }

            throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_ALREADY_REGISTERED,
                "Failed to convert guest to user via OAuth2 provider: email is already registered")
        }

        val user = User.ofProvider(
            name = name,
            email = email,
            provider = provider,
            principalId = principalId,
            mailTwoFactorCodeExpiresIn = twoFactorEmailCodeProperties.expiresIn
        )

        val savedUser = userService.save(user)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to save user after OAuth2 registration") }

        val avatarUrl = (oauth2User.attributes["picture"] as? String)
            ?: (oauth2User.attributes["avatar_url"] as? String)

        return if (avatarUrl == null) {
            logger.debug { "No avatar set in social login" }
            savedUser
        } else {
            val result = downloadService.download(avatarUrl)
                .flatMapEither(
                    { avatar ->
                        val userId = savedUser.id.getOrThrow { ex ->
                            OAuth2FlowException(
                                OAuth2ErrorCode.SERVER_ERROR,
                                "Failed to get user id after OAuth2 registration",
                                ex
                            )
                        }
                        val authentication = AuthenticationOutcome.Authenticated(
                            userId,
                            savedUser.roles,
                            savedUser.groups,
                            UUID.randomUUID(),
                            ""
                        )

                        val user = principalSettingsService.setAvatar(avatar, savedUser, authentication)
                            .getOrElse { savedUser }

                        Ok(user)
                    },
                    { ex ->
                        logger.debug(ex) { "Failed to download avatar" }
                        Ok(savedUser)
                    }
                )

            result.getOrThrow()
        }

    }

    /**
     * Handles the connection process with the given parameters, delegating to the identity provider service.
     *
     * @param email The email address of the user attempting to connect.
     * @param provider The name of the identity provider.
     * @param principalId The unique identifier of the principal at the identity provider.
     * @param oauth2ProviderConnectionToken The OAuth2 token used for connection authentication.
     * @param stepUpTokenValue An optional token for additional authentication (Step-Up token), or null if not provided.
     * @param exchange The web exchange object containing the HTTP request and response.
     * @param locale An optional locale for the connection, or null if not specified.
     * @return A pair consisting of the connected user and the resulting OAuth2 action.
     */
    private suspend fun handleConnection(
        email: String,
        provider: String,
        principalId: String,
        oauth2ProviderConnectionToken: String,
        stepUpTokenValue: String?,
        exchange: ServerWebExchange,
        locale: Locale?
    ): Pair<User, OAuth2Action> {
        logger.debug { "Handling connection" }

        return identityProviderService.connectProvider(
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
