package io.stereov.singularity.content.article.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.core.AccessCriteriaTest
import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.principal.group.model.KnownGroups
import io.stereov.singularity.test.BaseArticleTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.time.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import java.time.Instant
import java.util.*

class ArticleService : BaseArticleTest() {

    @Test fun `findByKey works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(article.key, res.key)
    }
    @Test fun `findByKey works with another locale`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.translations[Locale.GERMAN] = ArticleTranslation("TestDE", "", "")
        articleService.save(article)

        val res = webTestClient.get()
            .uri("/api/content/articles/${article.key}?locale=de")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(article.key, res.key)
        assertEquals(Locale.GERMAN, res.locale)
        assertEquals("TestDE", res.title)
    }
    @Test fun `findByKey works with public`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.access.visibility = AccessType.PUBLIC
        articleService.save(article)

        val res = webTestClient.get()
            .uri("/api/content/articles/${article.key}")
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(article.key, res.key)
    }
    @Test fun `findByKey works with shared`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()
        article.access.users.viewer.add(anotherUser.id.toString())
        articleService.save(article)

        val res = webTestClient.get()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(article.key, res.key)
    }
    @Test fun `findByKey needs access`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()

        webTestClient.get()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }
    @Test fun `findByKey needs access and authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        webTestClient.get()
            .uri("/api/content/articles/${article.key}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `findByKey needs article`() = runTest {
        webTestClient.get()
            .uri("/api/content/articles/oops")
            .exchange()
            .expectStatus().isNotFound
    }
    @Test fun `findByKey does not need existing owner`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        userService.deleteById(owner.id).getOrThrow()

        val res = webTestClient.get()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertNull(res.owner)
    }

    @Test fun `find works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val article1 = saveArticle(owner = owner)
        val article2 = saveArticle(owner = owner)
        val article3 = saveArticle(owner = owner)
        val article4 = saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?page=0&size=3&sort=createdAt,asc")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(3, res.content.size)
        assertEquals(5, res.totalElements)
        assertEquals(article.key, res.content[0].key)
        assertEquals(article1.key, res.content[1].key)
        assertEquals(article2.key, res.content[2].key)

        val res1 = webTestClient.get()
            .uri("/api/content/articles?page=1&size=3&sort=createdAt,asc")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(2, res1.content.size)
        assertEquals(5, res1.totalElements)
        assertEquals(article3.key, res1.content[0].key)
        assertEquals(article4.key, res1.content[1].key)
    }
    @Test fun `find works with title`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.translations[Locale.ENGLISH] = ArticleTranslation("ThisOne", "", "")
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?title=this")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with title in another locale`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.translations[Locale.GERMAN] = ArticleTranslation("ThisOne", "", "")
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?title=this&locale=de")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with content`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.translations[Locale.ENGLISH] = ArticleTranslation("ThisOne", "", "YeahContent")
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?content=yeah")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with content in another locale`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.translations[Locale.GERMAN] = ArticleTranslation("ThisOne", "", "YeahContent")
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?content=yeah&locale=de")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with state`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.state = ArticleState.PUBLISHED
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?state=published")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with tags`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        tagService.save(TagDocument(key = "tag", translations = mutableMapOf(Locale.ENGLISH to TagTranslation("Tag"))))
        article.tags.add("tag")
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?tags=tag")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with roles`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        saveArticle()

        val res = webTestClient.get()
            .uri("/api/content/articles?role=owner")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with createdAtBefore`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.createdAt = Instant.ofEpochSecond(0)
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?createdAtBefore=${Instant.ofEpochSecond(100)}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with updatedAtBefore`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        runBlocking { delay(java.time.Duration.ofSeconds(2)) }
        val another = saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?updatedAtBefore=${another.createdAt.minusSeconds(1)}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with publishedAtBefore`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.publishedAt = Instant.ofEpochSecond(0)
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?publishedAtBefore=${Instant.ofEpochSecond(0)}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with createdAtAfter`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.createdAt = Instant.now().plusSeconds(10000)
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?createdAtAfter=${Instant.now().plusSeconds(100)}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with updatedAtAfter`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val another = saveArticle(owner = owner)
        runBlocking { delay(java.time.Duration.ofSeconds(2)) }
        val article = saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?updatedAtAfter=${another.createdAt.plusSeconds(1)}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find works with publishedAtAfter`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        article.publishedAt = Instant.now().plusSeconds(100000)
        articleService.save(article)
        saveArticle(owner = owner)

        val res = webTestClient.get()
            .uri("/api/content/articles?publishedAtAfter=${Instant.now().plusSeconds(1000)}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
    }
    @Test fun `find does not need owner`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        userService.deleteById(owner.id).getOrThrow()

        val res = webTestClient.get()
            .uri("/api/content/articles")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<AccessCriteriaTest.ArticleOverviewPage>()
            .responseBody
            .awaitFirst()

        assertEquals(1, res.content.size)
        assertEquals(1, res.totalElements)
        assertEquals(article.key, res.content[0].key)
        assertEquals(owner.id, article.access.ownerId)
    }
}
