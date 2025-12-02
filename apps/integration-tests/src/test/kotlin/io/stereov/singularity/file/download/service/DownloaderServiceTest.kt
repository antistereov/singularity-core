package io.stereov.singularity.file.download.service

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.geolocation.GeoIpDatabaseServiceTest
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import java.io.File

class DownloaderServiceTest() : BaseIntegrationTest() {

    @Autowired
    private lateinit var properties: LocalFileStorageProperties

    @Test
    fun `should download image correctly`() = runTest {
        val user = registerUser()
        val resource = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(resource)
        val key = FileKey("key")
        val metadata =
            fileStorage.upload(authentication = user.authentication, key = key, isPublic = true, file = filePart)
                .getOrThrow()

        val file = File(properties.fileDirectory, metadata.key)

        Assertions.assertTrue(file.exists())

        val url = "http://localhost:${port}/api/assets/${metadata.key}"
        val downloaded = downloadService.download(url).getOrThrow()
        val bytes = dataBufferPublisher.toSingleByteArray(downloaded.content)
        Assertions.assertEquals(resource.length(), bytes.size.toLong())
        org.assertj.core.api.Assertions.assertThat(bytes).isEqualTo(resource.readBytes())
        Assertions.assertEquals(url, downloaded.url)
        Assertions.assertEquals(MediaType.IMAGE_JPEG, downloaded.contentType)

        file.delete()
    }

    @Test
    fun `should download md correctly`() = runTest {
        val user = registerUser()
        val resource = ClassPathResource("files/test.md").file
        val filePart = MockFilePart(resource, type = MediaType.TEXT_MARKDOWN)
        val key = FileKey("key")
        val metadata =
            fileStorage.upload(authentication = user.authentication, key = key, isPublic = true, file = filePart)
                .getOrThrow()
        val url = "http://localhost:${port}/api/assets/${metadata.key}"
        val downloaded = downloadService.download(url).getOrThrow()

        val bytes = dataBufferPublisher.toSingleByteArray(downloaded.content)
        Assertions.assertEquals(resource.length(), bytes.size.toLong())
        org.assertj.core.api.Assertions.assertThat(bytes).isEqualTo(resource.readBytes())
        Assertions.assertEquals(url, downloaded.url)
        Assertions.assertEquals(MediaType.TEXT_MARKDOWN, downloaded.contentType)

        GeoIpDatabaseServiceTest.file.delete()
    }

}