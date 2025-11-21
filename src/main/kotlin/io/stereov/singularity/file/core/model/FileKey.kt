package io.stereov.singularity.file.core.model

import java.util.*

/**
 * Represents a unique identifier for a file, composed of a mandatory filename and optional prefix, suffix, and extension.
 *
 * The `FileKey` generates a unique key internally by combining these components, ensuring uniqueness by appending a randomly
 * generated UUID. The key can include the following parts:
 * - A `prefix` to represent a directory-like structure, which is sanitized to remove leading and trailing slashes.
 * - A `filename` which is always part of the final key.
 * - A `suffix` appended to the filename, preceded by an underscore if provided.
 * - An `extension` appended to the filename, preceded by a dot if provided.
 *
 * The resulting key is composed of these sanitized parts and is used as a unique reference to the file object.
 *
 * The key is immutable once created and is accessible through the `key` property. Additionally, the `toString` method
 * returns the string representation of the key.
 */
data class FileKey(
    private val filename: String,
    private val prefix: String? = null,
    private val suffix: String? = null,
    private val extension: String? = null,
) {
    val key = doGetKey()

    private fun doGetKey(): String {
        val prefixPart = prefix
            ?.removePrefix("/")
            ?.removeSuffix("/")
            ?.let { "$it/" }
            ?: ""
        val suffixPart = suffix
            ?.takeIf { it.isNotBlank() }
            ?.let { "_$it" }
            ?: ""
        val extensionPart = extension
            ?.takeIf { it.isNotBlank() }
            ?.let { ".$it" }
            ?: ""
        return "$prefixPart$filename$suffixPart-${UUID.randomUUID()}$extensionPart"
    }

    override fun toString(): String {
        return key
    }
}
