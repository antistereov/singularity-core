package io.stereov.web.global.service.file

import io.mockk.mockk
import io.stereov.web.global.service.file.service.LocalFileStorage
import io.stereov.web.properties.LocalFileStorageProperties
import org.springframework.http.codec.multipart.FilePart

class LocalFileStorageTest {
    private val properties = LocalFileStorageProperties("./tmp/files")
    val service = LocalFileStorage(properties)

    private val filename = "test.txt"
    private val subfolder = "test"
    private val uploadFile = mockk<FilePart>()

    // TODO: recreate tests

//    @BeforeEach
//    fun init() {
//        every { uploadFile.filename() } returns filename
//        every { uploadFile.transferTo(any<Path>()) } answers {
//            val target = firstArg<Path>()
//
//            Mono.fromCallable {
//                Files.write(target, content.toByteArray())
//                null
//            }
//        }
//        every { uploadFile.transferTo(any<File>()) } answers {
//            val target = firstArg<File>()
//
//            Mono.fromCallable {
//                Files.write(target.toPath(), content.toByteArray())
//                null
//            }
//        }
//    }
//
//    @BeforeEach
//    fun deleteFiles() {
//        Path(properties.basePath, subfolder, filename).deleteIfExists()
//    }
//
//    @Test fun `store works`() = runTest {
//        service.storeFile(uploadFile, subfolder)
//
//        assertTrue(Path(properties.basePath, subfolder, filename).toFile().exists())
//    }
//    @Test fun `store needs subfolder of base dir`() = runTest {
//        assertThrows<FileSecurityException> {
//            service.storeFile(uploadFile, "../")
//        }
//    }
//
//    @Test fun `exists works if not exist`() = runTest {
//        assertFalse(service.fileExists(subfolder, filename))
//    }
//    @Test fun `exists works if exists`() = runTest {
//        service.storeFile(uploadFile, subfolder)
//
//        assertTrue(service.fileExists(subfolder, filename))
//    }
//    @Test fun `exists needs subfolder of base dir`() = runTest {
//        assertThrows<FileSecurityException> {
//            service.fileExists("..", filename)
//        }
//    }
//
//    @Test fun `loadFile works`() = runTest {
//        service.storeFile(uploadFile, subfolder)
//
//        val file = service.loadFile(subfolder, filename)
//        file.readText().trim()
//    }
//    @Test fun `loadFile needs existing file`() = runTest {
//        assertThrows<NoSuchFileException> {
//            service.loadFile(subfolder, filename)
//        }
//    }
//    @Test fun `loadFile needs subfolder of base dir`() = runTest {
//        assertThrows<FileSecurityException> {
//            service.loadFile("..", filename)
//        }
//    }
//
//    @Test fun `removeFile works`() = runTest {
//        service.storeFile(uploadFile, subfolder)
//
//        assertTrue(Path(properties.basePath, subfolder, filename).exists())
//
//        service.removeFile(subfolder, filename)
//
//        assertFalse(Path(properties.basePath, subfolder, filename).exists())
//    }
//    @Test fun `removeFile needs existing file`() = runTest{
//        assertThrows<NoSuchFileException> {
//            service.removeFile(subfolder, filename)
//        }
//    }
//    @Test fun `removeFile needs subfolder of base dir`() = runTest {
//        assertThrows<FileSecurityException> {
//            service.removeFile("..", filename)
//        }
//    }
//
//    @Test fun `removeFileIfExists works`() = runTest {
//        service.storeFile(uploadFile, subfolder)
//
//        assertTrue(Path(properties.basePath, subfolder, filename).exists())
//
//        assertTrue(service.removeFileIfExists(subfolder, filename))
//
//        assertFalse(Path(properties.basePath, subfolder, filename).exists())
//    }
//    @Test fun `removeFileIfExists works with non-existing file`() = runTest{
//        assertFalse(service.removeFileIfExists(subfolder, filename))
//    }
//    @Test fun `removeFileIfExists needs subfolder of base dir`() = runTest {
//        assertThrows<FileSecurityException> {
//            service.removeFileIfExists("..", filename)
//        }
//    }
}
