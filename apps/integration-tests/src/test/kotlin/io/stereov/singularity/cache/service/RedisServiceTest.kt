package io.stereov.singularity.cache.service

import io.stereov.singularity.cache.exception.model.RedisKeyNotFoundException
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.*

class RedisServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var cacheService: CacheService

    @BeforeEach
    fun setup() = runBlocking {
        cacheService.deleteAll()
    }

    data class TestData(
        val id: UUID = UUID.randomUUID(),
        val createdAt: Instant = Instant.now(),
        val extra: ExtraData = ExtraData(),
        val name: String = "name"
    )
    data class ExtraData(
        val age: Int = 12,
        val favoriteFloat: Float = 12.0f,
    )

    @Test fun `save and get works`() = runTest {
        val value = TestData()
        cacheService.put("key", value)

        assertEquals(value, cacheService.getOrNull<TestData>("key"))
    }
    @Test fun `save overrides key`() = runTest {
        cacheService.put("key", "value")
        assertEquals("value", cacheService.getOrNull("key"))

        cacheService.put("key", "value2")
        assertEquals("value2", cacheService.getOrNull("key"))
    }

    @Test fun `delete works`() = runTest {
        cacheService.put("key", "value")
        assertEquals("value", cacheService.getOrNull("key"))

        cacheService.delete("key")
        assertNull(cacheService.getOrNull("key"))
    }
    @Test fun `delete works if key does not exists`() = runTest {
        cacheService.delete("key")
    }

    @Test fun `getDataOrNul returns null if no key exists`() = runTest {
        assertNull(cacheService.getOrNull("key"))
    }
    @Test fun `getData throws error if no key exists`() = runTest {
        assertThrowsExactly(RedisKeyNotFoundException::class.java) {
            runBlocking { cacheService.get("key") }
        }
    }

    @Test fun `deleteAll deletesAll`() = runTest {
        cacheService.put("key1", "value")
        cacheService.put("key2", "value")

        assertEquals("value", cacheService.getOrNull("key1"))
        assertEquals("value", cacheService.getOrNull("key2"))

        cacheService.deleteAll()

        assertNull(cacheService.getOrNull("key1"))
        assertNull(cacheService.getOrNull("key2"))
    }
    @Test fun `deleteAll works if no data exist`() = runTest {
        cacheService.deleteAll()
    }
}
