package io.stereov.singularity.database.hash.model

/**
 * Represents a hash object containing the hashed data.
 *
 * This class is used to encapsulate the hashed data as a string.
 *
 * @property data The hashed string data.
 *
 * @see SearchableHash
 */
data class Hash(
    val data: String
) {
    override fun toString(): String {
        return data
    }
}
