package io.stereov.singularity.file.local

import io.stereov.singularity.auth.session.model.SessionTokenType
import io.stereov.singularity.file.core.model.FileMetadataDocument
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

    suspend fun runFileTest(public: Boolean = true, key: String = "test-image.jpg", method: suspend (file: File, metadata: FileMetadataDocument, user: TestRegisterResponse) -> Unit) = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = key

        val metadata = storage.upload(user.info.id, filePart, key, public)

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

    @Test
    fun `should upload public file`() = runTest {
        runFileTest { _, metadata, _ ->
            val file = File(properties.fileDirectory, metadata.key)

            assertTrue(file.exists())
            val savedMetadata = metadataService.findByKey(metadata.key)

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
                .cookie(SessionTokenType.Access.cookieName, user.accessToken)
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
        val anotherUser = registerUser(email = "another@email.com")

        runFileTest(false) { _, metadata, _ ->

            webTestClient.get()
                .uri("/api/assets/${metadata.key}")
                .cookie(SessionTokenType.Access.cookieName, anotherUser.accessToken)
                .exchange()
                .expectStatus().isForbidden
        }
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

            val relativeUri = URI(response.url).path

            assertThat(response.url).isEqualTo("http://localhost:8000/api/assets/${metadata.key}")

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

            val relativeUri = URI(response.url).path

            assertThat(response.url).isEqualTo("http://localhost:8000/api/assets/${metadata.key}")

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
        storage.remove("just-a-key")
    }
    @Test fun `exists works`() = runTest {
        assertFalse(storage.exists("just-a-key"))

        runFileTest { _, metadata, _ ->
            assertTrue(storage.exists(metadata.key))
        }
    }
}
