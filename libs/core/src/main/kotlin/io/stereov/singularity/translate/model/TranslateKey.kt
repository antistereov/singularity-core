package io.stereov.singularity.translate.model

/**
 * Represents a key used for translation in the system.
 *
 * This key is intended to identify specific translations stored in a resource bundle
 * or other translation mechanisms. The key is typically used in conjunction with
 * a locale and resource to fetch the appropriate translated content.
 *
 * @property key The unique identifier for the translation entry.
 */
data class TranslateKey(val key: String)
