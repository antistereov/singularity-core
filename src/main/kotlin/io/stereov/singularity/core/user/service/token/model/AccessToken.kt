package io.stereov.singularity.core.user.service.token.model

import org.bson.types.ObjectId

/**
 * # Access token model.
 *
 * This data class represents an access token used for user authentication.
 * It contains the user ID, device ID, and token ID.
 *
 * @property userId The ID of the user associated with the access token.
 * @property deviceId The ID of the device associated with the access token.
 * @property tokenId The ID of the access token.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class AccessToken(
    val userId: ObjectId,
    val deviceId: String,
    val tokenId: String,
)
