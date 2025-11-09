package io.stereov.singularity.file.image

import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.core.model.DownloadedFile
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import java.io.File

class ImageStoreTest() : BaseIntegrationTest() {

    @Autowired
    private lateinit var properties: LocalFileStorageProperties

    @Autowired
    private lateinit var imageProperties: ImageProperties


    @Autowired
    lateinit var imageStore: ImageStore

    @Test fun `save image works`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("key")

        val metadata = imageStore.upload(user.info.id, filePart, key.key, true)

        assertTrue { metadata.renditions.keys.contains(ImageProperties::small.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::medium.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::large.name) }
        assertTrue { metadata.renditions.keys.contains(FileMetadataDocument.ORIGINAL_RENDITION) }

        metadata.renditions.values.forEach { rend ->
            assertTrue { File(properties.fileDirectory, rend.key).exists() }
        }
        assertTrue { metadata.renditions[ImageProperties::small.name]!!.height!! >= imageProperties.small }
        assertTrue { metadata.renditions[ImageProperties::small.name]!!.width!! >= imageProperties.small }
    }
    @Test fun `save image doesn't scale up small images`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("key")

        val metadata = imageStore.upload(user.info.id, filePart, key.key, true)

        assertTrue { metadata.renditions.keys.contains(ImageProperties::small.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::medium.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::large.name) }
        assertTrue { metadata.renditions.keys.contains(FileMetadataDocument.ORIGINAL_RENDITION) }

        metadata.renditions.values.forEach { rend ->
            assertTrue { File(properties.fileDirectory, rend.key).exists() }
        }
        assertTrue { imageProperties.large > 640 }
        assertTrue { metadata.renditions[ImageProperties::large.name]!!.height!! == 424 }
        assertTrue { metadata.renditions[ImageProperties::large.name]!!.width!! == 640 }
    }
    @Test fun `save image throws when wrong content type`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file, type = MediaType.APPLICATION_JSON)
        val key = FileKey("key")

        assertThrows<UnsupportedMediaTypeException> { imageStore.upload(user.info.id, filePart, key.key, true) }
    }
    @Test fun `save image throws when wrong content type not set`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file, setType = false)
        val key = FileKey("key")

        assertThrows<UnsupportedMediaTypeException> { imageStore.upload(user.info.id, filePart, key.key, true) }
    }

    @Test fun `save image works with downloaded file`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val key = FileKey("key")
        val downloadedFile = DownloadedFile(bytes = file.readBytes(), contentType = MediaType.IMAGE_JPEG, url = "")

        val metadata = imageStore.upload(user.info.id, downloadedFile, key.key, true)

        assertTrue { metadata.renditions.keys.contains(ImageProperties::small.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::medium.name) }
        assertTrue { metadata.renditions.keys.contains(ImageProperties::large.name) }
        assertTrue { metadata.renditions.keys.contains(FileMetadataDocument.ORIGINAL_RENDITION) }

        metadata.renditions.values.forEach { rend ->
            assertTrue { File(properties.fileDirectory, rend.key).exists() }
        }
    }
    @Test fun `save image throws when wrong content type with downloaded file`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val downloadedFile = DownloadedFile(bytes = file.readBytes(), contentType = MediaType.APPLICATION_OCTET_STREAM, url = "")
        val key = FileKey("key")

        assertThrows<UnsupportedMediaTypeException> { imageStore.upload(user.info.id, downloadedFile, key.key, true) }
    }
}
