package io.stereov.singularity.test

import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.principal.group.model.KnownGroups
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import java.util.*

class BaseArticleTest : BaseMailIntegrationTest() {

    @Autowired
    lateinit var articleService: ArticleService

    @Autowired
    lateinit var  articleManagementService: ArticleManagementService

    @Autowired
    lateinit var articleMapper: ArticleMapper

    @AfterEach
    fun deleteArticles() {
        runBlocking { articleService.deleteAll() }
    }


    @BeforeEach
    fun createTag() {
        runBlocking { tagService.create(
            CreateTagMultiLangRequest(key = "test", translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("NameEn", "DescEn"),
                Locale.GERMAN to TagTranslation("NameDe", "DescDe")
            ))
        ) }
    }

    suspend fun saveArticle(owner: TestRegisterResponse<*>? = null, title: String? = null): Article {
        val actualUser = owner ?: registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val req = CreateArticleRequest(Locale.ENGLISH, title ?: "test", "", "")
        val res = webTestClient.post()
            .uri("/api/content/articles")
            .accessTokenCookie(actualUser.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody<FullArticleResponse>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        return articleMapper.createArticle(res, null)
    }
}
