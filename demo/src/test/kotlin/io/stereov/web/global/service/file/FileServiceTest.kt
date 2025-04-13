package io.stereov.web.global.service.file

import io.stereov.web.global.service.file.exception.model.FileSecurityException
import io.stereov.web.global.service.file.exception.model.NoSuchFileException
import io.stereov.web.global.service.file.service.FileService
import io.stereov.web.properties.FileProperties
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockMultipartFile
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class FileServiceTest {
    private val properties = FileProperties("./tmp/files")
    val service = FileService(properties)

    private val filename = "test.txt"
    private val subfolder = "test"
    private val fileContent = "This is a test text file."
    private val uploadFile = MockMultipartFile("file", filename, "text/plain", fileContent.toByteArray())

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
        val content = StringBuilder()

        file.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                content.append(line).append("\n")
            }
        }

        assertEquals(fileContent, content.toString().trim())
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
