package io.stereov.singularity.database.hash.model

import java.util.*

/**
 * Represents a searchable hash object containing hashed data and its associated secret identifier.
 *
 * This class is used to encapsulate hashed data alongside a unique identifier to facilitate secure
 * storage and retrieval of sensitive information.
 *
 * @property data The hashed string data.
 * @property secretId The UUID associated with the hashed data, used for identification and search.
 *
 * @see Hash
 */
data class SearchableHash(
    val data: String,
    val secretId: UUID,
)
