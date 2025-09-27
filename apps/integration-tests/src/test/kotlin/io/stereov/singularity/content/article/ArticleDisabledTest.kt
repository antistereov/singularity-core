package io.stereov.singularity.content.article

import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.controller.ArticleManagementController
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBeansOfType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class ArticleDisabledTest : BaseIntegrationTest() {

    @Test fun `should not initialize beans`() = runTest {
        assertTrue(applicationContext.getBeansOfType<ArticleController>().isEmpty())
        assertTrue(applicationContext.getBeansOfType<ArticleManagementController>().isEmpty())
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.content.articles.enable") { false }
        }
    }
}