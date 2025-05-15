package io.stereov.singularity.test

import io.stereov.singularity.content.article.dto.CreateArticleRequest
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.core.config.Constants
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired

class BaseContentTest : BaseIntegrationTest() {

    final val contentBasePath = "$basePath/content"

    @Autowired
    lateinit var articleService: ArticleService

    @AfterEach
    fun clearArticleDatabase() = runBlocking {
        articleService.deleteAll()
    }

    suspend fun save(creator: TestRegisterResponse? = null, title: String? = null): Article {
        val actualUser = creator ?: registerUser()
        val req = CreateArticleRequest(title ?: "test")
        val res = webTestClient.post()
            .uri("/api/content/articles/create")
            .cookie(Constants.ACCESS_TOKEN_COOKIE, actualUser.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(FullArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        return Article.create(res)
    }
}
