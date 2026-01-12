package io.stereov.singularity.content.core

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentAccessSubject
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.principal.group.model.KnownGroups
import io.stereov.singularity.test.BaseArticleTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.data.web.PagedModel
import org.springframework.test.web.reactive.server.expectBody

class AccessCriteriaTest : BaseArticleTest() {

    private val articleBasePath = "/api/content/articles"

    data class ArticleOverviewPage(
        val content: List<ArticleOverviewResponse> = emptyList(),
        val page: PagedModel.PageMetadata,
    )

    @Test
    fun `getArticles works with no authentication`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.PUBLIC
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody
        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with shared`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.SHARED
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.page.totalElements)
    }
    @Test
    fun `getArticles works with private`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.PRIVATE
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.page.totalElements)
    }
    @Test
    fun `getArticles works with creator`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)
        article.access.visibility = AccessType.PRIVATE

        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with user viewer`() = runTest {
        val user = registerUser(emailSuffix = "another@email.com")
        val article = saveArticle()
        article.share(ContentAccessSubject.USER, user.id.toHexString(), ContentAccessRole.VIEWER)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with user editor`() = runTest {
        val user = registerUser(emailSuffix = "another@email.com")
        val article = saveArticle()
        article.share(ContentAccessSubject.USER, user.id.toHexString(), ContentAccessRole.EDITOR)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with user admin`() = runTest {
        val user = registerUser(emailSuffix = "another@email.com")
        val article = saveArticle()
        article.share(ContentAccessSubject.USER, user.id.toHexString(), ContentAccessRole.MAINTAINER)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with group viewer`() = runTest {
        val group = createGroup()

        val user = registerUser(emailSuffix = "another@email.com", groups = listOf(group.key))
        val article = saveArticle()
        article.share(ContentAccessSubject.GROUP, user.info.groups.first(), ContentAccessRole.VIEWER)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with group editor`() = runTest {
        val group = createGroup()
        val user = registerUser(emailSuffix = "another@email.com", groups = listOf(group.key))
        val article = saveArticle()
        article.share(ContentAccessSubject.GROUP, user.info.groups.first(), ContentAccessRole.EDITOR)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles works with group admin`() = runTest {
        val group = createGroup()
        val user = registerUser(emailSuffix = "another@email.com", groups = listOf(group.key))
        val article = saveArticle()
        article.share(ContentAccessSubject.GROUP, user.info.groups.first(), ContentAccessRole.MAINTAINER)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles correctly filters tags`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)
        val tag = tagService.create(CreateTagRequest("test2", name = "Test", locale = null)).getOrThrow()

        article.tags.add(tag.key)
        articleService.save(article)

        saveArticle(owner = user)

        val res = webTestClient.get()
            .uri("$articleBasePath?tags=${tag.key}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getArticles correctly filters when multiple tags`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)
        val tag = tagService.create(CreateTagRequest("test2", name = "Test", locale = null)).getOrThrow()
        val anotherTag = tagService.create(CreateTagRequest("test3", name = "Another Test", locale = null)).getOrThrow()

        article.tags.add(tag.key)
        articleService.save(article)

        val anotherArticle = saveArticle(owner = user)
        anotherArticle.tags.add(anotherTag.key)
        articleService.save(anotherArticle)

        val res = webTestClient.get()
            .uri("$articleBasePath?tags=${tag.key},${anotherTag.key}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(2, res.page.totalElements)
        Assertions.assertTrue(res.content.any { it.id == article.id.getOrThrow() })
        Assertions.assertTrue(res.content.any { it.id == anotherArticle.id.getOrThrow() })
    }

    @Test
    fun `getLatestArticles works with no authentication`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.PUBLIC
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with draft`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.PUBLIC
        article.state = ArticleState.DRAFT
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.page.totalElements)
        Assertions.assertEquals(0, res.content.size)
    }
    @Test
    fun `getLatestArticles works with archived`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.PUBLIC
        article.state = ArticleState.ARCHIVED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.page.totalElements)
        Assertions.assertEquals(0, res.content.size)
    }
    @Test
    fun `getLatestArticles works with shared`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.SHARED
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.page.totalElements)
        Assertions.assertEquals(0, res.content.size)
    }
    @Test
    fun `getLatestArticles works with private`() = runTest {
        val article = saveArticle()
        article.access.visibility = AccessType.PRIVATE
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(0, res.page.totalElements)
        Assertions.assertEquals(0, res.content.size)
    }
    @Test
    fun `getLatestArticles works with creator`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)
        article.access.visibility = AccessType.PRIVATE
        article.state = ArticleState.PUBLISHED

        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with user viewer`() = runTest {
        val user = registerUser(emailSuffix = "another@email.com")
        val article = saveArticle()
        article.share(ContentAccessSubject.USER, user.id.toHexString(), ContentAccessRole.VIEWER)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with user editor`() = runTest {
        val user = registerUser(emailSuffix = "another@email.com")
        val article = saveArticle()
        article.share(ContentAccessSubject.USER, user.id.toHexString(), ContentAccessRole.EDITOR)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with user admin`() = runTest {
        val user = registerUser(emailSuffix = "another@email.com")
        val article = saveArticle()
        article.share(ContentAccessSubject.USER, user.id.toHexString(), ContentAccessRole.MAINTAINER)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with group viewer`() = runTest {
        val group = createGroup()
        val user = registerUser(emailSuffix = "another@email.com", groups = listOf(group.key))
        val article = saveArticle()
        article.share(ContentAccessSubject.GROUP, user.info.groups.first(), ContentAccessRole.VIEWER)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with group editor`() = runTest {
        val group = createGroup()
        val user = registerUser(emailSuffix = "another@email.com", groups = listOf(group.key))
        val article = saveArticle()
        article.share(ContentAccessSubject.GROUP, user.info.groups.first(), ContentAccessRole.EDITOR)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
    @Test
    fun `getLatestArticles works with group admin`() = runTest {
        val group = createGroup()
        val user = registerUser(emailSuffix = "another@email.com", groups = listOf(group.key))
        val article = saveArticle()
        article.share(ContentAccessSubject.GROUP, user.info.groups.first(), ContentAccessRole.MAINTAINER)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath?sort=createdAt,desc&state=published")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectBody<ArticleOverviewPage>()
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(1, res.page.totalElements)
        Assertions.assertEquals(1, res.content.size)
        Assertions.assertEquals(article.id.getOrThrow(), res.content.first().id)
    }
}
