package io.stereov.singularity.content.article.controller

import io.lettuce.core.KillArgs.Builder.user
import io.stereov.singularity.content.article.dto.FullArticleDto
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleContent
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.config.Constants
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.user.model.Role
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class ArticleControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var articleService: ArticleService

    @BeforeEach
    fun cleanUp() = runBlocking {
        articleService.deleteAll()
        userService.deleteAll()
    }

    val fullArticle = FullArticleDto(
        id = null,
        key = "article-key",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        publishedAt = null,
        creator = null,
        path = Article.basePath + "/article-key",
        state = ArticleState.DRAFT,
        title = "Title",
        colors = ArticleColors("black", "white"),
        summary = "Summary",
        image = null,
        content = ArticleContent("content", listOf()),
        accessType = AccessType.PUBLIC,
        canView = listOf(),
        canEdit = listOf(),
    )

    suspend fun save(user: TestRegisterResponse? = null, article: FullArticleDto = fullArticle): FullArticleDto {
        val actualUser = user ?: registerUser()
        val actualArticle = article.copy(creator = actualUser.info.toOverviewDto())

        val res = webTestClient.put()
            .uri("/api/content/articles")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, actualUser.accessToken)
            .bodyValue(actualArticle)
            .exchange()
            .expectStatus().isOk
            .expectBody(FullArticleDto::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        return res
    }

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
        val article = save(user = user)

        webTestClient.put()
            .uri("/api/content/articles/trusted/${article.key}?trusted=false")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, user.accessToken)
            .exchange()
            .expectStatus()
            .isForbidden

    }

    @Test fun `setTrusted can works`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN))
        val article = save(user = user)

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
