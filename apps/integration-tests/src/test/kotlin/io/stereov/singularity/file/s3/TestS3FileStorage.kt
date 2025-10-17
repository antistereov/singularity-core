package io.stereov.singularity.file.s3

import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.stereov.singularity.auth.geolocation.GeoIpDatabaseServiceTest.Companion.file
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.model.FileTooLargeException
import io.stereov.singularity.file.core.exception.model.FileUploadException
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.properties.StorageType
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.s3.properties.S3Properties
import io.stereov.singularity.file.s3.service.S3FileStorage
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.test.BaseSpringBootTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.net.URI
import java.time.temporal.ChronoUnit
import kotlin.math.pow

class TestS3FileStorage : BaseSpringBootTest() {

    @Autowired
    private lateinit var storage: FileStorage

    @Autowired
    private lateinit var metadataService: FileMetadataService

    @BeforeEach
    fun delete() = runBlocking {
        userService.deleteAll()
        metadataService.deleteAll()
    }

    suspend fun runFileTest(public: Boolean = true, method: suspend (file: File, metadata: FileMetadataResponse, user: TestRegisterResponse) -> Unit) = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey(file.name)
        val metadata = storage.upload(ownerId = user.info.id, key = key, isPublic = public, file = filePart)

        method(file, metadata, user)

        runCatching { storage.remove(metadata.key) }.getOrNull()
    }

    @Test fun `should initialize beans correctly`() {
        applicationContext.getBean<S3Properties>()

        val fileStorage = applicationContext.getBean<FileStorage>()

        assertThat(fileStorage).isOfAnyClassIn(S3FileStorage::class.java)
    }

    @Test fun `should upload public file`() = runTest {
        runFileTest { _, metadata, _ ->
            val savedMetadata = fileStorage.metadataResponseByKey(metadata.key)

            val metadataWithMillis = metadata.copy(
                createdAt = metadata.createdAt.truncatedTo(ChronoUnit.MILLIS),
                updatedAt = metadata.updatedAt.truncatedTo(ChronoUnit.MILLIS)
            )
            assertEquals(metadataWithMillis, savedMetadata)

            val all = metadataService.findAllPaginated(0, 10, emptyList(), null)
            println(all)

            assertTrue(metadataService.existsByKey(metadata.key))
            assertTrue(metadataService.existsRenditionByKey(metadata.key))
        }
    }
    @Test fun `should upload throw error when file too large`() = runTest {
        val user = registerUser()
        val filePart = MockFilePart(file, 10.0.pow(1000).toLong())
        val key = FileKey("key")
        assertThrows<FileTooLargeException> {
            storage.upload(
                ownerId = user.info.id,
                key = key,
                isPublic = true,
                file = filePart
            )
        }

    }
    @Test fun `should upload throw error when content length not set`() = runTest {
        val user = registerUser()
        val filePart = MockFilePart(file, setLength = false)
        val key = FileKey("key")
        assertThrows<FileUploadException> { storage.upload(ownerId = user.info.id, key = key, isPublic = true, file = filePart) }
    }
    @Test fun `creates response with correct url`() = runTest {
        runFileTest { file, metadata, _ ->

            webTestClient.get()
                .uri(metadata.renditions[FileMetadataDocument.ORIGINAL_RENDITION]!!.url)
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
    @Test fun `should upload multiple renditions`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1", extension = "jpg").key

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

        val upload = storage.uploadMultipleRenditions(key, mapOf("1" to req1, "2" to req2), user.info.id, true)

        webTestClient.get()
            .uri(upload.renditions["1"]!!.url)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(file.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(file.readBytes())
            }
        webTestClient.get()
            .uri(upload.renditions["2"]!!.url)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(file.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(file.readBytes())
            }

        assertTrue(metadataService.existsByKey(key))
        assertTrue(metadataService.existsRenditionByKey(req1.key.key))
        assertTrue(metadataService.existsRenditionByKey(req2.key.key))
    }

    @Test fun `remove works`() = runTest {
        runFileTest { _, metadata, _ ->
            storage.remove(metadata.key)

            webTestClient.get()
                .uri(metadata.renditions[FileMetadataDocument.ORIGINAL_RENDITION]!!.url)
                .exchange()
                .expectStatus().isNotFound

            assertFalse(metadataService.existsByKey(metadata.key))
            assertFalse(metadataService.existsRenditionByKey(metadata.key))
            assertFalse(storage.exists(metadata.key))
        }
    }
    @Test fun `remove does nothing when key not existing`() = runTest {
        assertThrows<DocumentNotFoundException> { fileStorage.remove("just-a-key") }
    }
    @Test fun `should removes multiple renditions`() = runTest {
        val user = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1", extension = "jpg")

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

        val upload = storage.uploadMultipleRenditions(key.key, mapOf("1" to req1, "2" to req2), user.info.id, true)

        webTestClient.get()
            .uri(upload.renditions["1"]!!.url)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(file.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(file.readBytes())
            }
        webTestClient.get()
            .uri(upload.renditions["2"]!!.url)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(file.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(file.readBytes())
            }

        assertTrue(metadataService.existsByKey(key.key))
        assertTrue(metadataService.existsRenditionByKey(req1.key.key))
        assertTrue(metadataService.existsRenditionByKey(req2.key.key))

        fileStorage.remove(key)

        webTestClient.get()
            .uri(upload.renditions["1"]!!.url)
            .exchange()
            .expectStatus().isNotFound

        webTestClient.get()
            .uri(upload.renditions["2"]!!.url)
            .exchange()
            .expectStatus().isNotFound

        assertFalse(metadataService.existsByKey(key.key))
        assertFalse(metadataService.existsRenditionByKey(req1.key.key))
        assertFalse(metadataService.existsRenditionByKey(req2.key.key))
    }

    @Test fun `exists works`() = runTest {
        assertFalse(storage.exists("just-a-key"))

        runFileTest { _, metadata, _ ->
            assertTrue(storage.exists(metadata.key))
        }
    }

    companion object {
        val mongoDBContainer = MongoDBContainer("mongo:latest").apply {
            start()
        }

        private val redisContainer = GenericContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .apply {
                start()
            }

        val minioContainer = MinIOContainer("minio/minio:latest")
            .apply {
                start()
            }

        @BeforeAll
        @JvmStatic
        fun initializeBucket() = runBlocking {
            val minioClient: MinioClient = MinioClient
                .builder()
                .endpoint(minioContainer.s3URL)
                .credentials(minioContainer.userName, minioContainer.password)
                .build()

            minioClient.makeBucket(MakeBucketArgs.builder().bucket("app").build())
        }


        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            val uri = URI(minioContainer.s3URL)

            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("singularity.security.rate-limit.user-limit") { 10000 }
            registry.add("singularity.file.storage.type") { StorageType.S3 }
            registry.add("singularity.file.storage.s3.domain") { "${uri.host}:${uri.port}" }
            registry.add("singularity.file.storage.s3.access-key") { minioContainer.userName }
            registry.add("singularity.file.storage.s3.secret-key") { minioContainer.password }
            registry.add("singularity.file.storage.s3.path-style-access-enabled") { true }
        }
    }
}
