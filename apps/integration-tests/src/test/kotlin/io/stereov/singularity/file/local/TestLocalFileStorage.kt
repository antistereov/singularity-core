package io.stereov.singularity.file.local

import io.stereov.singularity.content.file.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.local.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.time.temporal.ChronoUnit

class TestLocalFileStorage : BaseIntegrationTest() {

    @Autowired
    private lateinit var storage: FileStorage

    @Autowired
    private lateinit var properties: LocalFileStorageProperties

    @Autowired
    private lateinit var metadataService: FileMetadataService

    @BeforeEach
    fun delete() = runBlocking {
        metadataService.deleteAll()
    }

    @Test
    fun `should upload public file`() = runTest {
        val user = registerUser()
        val filePart = MockFilePart(ClassPathResource("files/test-image.jpg"))
        val key = "test-image"

        val metadata = storage.upload(user.info.id, filePart, key, true)
        val file = File(properties.publicPath, metadata.key)

        assertTrue(file.exists())
        val savedMetadata = metadataService.findByKey(metadata.key)

        val metadataWithMillis = metadata.copy(
            createdAt = metadata.createdAt.truncatedTo(ChronoUnit.MILLIS),
            updatedAt = metadata.updatedAt.truncatedTo(ChronoUnit.MILLIS)
        )
        assertEquals(metadataWithMillis, savedMetadata)

        file.delete()
    }
}
