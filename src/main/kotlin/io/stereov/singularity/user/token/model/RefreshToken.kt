package io.stereov.singularity.user.token.model

import org.bson.types.ObjectId

/**
 * # Refresh token model.
 *
 * This data class represents a refresh token used for user authentication.
 * It contains the account ID, device ID, and the token value.
 *
 * @property userId The ID of the account associated with the refresh token.
 * @property deviceId The ID of the device associated with the refresh token.
 * @property tokenId The ID of the refresh token.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class RefreshToken(
    val userId: ObjectId,
    val deviceId: String,
    val tokenId: String,
)
