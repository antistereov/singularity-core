package io.stereov.singularity.content.article.controller

import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.test.BaseContentTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArticleControllerIntegrationTest : BaseContentTest() {

    @Test fun `setTrusted can only be called by an admin`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val article = save(creator = user)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=false")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isForbidden

    }

    @Test fun `setTrusted can works`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN), groups = listOf(KnownGroups.EDITOR))
        val article = save(creator = user)

        assertFalse(articleService.findByKey(article.key).trusted)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=true")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isOk

        assertTrue(articleService.findByKey(article.key).trusted)

    }

    // TODO: what if article key changes for existing article
}
