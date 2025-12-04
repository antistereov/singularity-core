package io.stereov.singularity.cache.service

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.cache.exception.CacheException
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.*

class CacheServiceTest : BaseIntegrationTest() {

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
        cacheService.put("key", value).getOrThrow()

        assertEquals(value, cacheService.get<TestData>("key").getOrThrow())
    }
    @Test fun `save overrides key`() = runTest {
        cacheService.put("key", "value").getOrThrow()
        assertEquals("value", cacheService.get<String>("key").getOrThrow())

        cacheService.put("key", "value2").getOrThrow()
        assertEquals("value2", cacheService.get<String>("key").getOrThrow())
    }
    @Test fun `put expiration works`() = runTest {
        cacheService.put("test", "test", 1).getOrThrow()

        assertTrue(cacheService.exists("test").getOrThrow())

        runBlocking { delay(Duration.ofMillis(1100)) }

        assertFalse(cacheService.exists("test").getOrThrow())
    }

    @Test fun `exists works`() = runTest {
        cacheService.put("test", "test").getOrThrow()

        assertTrue(cacheService.exists("test").getOrThrow())
        assertFalse(cacheService.exists("te").getOrThrow())
    }

    @Test fun `delete works`() = runTest {
        cacheService.put("key", "value").getOrThrow()
        assertEquals("value", cacheService.get<String>("key").getOrThrow())

        cacheService.delete("key").getOrThrow()
        assertThrows<CacheException.KeyNotFound> { cacheService.get<String>("key").getOrThrow() }
    }
    @Test fun `delete works if key does not exists`() = runTest {
        cacheService.delete("key")
    }

    @Test fun `getDataOrNul returns null if no key exists`() = runTest {
        assertThrows<CacheException.KeyNotFound> { cacheService.get<String>("key").getOrThrow() }
    }
    @Test fun `getData throws error if no key exists`() = runTest {
        assertThrows<CacheException.KeyNotFound> { cacheService.get<String>("key").getOrThrow() }
    }

    @Test fun `deleteAll deletesAll`() = runTest {
        cacheService.put("key1", "value").getOrThrow()
        cacheService.put("key2", "value").getOrThrow()

        assertEquals("value", cacheService.get<String>("key1").getOrThrow())
        assertEquals("value", cacheService.get<String>("key2").getOrThrow())

        cacheService.deleteAll().getOrThrow()

        assertThrows<CacheException.KeyNotFound> { cacheService.get<String>("key1").getOrThrow() }
        assertThrows<CacheException.KeyNotFound> { cacheService.get<String>("key2").getOrThrow() }
    }
    @Test fun `deleteAll works if no data exist`() = runTest {
        cacheService.deleteAll().getOrThrow()
    }
    @Test fun `deleteAll works with pattern`() = runTest {
        cacheService.put("test:1", "test").getOrThrow()
        cacheService.put("test:2", "test").getOrThrow()
        cacheService.put("te:1", "test").getOrThrow()

        cacheService.deleteAll("test:*").getOrThrow()

        assertFalse(cacheService.exists("test:1").getOrThrow())
        assertFalse(cacheService.exists("test:2").getOrThrow())
        assertTrue(cacheService.exists("te:1").getOrThrow())
    }
}
