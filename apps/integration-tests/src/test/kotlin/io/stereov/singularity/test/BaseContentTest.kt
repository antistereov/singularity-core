package io.stereov.singularity.test

import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.service.ArticleService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class BaseContentTest : BaseIntegrationTest() {

    final val contentBasePath = "$basePath/content"

    @Autowired
    lateinit var articleMapper: ArticleMapper
    
    @Autowired
    lateinit var articleService: ArticleService

    @AfterEach
    fun clearArticleDatabase() = runBlocking {
        articleService.deleteAll()
        tagService.deleteAll()
    }

    suspend fun save(creator: TestRegisterResponse? = null, title: String? = null): Article {
        val actualUser = creator ?: registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val req = CreateArticleRequest(Locale.ENGLISH, title ?: "test", "", "")
        val res = webTestClient.post()
            .uri("/api/content/articles")
            .accessTokenCookie(actualUser.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(FullArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        return articleMapper.createArticle(res, null)
    }
}
