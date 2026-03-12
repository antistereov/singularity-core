package io.stereov.singularity.file.core.model

import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.file.core.util.MediaTypeExtensionRegistry
import io.stereov.singularity.global.util.letIf
import org.springframework.http.MediaType
import java.util.*

data class FileKeyHelper(
    val filename: String,
    val mediaType: MediaType?,
    val path: String?,
    val uuid: UUID = UUID.randomUUID()
) {

    private fun generateFilename(): String {
        var calculatedValue = if (path.isNullOrBlank()) "" else path.removeSuffix("/").plus("/")

        val originalFilename = filename.substringBeforeLast(".")
        if (originalFilename.isBlank()) {
            calculatedValue += uuid.toString()
        } else {
            calculatedValue += originalFilename
            calculatedValue += "-$uuid"
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
        generateFilename().addExtension()
    )

    fun toRenditionKey(renditionIdentifier: String = "") = FileRenditionKey(
        generateFilename()
            .addRenditionIdentifier(renditionIdentifier)
            .addExtension()
    )
}