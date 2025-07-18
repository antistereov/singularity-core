package io.stereov.singularity.file.local

import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.local.util.MockFilePart
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

class TestLocalFileStorage : BaseIntegrationTest() {

    @Autowired
    private lateinit var storage: FileStorage

    @Test
    fun `should upload public file`() = runTest {
        val user = registerUser()
        val filePart = MockFilePart(ClassPathResource("files/test-image.jpg"))
        val key = "test-image"

        val metadata = storage.upload(user.info.id, filePart, key, true)


    }
}