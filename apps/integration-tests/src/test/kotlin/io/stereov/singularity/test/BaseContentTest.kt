package io.stereov.singularity.test

import io.stereov.singularity.content.article.dto.FullArticleDto
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleContent
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.config.Constants
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class BaseContentTest : BaseIntegrationTest() {

    val contentBasePath = "$basePath/content"

    @Autowired
    lateinit var articleService: ArticleService

    @AfterEach
    fun clearArticleDatabase() = runBlocking {
        articleService.deleteAll()
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
        canView = mutableSetOf(),
        canEdit = mutableSetOf(),
    )

    suspend fun save(creator: TestRegisterResponse? = null, article: FullArticleDto = fullArticle): FullArticleDto {
        val actualUser = creator ?: registerUser()
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
}
