package io.stereov.singularity.admin.core.controller

import io.stereov.singularity.test.BaseSpringBootTest
import io.stereov.singularity.principal.core.model.Role
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class AdminControllerStartupTest : BaseSpringBootTest() {

    @Test fun `root account will be created at start`() = runTest {
        val root = userService.findByEmail("root@email.com")

        Assertions.assertTrue(root.roles.contains(Role.ADMIN))
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

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.app.create-root-user") { "true" }
            registry.add("singularity.app.root-email") { "root@email.com" }
            registry.add("singularity.app.root-password") { "root-password" }
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
        }
    }
}
