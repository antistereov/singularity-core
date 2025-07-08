package io.stereov.singularity.content.common.content.util

fun String.toSlug(): String {
    return this.trim().lowercase().replace(Regex("\\s+"), "-")
}
