package io.stereov.singularity.auth.oauth2.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.oauth2.dto.request.AddPasswordAuthenticationRequest
import io.stereov.singularity.auth.oauth2.exception.DisconnectProviderException
import io.stereov.singularity.auth.oauth2.exception.OAuth2FlowException
import io.stereov.singularity.auth.oauth2.exception.SetPasswordException
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode
import io.stereov.singularity.auth.token.exception.OAuth2ProviderConnectionTokenExtractionException
import io.stereov.singularity.auth.token.exception.StepUpTokenExtractionException
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

/**
 * A service responsible for handling identity-related operations, such as setting passwords,
 * disconnecting providers, and connecting OAuth2 providers. This service interacts with various
 * dependencies to manage authentication identities and ensure secure operations.
 */
@Service
class IdentityProviderService(
    private val userService: UserService,
    private val oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
    private val hashService: HashService,
    private val accessTokenCache: AccessTokenCache,
    private val accessTokenService: AccessTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val emailProperties: EmailProperties,
    private val securityAlertService: SecurityAlertService,
    private val securityAlertProperties: SecurityAlertProperties,
    private val principalService: PrincipalService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Sets a password for the user by creating a password identity.
     *
     * If a password identity already exists for the user, the method returns an error indicating
     * that the password authentication is already set up.
     *
     * @param req Contains the request data for adding a password, including the plaintext password.
     * @param user The user for whom the password is being set.
     * @return A [Result] containing the updated [User] on success, or a [SetPasswordException] on failure.
     */
    suspend fun setPassword(
        req: AddPasswordAuthenticationRequest,
        user: User
    ): Result<User, SetPasswordException> = coroutineBinding {
        logger.debug { "Creating password identity" }

        val identities = user.sensitive.identities

        if (identities.password != null) {
            Err(SetPasswordException.PasswordAlreadySet("The user already set up password authentication."))
                .bind()
        }

        val passwordHash = hashService.hashBcrypt(req.password)
            .mapError { ex -> SetPasswordException.Hash("Failed to hash password: ${ex.message}", ex) }
            .bind()

        identities.password = UserIdentity.ofPassword(passwordHash)

        userService.save(user)
            .mapError { when (it) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> SetPasswordException.PostCommitSideEffect("Failed to decrypt user document after successfully saving it to database: ${it.message}", it)
                else -> SetPasswordException.Database("Failed to save user to database: ${it.message}", it)
            } }
            .bind()
    }

    /**
     * Disconnects a specified provider from a user's account.
     *
     * @param provider The name of the provider to be disconnected.
     * @param user The user object whose provider is to be disconnected.
     * @param locale The locale information for customization (can be null).
     * @return A [Result] containing the updated [User] if the operation is successful,
     * or a [DisconnectProviderException] if an error occurs during the process.
     */
    suspend fun disconnect(
        provider: String,
        user: User,
        locale: Locale?
    ): Result<User, DisconnectProviderException> = coroutineBinding {
        logger.debug { "Disconnecting provider $provider from user" }

        val identities = user.sensitive.identities

        if (provider == UserIdentity.PASSWORD_IDENTITY) {
            Err(DisconnectProviderException.CannotDisconnectPassword("Cannot disconnect password provider from user."))
                .bind()
        }

        if (!identities.providers.containsKey(provider)) {
            Err(DisconnectProviderException.ProviderNotFound("Provider $provider is not connected to the user."))
                .bind()
        }

        if (identities.providers.size == 1 && identities.password == null) {
            Err(DisconnectProviderException.CannotDisconnectLastProvider("Cannot disconnect the last provider from the user."))
                .bind()
        }

        identities.providers.remove(provider)

        val savedUser = userService.save(user)
            .mapError { when (it) {
                is SaveEncryptedDocumentException.PostCommitSideEffect -> DisconnectProviderException.PostCommitSideEffect("Failed to decrypt user document after successfully saving it to database: ${it.message}", it)
                else -> DisconnectProviderException.Database("Failed to save user to database: ${it.message}", it)
            } }
            .bind()

        if (emailProperties.enable && securityAlertProperties.oauth2ProviderDisconnected) {
            securityAlertService.sendOAuth2Disconnected(savedUser, provider, locale)
                .mapError { ex -> DisconnectProviderException.PostCommitSideEffect("Failed to send security alert: ${ex.message}", ex) }
        }

        savedUser
    }

    /**
     * Connects a new OAuth2 provider to the current user or converts a guest to a registered user.
     *
     * @param email The email address of the user attempting the connection. If the current user is a guest, this email will be used for conversion.
     * @param provider The OAuth2 provider name to connect.
     * @param principalId The unique identifier of the principal provided by the OAuth2 provider.
     * @param oauth2ProviderConnectionTokenValue The connection token value required to authenticate the provider connection.
     * @param stepUpTokenValue The step-up authentication token value used for verifying higher-level authentication, or null if not applicable.
     * @param exchange The server web exchange containing the current request and response.
     * @param locale The locale used for localizing messages or notifications, or null if not applicable.
     * @return A pair consisting of the updated `User` object and an `OAuth2Action` indicating either a guest conversion or a provider connection.
     * @throws OAuth2FlowException If any validation or processing error occurs (e.g., token validation fails or step-up authentication is missing).
     */
    suspend fun connectProvider(
        email: String,
        provider: String,
        principalId: String,
        oauth2ProviderConnectionTokenValue: String,
        stepUpTokenValue: String?,
        exchange: ServerWebExchange,
        locale: Locale?
    ): Pair<User, OAuth2AuthenticationService.OAuth2Action> {
        logger.debug { "Connecting a new OAuth2 provider $provider to user" }

        val accessToken = accessTokenService.extractOrOAuth2FlowException(exchange)

        val sessionId = accessToken.sessionId
        if (stepUpTokenValue == null)
            throw OAuth2FlowException(
                OAuth2ErrorCode.STEP_UP_MISSING,
                "Failed to connect a new provider to the current user. Step-up authentication missing. Provide it as cookie or request param."
            )

        stepUpTokenService.extract(stepUpTokenValue, accessToken.principalId, sessionId)
            .getOrElse { exception ->
                when (exception) {
                    is StepUpTokenExtractionException.Expired -> throw OAuth2FlowException(
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

        val principal = principalService.findById(accessToken.principalId)
            .getOrThrow { ex -> OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to find user with principal id $principalId: ${ex.message}", ex) }

        val isGuest = principal is Guest

        val user = when (principal) {
            is Guest -> {
                val emailExists = userService.existsByEmail(email)
                    .getOrThrow { ex -> OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to check if email $email is already registered: ${ex.message}", ex)}

                if (emailExists) {
                    throw OAuth2FlowException(OAuth2ErrorCode.EMAIL_ALREADY_REGISTERED,
                        "Failed to convert guest to user via OAuth2 provider: email is already registered")
                }

                accessTokenCache.invalidateAllTokens(accessToken.principalId)
                    .onFailure { ex -> logger.error(ex) { "Failed to invalidate access tokens for guest $principalId"} }

                User.ofProvider(
                    accessToken.principalId,
                    provider,
                    principalId,
                    principal.createdAt,
                    Instant.now(),
                    principal.sensitive.name,
                    email,
                    false,
                    mutableSetOf(),
                    twoFactorEmailCodeProperties.expiresIn,
                    principal.sensitive.sessions,
                )
            }
            is User -> {
                if (principal.sensitive.identities.providers.containsKey(provider))
                    throw OAuth2FlowException(OAuth2ErrorCode.PROVIDER_ALREADY_CONNECTED,
                        "The user already connected the provider $provider")
                principal
            }
        }

        val connectionToken = oAuth2ProviderConnectionTokenService.extract(oauth2ProviderConnectionTokenValue, user)
            .getOrThrow { e ->
                when (e) {
                    is OAuth2ProviderConnectionTokenExtractionException.Expired -> throw OAuth2FlowException(OAuth2ErrorCode.CONNECTION_TOKEN_EXPIRED,
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

        user.sensitive.identities.providers[provider] = UserIdentity.ofProvider(principalId)

        val savedUser = userService.save(user)
            .getOrThrow { OAuth2FlowException(OAuth2ErrorCode.SERVER_ERROR, "Failed to save user after OAuth2 provider connection") }

        if (emailProperties.enable && securityAlertProperties.oauth2ProviderConnected && !isGuest) {
            securityAlertService.sendOAuth2Connected(savedUser, provider, locale)
                .onFailure { ex -> logger.error(ex) { "Failed to send security alert" } }
        }

        val action = when (isGuest) {
            true -> OAuth2AuthenticationService.OAuth2Action.GUEST_CONVERSION
            false -> OAuth2AuthenticationService.OAuth2Action.CONNECTION
        }

        return savedUser to action
    }

}
