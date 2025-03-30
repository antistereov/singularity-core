package io.stereov.web.user.service.token.model

/**
 * # Refresh token model.
 *
 * This data class represents a refresh token used for user authentication.
 * It contains the account ID, device ID, and the token value.
 *
 * @property accountId The ID of the account associated with the refresh token.
 * @property deviceId The ID of the device associated with the refresh token.
 * @property value The value of the refresh token.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class RefreshToken(
    val accountId: String,
    val deviceId: String,
    val value: String,
)
