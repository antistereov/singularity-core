package io.stereov.singularity.content.common.util

fun String.toSlug(): String {
    return this.trim().lowercase().replace(Regex("\\s+"), "-")
}
