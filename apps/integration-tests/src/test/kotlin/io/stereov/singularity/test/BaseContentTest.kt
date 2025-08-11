package io.stereov.singularity.test

import io.stereov.singularity.content.article.dto.CreateArticleRequest
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.core.tag.service.TagService
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.group.model.KnownGroups
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired

class BaseContentTest : BaseIntegrationTest() {

    @Autowired
    lateinit var tagService: TagService
    final val contentBasePath = "$basePath/content"

    @Autowired
    lateinit var articleService: ArticleService

    @AfterEach
    fun clearArticleDatabase() = runBlocking {
        articleService.deleteAll()
        tagService.deleteAll()
    }

    suspend fun save(creator: TestRegisterResponse? = null, title: String? = null): Article {
        val actualUser = creator ?: registerUser(groups = listOf(KnownGroups.EDITOR))
        val req = CreateArticleRequest(Language.EN, title ?: "test", "", "")
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

        return Article.create(res, Language.EN)
    }
}
