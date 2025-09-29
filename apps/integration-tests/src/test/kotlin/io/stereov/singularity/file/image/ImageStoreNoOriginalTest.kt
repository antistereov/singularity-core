package io.stereov.singularity.file.image

import io.stereov.singularity.file.core.exception.model.FileTooLargeException
import io.stereov.singularity.file.core.exception.model.FileUploadException
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File
import kotlin.math.pow

class ImageStoreNoOriginalTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var properties: LocalFileStorageProperties


    @Autowired
    lateinit var imageStore: ImageStore

    @Test fun `save image saves no original`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("key")

        val metadata = imageStore.upload(user.info.id, filePart, key.key, true)

        assertTrue { metadata.renditions.keys.contains(ImageProperties::small.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::medium.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::large.name) }
        assertFalse { metadata.renditions.keys.contains(FileMetadataDocument.ORIGINAL_RENDITION) }

        metadata.renditions.values.forEach { rend ->
            assertTrue { File(properties.fileDirectory, rend.key).exists() }
        }
    }
    @Test fun `save image throws when content to long`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file, length = 10.0.pow(10000).toLong())
        val key = FileKey("key")

        assertThrows<FileTooLargeException> { imageStore.upload(user.info.id, filePart, key.key, true) }
    }
    @Test fun `save image throws when content missing`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file, setLength = false)
        val key = FileKey("key")

        assertThrows<FileUploadException> { imageStore.upload(user.info.id, filePart, key.key, true) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {

            registry.add("singularity.file.storage.image.store-original") { false }
        }
    }
}
