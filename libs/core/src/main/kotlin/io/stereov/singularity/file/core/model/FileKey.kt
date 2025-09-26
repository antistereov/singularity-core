package io.stereov.singularity.file.core.model

import java.util.*

/**
 * Create a file key based on [prefix] (e.g. `/user/123456`),
 * a [filename] (e.g. `avatar`),
 * a [suffix] (e.g. `small`),
 * an [extension] (e.g. `jpeg`).
 *
 * A [UUID] will be attached to make the key unique to prevent cache busting.
 *
 * This will result in `/user/123456/avatar_small-3bb2c18c-....jpeg`.
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