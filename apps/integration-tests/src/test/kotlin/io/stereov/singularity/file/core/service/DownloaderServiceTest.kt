package io.stereov.singularity.file.core.service

import io.stereov.singularity.auth.geolocation.GeoIpDatabaseServiceTest.Companion.file
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import java.io.File

class DownloaderServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var properties: LocalFileStorageProperties

    @Test fun `should download image correctly`() = runTest {
        val user = registerUser()
        val resource = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(resource)
        val key = FileKey("key")
        val metadata = fileStorage.upload(ownerId = user.info.id, key = key, isPublic = true, file = filePart)

        val file = File(properties.fileDirectory, metadata.key)

        assertTrue(file.exists())

        val url = "http://localhost:${port}/api/assets/${metadata.key}"
        val downloaded = downloadService.download(url)

        assertEquals(resource.length(), downloaded.size.toLong())
        assertThat(downloaded.bytes).isEqualTo(resource.readBytes())
        assertEquals(url, downloaded.url)
        assertEquals(MediaType.IMAGE_JPEG, downloaded.contentType)

        file.delete()
    }

    @Test fun `should download md correctly`() = runTest {
        val user = registerUser()
        val resource = ClassPathResource("files/test.md").file
        val filePart = MockFilePart(resource, type = MediaType.TEXT_MARKDOWN)
        val key = FileKey("key")
        val metadata = fileStorage.upload(ownerId = user.info.id, key = key, isPublic = true, file = filePart)

        val url = "http://localhost:${port}/api/assets/${metadata.key}"
        val downloaded = downloadService.download(url)

        assertEquals(resource.length(), downloaded.size.toLong())
        assertThat(downloaded.bytes).isEqualTo(resource.readBytes())
        assertEquals(url, downloaded.url)
        assertEquals(MediaType.TEXT_MARKDOWN, downloaded.contentType)

        file.delete()
    }

}