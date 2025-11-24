package io.stereov.singularity.secrets.core.model

import io.stereov.singularity.global.exception.SingularityException
import java.time.Instant

/**
 * Represents information related to the rotation of a secret.
 *
 * Stores metadata about the rotation process, including the timestamp of the current rotation,
 * the timestamp of the last successful rotation, the success status of the rotation, and any
 * exception that occurred during the process.
 *
 * This class is utilized to monitor and report the state of secret rotation activities, providing
 * structured data on each rotation attempt.
 *
 * @property timestamp The timestamp indicating when this rotation attempt was recorded. Defaults to the current instant.
 * @property lastRotation The timestamp of the last successful rotation, or null if no rotation has occurred.
 * @property success A boolean indicating whether the rotation operation was successful.
 * @property error An optional [SingularityException] detailing an error encountered during the rotation, if any.
 */
data class RotationInformation(
    val timestamp: Instant = Instant.now(),
    val lastRotation: Instant?,
    val success: Boolean,
    val error: SingularityException? = null
)
