package io.stereov.singularity.file.local

import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.controller.LocalFileStorageController
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.local.service.LocalFileStorage
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import java.io.File
import java.net.URI
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

    suspend fun runFileTest(public: Boolean = true, key: String = "test-image.jpg", method: suspend (file: File, metadata: FileMetadataResponse, user: TestRegisterResponse) -> Unit) = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey(key)
        val metadata = storage.upload(ownerId = user.info.id, key = key, isPublic = public, file = filePart)

        val uploadedFile = File(properties.fileDirectory, metadata.key)
        method(uploadedFile, metadata, user)

        uploadedFile.delete()
    }

    @Test fun `should initialize beans correctly`() {
        applicationContext.getBean<LocalFileStorageProperties>()
        applicationContext.getBean<LocalFileStorageController>()

        val fileStorage = applicationContext.getBean<FileStorage>()

        assertThat(fileStorage).isOfAnyClassIn(LocalFileStorage::class.java)
    }

    @Test fun `should upload public file`() = runTest {
        runFileTest { _, metadata, _ ->
            val file = File(properties.fileDirectory, metadata.key)

            assertTrue(file.exists())
            val savedMetadata = fileStorage.metadataResponseByKey(metadata.key)

            val metadataWithMillis = metadata.copy(
                createdAt = metadata.createdAt.truncatedTo(ChronoUnit.MILLIS),
                updatedAt = metadata.updatedAt.truncatedTo(ChronoUnit.MILLIS)
            )
            assertEquals(metadataWithMillis, savedMetadata)
        }
    }
    @Test fun `should serve public file`() = runTest {
        runFileTest { file, metadata, _ ->

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectHeader().contentLength(file.length())
                .expectBody()
                .consumeWith {
                    assertThat(it.responseBody).isEqualTo(file.readBytes())
                }

        }
    }
    @Test fun `should serve private file`() = runTest {
        runFileTest(false) { file, metadata, user ->

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .accessTokenCookie(user.accessToken)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectHeader().contentLength(file.length())
                .expectBody()
                .consumeWith {
                    assertThat(it.responseBody).isEqualTo(file.readBytes())
                }

        }
    }
    @Test fun `should not serve private file publicly`() = runTest {
        runFileTest(false) { _, metadata, _ ->

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }
    @Test fun `should not serve private file to non-owner`() = runTest {
        val anotherUser = registerUser(emailSuffix = "another@email.com")

        runFileTest(false) { _, metadata, _ ->

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .accessTokenCookie(anotherUser.accessToken)
                .exchange()
                .expectStatus().isForbidden
        }
    }
    @Test fun `should upload multiple renditions`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = "file1.jpg"

        val req1 = FileUploadRequest.FilePartUpload(
            key = FileKey("file1_small", extension = "jpg"),
            contentType = MediaType.IMAGE_JPEG.toString(),
            data = filePart,
        )
        val req2 = FileUploadRequest.FilePartUpload(
            key = FileKey("file2_small", extension = "jpg"),
            contentType = MediaType.IMAGE_JPEG.toString(),
            data = filePart,
        )

        storage.uploadMultipleRenditions(key, mapOf("1" to req1, "2" to req2), user.info.id, true)

        val file1 = File(properties.fileDirectory, req1.key.key)
        val file2 = File(properties.fileDirectory, req2.key.key)

        assertTrue { file1.exists() }
        assertTrue { file2.exists() }
        assertFalse { File(properties.fileDirectory, key).exists() }
        assertThat(file1.readBytes()).isEqualTo(file.readBytes())
        assertThat(file2.readBytes()).isEqualTo(file.readBytes())

        assertTrue(metadataService.existsByKey(key))
        assertTrue(metadataService.existsRenditionByKey(req1.key.key))
        assertTrue(metadataService.existsRenditionByKey(req2.key.key))
    }

    @Test fun `requires key`() = runTest {
        webTestClient.get()
            .uri("/api/assets/")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `should prohibit path injection attacks`() = runTest {
        webTestClient.get()
            .uri("/api/assets/hehe/../../test.jpg")
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `returns not found when no file found`() = runTest {
        webTestClient.get()
            .uri("/api/assets/test.jpg")
            .exchange()
            .expectStatus().isNotFound
    }
    @Test fun `returns not found when no database entry is removed but file exists`() = runTest {
        runFileTest { file, metadata, _ ->
            metadataService.deleteByKey(metadata.key)

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .exchange()
                .expectStatus().isNotFound

            // also deletes file for consistency
            assertFalse { file.exists() }
        }
    }
    @Test fun `returns not found when no file is deleted but db entry exists`() = runTest {
        runFileTest { file, metadata, _ ->
            file.delete()

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .exchange()
                .expectStatus().isNotFound

            // also deletes database entry for consistency
            assertThrows<DocumentNotFoundException> { metadataService.findByKey(metadata.key) }
        }
    }

    @Test fun `creates response with correct url`() = runTest {
        runFileTest { file, metadata, _ ->
            val response = storage.metadataResponseByKey(metadata.key)

            val relativeUri = URI(response.renditions.values.first().url).path

            assertThat(response.renditions.values.first().url).isEqualTo("http://localhost:8000/api/assets/${metadata.key}")

            webTestClient.get()
                .uri(relativeUri)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectHeader().contentLength(file.length())
                .expectBody()
                .consumeWith {
                    assertThat(it.responseBody).isEqualTo(file.readBytes())
                }
        }
    }
    @Test fun `creates response with correct url when in subdirectory`() = runTest {
        runFileTest(key = "sub/dir/test-image.jpg") { file, metadata, _ ->
            val response = storage.metadataResponseByKey(metadata.key)

            val relativeUri = URI(response.renditions.values.first().url).path

            assertThat(response.renditions.values.first().url).isEqualTo("http://localhost:8000/api/assets/${metadata.key}")

            webTestClient.get()
                .uri(relativeUri)
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType(MediaType.IMAGE_JPEG)
                .expectHeader().contentLength(file.length())
                .expectBody()
                .consumeWith {
                    assertThat(it.responseBody).isEqualTo(file.readBytes())
                }
        }
    }

    @Test fun `remove works`() = runTest {
        runFileTest { file, metadata, _ ->
            storage.remove(metadata.key)

            assertFalse(metadataService.existsByKey(metadata.key))
            assertFalse(storage.exists(metadata.key))

            assertFalse(file.exists())
        }
    }
    @Test fun `remove does nothing when key not existing`() = runTest {
        assertThrows<DocumentNotFoundException> { fileStorage.remove("just-a-key") }
    }
    @Test fun `remove should remove multiple renditions`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = "file1.jpg"

        val req1 = FileUploadRequest.FilePartUpload(
            key = FileKey("file1_small", extension = "jpg"),
            contentType = MediaType.IMAGE_JPEG.toString(),
            data = filePart,
        )
        val req2 = FileUploadRequest.FilePartUpload(
            key = FileKey("file2_small", extension = "jpg"),
            contentType = MediaType.IMAGE_JPEG.toString(),
            data = filePart,
        )

        storage.uploadMultipleRenditions(key, mapOf("1" to req1, "2" to req2), user.info.id, true)

        val file1 = File(properties.fileDirectory, req1.key.key)
        val file2 = File(properties.fileDirectory, req2.key.key)
        val nonExisting = File(properties.fileDirectory, key)

        assertTrue { file1.exists() }
        assertTrue { file2.exists() }
        assertFalse { nonExisting.exists() }
        assertThat(file1.readBytes()).isEqualTo(file.readBytes())
        assertThat(file2.readBytes()).isEqualTo(file.readBytes())

        assertTrue(metadataService.existsByKey(key))
        assertTrue(metadataService.existsRenditionByKey(req1.key.key))
        assertTrue(metadataService.existsRenditionByKey(req2.key.key))

        fileStorage.remove(key)

        assertFalse { file1.exists() }
        assertFalse { file2.exists() }
        assertFalse { nonExisting.exists() }
        assertFalse(metadataService.existsByKey(key))
        assertFalse(metadataService.existsRenditionByKey(req1.key.key))
        assertFalse(metadataService.existsRenditionByKey(req2.key.key))
    }

    @Test fun `exists works`() = runTest {
        assertFalse(storage.exists("just-a-key"))

        runFileTest { _, metadata, _ ->
            assertTrue(storage.exists(metadata.key))
            storage.remove(metadata.key)
            assertFalse(storage.exists(metadata.key))
        }
    }
    @Test fun `exists removes metadata when file not found`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = "file1.jpg"

        val req1 = FileUploadRequest.FilePartUpload(
            key = FileKey("file1_small", extension = "jpg"),
            contentType = MediaType.IMAGE_JPEG.toString(),
            data = filePart,
        )
        val req2 = FileUploadRequest.FilePartUpload(
            key = FileKey("file2_small", extension = "jpg"),
            contentType = MediaType.IMAGE_JPEG.toString(),
            data = filePart,
        )

        storage.uploadMultipleRenditions(key, mapOf("1" to req1, "2" to req2), user.info.id, true)

        val file1 = File(properties.fileDirectory, req1.key.key)
        val file2 = File(properties.fileDirectory, req2.key.key)
        val nonExisting = File(properties.fileDirectory, key)

        assertTrue { file1.exists() }
        assertTrue { file2.exists() }
        assertFalse { nonExisting.exists() }
        assertThat(file1.readBytes()).isEqualTo(file.readBytes())
        assertThat(file2.readBytes()).isEqualTo(file.readBytes())

        assertTrue(metadataService.existsByKey(key))
        assertTrue(metadataService.existsRenditionByKey(req1.key.key))
        assertTrue(metadataService.existsRenditionByKey(req2.key.key))

        file1.delete()
        fileStorage.exists(key)

        assertFalse { file1.exists() }
        assertFalse { file2.exists() }
        assertFalse { nonExisting.exists() }
        assertFalse(metadataService.existsByKey(key))
        assertFalse(metadataService.existsRenditionByKey(req1.key.key))
        assertFalse(metadataService.existsRenditionByKey(req2.key.key))
    }
    @Test fun `exists removes file when metadata not found`() = runTest {
        runFileTest { file, metadata, _ ->
            assertTrue(storage.exists(metadata.key))

            metadataService.deleteByKey(metadata.key)
            storage.exists(metadata.key)

            assertFalse { file.exists() }
            assertFalse(storage.exists(metadata.key))
        }
    }
}
