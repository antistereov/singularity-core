package io.stereov.singularity.global.service.cache

import io.stereov.singularity.global.service.cache.exception.model.RedisKeyNotFoundException
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RedisServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var redisService: RedisService

    @BeforeEach
    fun setup() = runBlocking {
        redisService.deleteAll()
    }

    @Test fun `save and get works`() = runTest {
        redisService.saveData("key", "value")

        assertEquals("value", redisService.getDataOrNull("key"))
    }
    @Test fun `save overrides key`() = runTest {
        redisService.saveData("key", "value")
        assertEquals("value", redisService.getDataOrNull("key"))

        redisService.saveData("key", "value2")
        assertEquals("value2", redisService.getDataOrNull("key"))
    }

    @Test fun `delete works`() = runTest {
        redisService.saveData("key", "value")
        assertEquals("value", redisService.getDataOrNull("key"))

        redisService.deleteData("key")
        assertNull(redisService.getDataOrNull("key"))
    }
    @Test fun `delete works if key does not exists`() = runTest {
        redisService.deleteData("key")
    }

    @Test fun `getDataOrNul returns null if no key exists`() = runTest {
        assertNull(redisService.getDataOrNull("key"))
    }
    @Test fun `getData throws error if no key exists`() = runTest {
        assertThrowsExactly(RedisKeyNotFoundException::class.java) {
            runBlocking { redisService.getData("key") }
        }
    }

    @Test fun `deleteAll deletesAll`() = runTest {
        redisService.saveData("key1", "value")
        redisService.saveData("key2", "value")

        assertEquals("value", redisService.getDataOrNull("key1"))
        assertEquals("value", redisService.getDataOrNull("key2"))

        redisService.deleteAll()

        assertNull(redisService.getDataOrNull("key1"))
        assertNull(redisService.getDataOrNull("key2"))
    }
    @Test fun `deleteAll works if no data exist`() = runTest {
        redisService.deleteAll()
    }
}
