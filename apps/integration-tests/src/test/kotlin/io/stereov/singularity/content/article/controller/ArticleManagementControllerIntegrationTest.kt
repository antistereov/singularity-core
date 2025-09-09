package io.stereov.singularity.content.article.controller

import io.stereov.singularity.auth.core.model.AccessType
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.auth.session.model.SessionTokenType
import io.stereov.singularity.content.article.dto.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.model.ContentAccessSubject
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.test.BaseContentTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArticleManagementControllerIntegrationTest : BaseContentTest() {

    private val articleBasePath = "$contentBasePath/articles"

    data class ArticleOverviewPage(
        val content: List<ArticleOverviewResponse> = emptyList(),
        val pageNumber: Int,
        val pageSize: Int,
        val numberOfElements: Int,
        val totalElements: Long,
        val totalPages: Int,
        val first: Boolean,
        val last: Boolean,
        val hasNext: Boolean,
        val hasPrevious: Boolean
    )
    
    @Test fun `getArticles works with no authentication`() = runTest {
        val article = save()
        article.access.visibility = AccessType.PUBLIC
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody
        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with shared`() = runTest {
        val article = save()
        article.access.visibility = AccessType.SHARED
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.totalElements)
    }
    @Test fun `getArticles works with private`() = runTest {
        val article = save()
        article.access.visibility = AccessType.PRIVATE
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.totalElements)
    }
    @Test fun `getArticles works with creator`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val article = save(creator = user)
        article.access.visibility = AccessType.PRIVATE

        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with user viewer`() = runTest {
        val user = registerUser(email = "another@email.com")
        val article = save()
        article.share(ContentAccessSubject.USER, user.info.id.toHexString(), ContentAccessRole.VIEWER)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with user editor`() = runTest {
        val user = registerUser(email = "another@email.com")
        val article = save()
        article.share(ContentAccessSubject.USER, user.info.id.toHexString(), ContentAccessRole.EDITOR)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with user admin`() = runTest {
        val user = registerUser(email = "another@email.com")
        val article = save()
        article.share(ContentAccessSubject.USER, user.info.id.toHexString(), ContentAccessRole.ADMIN)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with group viewer`() = runTest {
        val group = createGroup()

        val user = registerUser(email = "another@email.com", groups = listOf(group.key))
        val article = save()
        article.share(ContentAccessSubject.GROUP, user.info.sensitive.groups.first(), ContentAccessRole.VIEWER)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with group editor`() = runTest {
        val group = createGroup()
        val user = registerUser(email = "another@email.com", groups = listOf(group.key))
        val article = save()
        article.share(ContentAccessSubject.GROUP, user.info.sensitive.groups.first(), ContentAccessRole.EDITOR)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles works with group admin`() = runTest {
        val group = createGroup()
        val user = registerUser(email = "another@email.com", groups = listOf(group.key))
        val article = save()
        article.share(ContentAccessSubject.GROUP, user.info.sensitive.groups.first(), ContentAccessRole.ADMIN)
        articleService.save(article)

        val res = webTestClient.get()
            .uri(articleBasePath)
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles correctly filters tags`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val article = save(creator = user)
        val tag = tagService.create(CreateTagRequest("test", name = "Test"))

        article.tags.add(tag.key)
        articleService.save(article)

        save(creator = user)

        val res = webTestClient.get()
            .uri("$articleBasePath?tags=${tag.key}")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(article.id, res.content.first().id)
    }
    @Test fun `getArticles correctly filters when multiple tags`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val article = save(creator = user)
        val tag = tagService.create(CreateTagRequest("test", name = "Test"))
        val anotherTag = tagService.create(CreateTagRequest("test2", name = "Another Test"))

        article.tags.add(tag.key)
        articleService.save(article)

        val anotherArticle = save(creator = user)
        anotherArticle.tags.add(anotherTag.key)
        articleService.save(anotherArticle)

        val res = webTestClient.get()
            .uri("$articleBasePath?tags=${tag.key},${anotherTag.key}")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(2, res.totalElements)
        assertTrue(res.content.any { it.id == article.id })
        assertTrue(res.content.any { it.id == anotherArticle.id })
    }

    @Test fun `getLatestArticles works with no authentication`() = runTest {
        val article = save()
        article.access.visibility = AccessType.PUBLIC
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with draft`() = runTest {
        val article = save()
        article.access.visibility = AccessType.PUBLIC
        article.state = ArticleState.DRAFT
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getLatestArticles works with archived`() = runTest {
        val article = save()
        article.access.visibility = AccessType.PUBLIC
        article.state = ArticleState.ARCHIVED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getLatestArticles works with shared`() = runTest {
        val article = save()
        article.access.visibility = AccessType.SHARED
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getLatestArticles works with private`() = runTest {
        val article = save()
        article.access.visibility = AccessType.PRIVATE
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(0, res.articles.size)
    }
    @Test fun `getLatestArticles works with creator`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val article = save(creator = user)
        article.access.visibility = AccessType.PRIVATE
        article.state = ArticleState.PUBLISHED

        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with user viewer`() = runTest {
        val user = registerUser(email = "another@email.com")
        val article = save()
        article.share(ContentAccessSubject.USER, user.info.id.toHexString(), ContentAccessRole.VIEWER)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with user editor`() = runTest {
        val user = registerUser(email = "another@email.com")
        val article = save()
        article.share(ContentAccessSubject.USER, user.info.id.toHexString(), ContentAccessRole.EDITOR)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with user admin`() = runTest {
        val user = registerUser(email = "another@email.com")
        val article = save()
        article.share(ContentAccessSubject.USER, user.info.id.toHexString(), ContentAccessRole.ADMIN)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with group viewer`() = runTest {
        val group = createGroup()
        val user = registerUser(email = "another@email.com", groups = listOf(group.key))
        val article = save()
        article.share(ContentAccessSubject.GROUP, user.info.sensitive.groups.first(), ContentAccessRole.VIEWER)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with group editor`() = runTest {
        val group = createGroup()
        val user = registerUser(email = "another@email.com", groups = listOf(group.key))
        val article = save()
        article.share(ContentAccessSubject.GROUP, user.info.sensitive.groups.first(), ContentAccessRole.EDITOR)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
            .exchange()
            .expectBody(ArticleResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.remainingCount)
        assertEquals(1, res.articles.size)
        assertEquals(article.id, res.articles.first().id)
    }
    @Test fun `getLatestArticles works with group admin`() = runTest {
        val group = createGroup()
        val user = registerUser(email = "another@email.com", groups = listOf(group.key))
        val article = save()
        article.share(ContentAccessSubject.GROUP, user.info.sensitive.groups.first(), ContentAccessRole.ADMIN)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)

        val res = webTestClient.get()
            .uri("$articleBasePath/scroll")
            .cookie(SessionTokenType.Access.cookieName, user.accessToken)
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
