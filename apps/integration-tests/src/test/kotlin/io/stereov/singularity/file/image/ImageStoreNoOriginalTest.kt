package io.stereov.singularity.file.image

import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File

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


    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {

            registry.add("singularity.file.storage.image.store-original") { false }
        }
    }
}
