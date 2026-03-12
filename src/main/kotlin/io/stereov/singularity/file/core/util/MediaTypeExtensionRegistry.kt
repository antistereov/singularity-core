package io.stereov.singularity.file.core.util

import org.springframework.http.MediaType

object MediaTypeExtensionRegistry {

    private val mapping = mapOf(
        MediaType.IMAGE_JPEG to "jpg",
        MediaType.IMAGE_PNG to "png",
        MediaType("image", "webp") to "webp",

        MediaType("audio", "mpeg") to "mp3",
        MediaType("audio", "wav") to "wav",
        MediaType("audio", "ogg") to "ogg",
        MediaType("audio", "flac") to "flac",

        MediaType.APPLICATION_PDF to "pdf",
        MediaType.APPLICATION_JSON to "json",
        MediaType.TEXT_PLAIN to "txt"
    )

    fun resolve(mediaType: MediaType?): String? {
        if (mediaType == null) return null

        return mapping.entries
            .firstOrNull { mediaType.isCompatibleWith(it.key) }
            ?.value
    }
}