package io.stereov.singularity.auth.token.model

import org.bson.types.ObjectId

/**
 * # StepUpToken data class.
 *
 * This data class represents a token used for step-up authentication.
 * It contains the user ID and device ID associated with the token.
 *
 * @property userId The ID of the user associated with the token.
 * @property deviceId The ID of the device associated with the token.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class StepUpToken(
    val userId: ObjectId,
    val deviceId: String,
)
