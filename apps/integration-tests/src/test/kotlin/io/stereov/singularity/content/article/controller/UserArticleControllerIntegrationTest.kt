package io.stereov.singularity.content.article.controller

import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.config.Constants
import io.stereov.singularity.test.BaseContentTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserArticleControllerIntegrationTest : BaseContentTest() {

    private val articleBasePath = "$contentBasePath/articles"

    @Test fun `getAccessible works with no authentication`() = runTest {
        val publicArticle = fullArticle.copy(accessType = AccessType.PUBLIC, state = ArticleState.PUBLISHED)
        val article = save(article = publicArticle)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getAccessible works with draft`() = runTest {
        val publicArticle = fullArticle.copy(accessType = AccessType.PUBLIC, state = ArticleState.DRAFT)
        save(article = publicArticle)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getAccessible works with archived`() = runTest {
        val publicArticle = fullArticle.copy(accessType = AccessType.PUBLIC, state = ArticleState.ARCHIVED)
        save(article = publicArticle)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getAccessible works with shared`() = runTest {
        val publicArticle = fullArticle.copy(accessType = AccessType.SHARED, state = ArticleState.PUBLISHED)
        save(article = publicArticle)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getAccessible works with private`() = runTest {
        val publicArticle = fullArticle.copy(accessType = AccessType.PRIVATE, state = ArticleState.PUBLISHED)
        save(article = publicArticle)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getAccessible works with creator`() = runTest {
        val user = registerUser()
        val publicArticle = fullArticle.copy(accessType = AccessType.PRIVATE, state = ArticleState.PUBLISHED)
        val article = save(article = publicArticle, creator = user)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getAccessible works with canView`() = runTest {
        val user = registerUser(email = "another@email.com")
        val publicArticle = fullArticle.copy(accessType = AccessType.SHARED, state = ArticleState.PUBLISHED)
        publicArticle.canView.add(user.info.id)
        val article = save(article = publicArticle)

        assertTrue(article.canView.contains(user.info.id))

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
}
