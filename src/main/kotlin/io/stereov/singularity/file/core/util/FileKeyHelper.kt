package io.stereov.singularity.file.core.util

import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.file.core.model.FileRenditionKey
import io.stereov.singularity.global.util.Random
import io.stereov.singularity.global.util.letIf
import io.stereov.singularity.global.util.toSlug
import org.springframework.http.MediaType

data class FileKeyHelper(
    val filename: String,
    val mediaType: MediaType?,
    val path: String?,
) {

    private fun generateFilename(): String {
        var calculatedValue = if (path.isNullOrBlank()) "" else path
            .removePrefix("/")
            .removeSuffix("/")
            .plus("/")
        val random = Random.generateId()

        val originalFilename = filename
            .removePrefix("/")
            .removeSuffix("/")
            .substringBeforeLast(".")

        if (originalFilename.isBlank()) {
            calculatedValue += random
        } else {
            calculatedValue += originalFilename
            calculatedValue += "-$random"
        }

        return calculatedValue
    }

    private fun String.addRenditionIdentifier(renditionIdentifier: String): String {
        return if (renditionIdentifier.isNotBlank()) {
            this.plus("-$renditionIdentifier")
        } else {
            this
        }
    }

    private fun String.addExtension(): String {
        val extension =  filename.substringAfterLast(".", "")
            .removePrefix("/")
            .removeSuffix("/")
            .let { it as String? }
            .letIf(
                { it.isNullOrBlank() },
                { MediaTypeExtensionRegistry.resolve(mediaType) }
            )

        return if (extension?.isNotBlank() == true) {
            this.plus(".$extension")
        } else {
            this
        }
    }

    fun toDocumentKey() = DocumentKey(
        generateFilename()
            .addExtension()
            .toSlug()
    )

    fun toRenditionKey(renditionIdentifier: String = "") = FileRenditionKey(
        generateFilename()
            .addRenditionIdentifier(renditionIdentifier)
            .addExtension()
    )
}