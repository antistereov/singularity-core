package io.stereov.singularity.secrets.core.model

import java.time.Instant
import java.util.*

/**
 * Represents a sensitive piece of information, referred to as a "secret."
 *
 * A secret typically contains a key-value pair alongside metadata such as an identifier
 * and the timestamp when the secret was created. This class is utilized in various
 * contexts involving secure storage, retrieval, and caching of sensitive information.
 *
 * @property id A unique identifier for the secret.
 * @property key The secret key associated with the secret, used for decryption.
 * @property value The value of the secret, representing the sensitive information.
 * @property createdAt The timestamp indicating when the secret was created.
 */
data class Secret(
    val id: UUID,
    val key: String,
    val value: String,
    val createdAt: Instant
)
