package io.stereov.singularity.content.article.controller

import io.stereov.singularity.core.config.Constants
import io.stereov.singularity.core.user.model.Role
import io.stereov.singularity.test.BaseContentTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArticleControllerIntegrationTest : BaseContentTest() {

    @Test fun `save works`() = runTest {
        val user = registerUser()
        val article = fullArticle.copy(creator = user.info.toOverviewDto())

        webTestClient.put()
            .uri("/api/content/articles")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .bodyValue(article)
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `save requires authentication`() = runTest {
        val user = registerUser()
        val article = fullArticle.copy(creator = user.info.toOverviewDto())

        webTestClient.put()
            .uri("/api/content/articles")
            .bodyValue(article)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `setTrusted can only be called by an admin`() = runTest {
        val user = registerUser()
        val article = save(creator = user)

        webTestClient.put()
            .uri("/api/content/articles/trusted/${article.key}?trusted=false")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isForbidden

    }

    @Test fun `setTrusted can works`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN))
        val article = save(creator = user)

        assertFalse(articleService.findByKey(article.key).trusted)

        webTestClient.put()
            .uri("/api/content/articles/trusted/${article.key}?trusted=true")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isOk

        assertTrue(articleService.findByKey(article.key).trusted)

    }

    // TODO: what if article key changes for existing article
}
