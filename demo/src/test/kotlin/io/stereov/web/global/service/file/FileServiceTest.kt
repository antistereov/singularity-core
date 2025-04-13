package io.stereov.web.global.service.file

import io.mockk.every
import io.mockk.mockk
import io.stereov.web.global.service.file.exception.model.FileSecurityException
import io.stereov.web.global.service.file.exception.model.NoSuchFileException
import io.stereov.web.global.service.file.service.FileService
import io.stereov.web.properties.AppProperties
import io.stereov.web.properties.FileProperties
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull.content
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class FileServiceTest {
    private val properties = FileProperties("./tmp/files")
    private val appProperties = AppProperties()
    val service = FileService(properties, appProperties)

    private val filename = "test.txt"
    private val subfolder = "test"
    private val uploadFile = mockk<FilePart>()

    @BeforeEach
    fun init() {
        every { uploadFile.filename() } returns filename
        every { uploadFile.transferTo(any<Path>()) } answers {
            val target = firstArg<Path>()

            Mono.fromCallable {
                Files.write(target, content.toByteArray())
                null
            }
        }
        every { uploadFile.transferTo(any<File>()) } answers {
            val target = firstArg<File>()

            Mono.fromCallable {
                Files.write(target.toPath(), content.toByteArray())
                null
            }
        }
    }

    @BeforeEach
    fun deleteFiles() {
        Path(properties.basePath, subfolder, filename).deleteIfExists()
    }

    @Test fun `store works`() = runTest {
        service.storeFile(uploadFile, subfolder)

        assertTrue(Path(properties.basePath, subfolder, filename).toFile().exists())
    }
    @Test fun `store needs subfolder of base dir`() = runTest {
        assertThrows<FileSecurityException> {
            service.storeFile(uploadFile, "../")
        }
    }

    @Test fun `exists works if not exist`() = runTest {
        assertFalse(service.fileExists(subfolder, filename))
    }
    @Test fun `exists works if exists`() = runTest {
        service.storeFile(uploadFile, subfolder)

        assertTrue(service.fileExists(subfolder, filename))
    }
    @Test fun `exists needs subfolder of base dir`() = runTest {
        assertThrows<FileSecurityException> {
            service.fileExists("..", filename)
        }
    }

    @Test fun `loadFile works`() = runTest {
        service.storeFile(uploadFile, subfolder)

        val file = service.loadFile(subfolder, filename)
        file.readText().trim()
    }
    @Test fun `loadFile needs existing file`() = runTest {
        assertThrows<NoSuchFileException> {
            service.loadFile(subfolder, filename)
        }
    }
    @Test fun `loadFile needs subfolder of base dir`() = runTest {
        assertThrows<FileSecurityException> {
            service.loadFile("..", filename)
        }
    }

    @Test fun `removeFile works`() = runTest {
        service.storeFile(uploadFile, subfolder)

        assertTrue(Path(properties.basePath, subfolder, filename).exists())

        service.removeFile(subfolder, filename)

        assertFalse(Path(properties.basePath, subfolder, filename).exists())
    }
    @Test fun `removeFile needs existing file`() = runTest{
        assertThrows<NoSuchFileException> {
            service.removeFile(subfolder, filename)
        }
    }
    @Test fun `removeFile needs subfolder of base dir`() = runTest {
        assertThrows<FileSecurityException> {
            service.removeFile("..", filename)
        }
    }

    @Test fun `removeFileIfExists works`() = runTest {
        service.storeFile(uploadFile, subfolder)

        assertTrue(Path(properties.basePath, subfolder, filename).exists())

        assertTrue(service.removeFileIfExists(subfolder, filename))

        assertFalse(Path(properties.basePath, subfolder, filename).exists())
    }
    @Test fun `removeFileIfExists works with non-existing file`() = runTest{
        assertFalse(service.removeFileIfExists(subfolder, filename))
    }
    @Test fun `removeFileIfExists needs subfolder of base dir`() = runTest {
        assertThrows<FileSecurityException> {
            service.removeFileIfExists("..", filename)
        }
    }
}
