package io.stereov.singularity.content.article.controller

import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.test.BaseArticleTest
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArticleControllerIntegrationTest : BaseArticleTest() {

    @Test fun `setTrusted can only be called by an admin`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(creator = user)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=false")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus()
            .isForbidden

    }
    @Test fun `setTrusted can works`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN), groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(creator = user)

        assertFalse(articleService.findByKey(article.key).trusted)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=true")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus()
            .isOk

        assertTrue(articleService.findByKey(article.key).trusted)

    }

    // TODO: what if article key changes for existing article
}
