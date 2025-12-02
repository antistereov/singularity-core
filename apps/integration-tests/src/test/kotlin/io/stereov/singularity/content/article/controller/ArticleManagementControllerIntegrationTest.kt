package io.stereov.singularity.content.article.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.content.article.dto.request.ChangeArticleStateRequest
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.request.UpdateArticleRequest
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.request.UpdateOwnerRequest
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.group.model.Group
import io.stereov.singularity.principal.group.model.GroupTranslation
import io.stereov.singularity.principal.group.model.KnownGroups
import io.stereov.singularity.test.BaseArticleTest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.returnResult
import java.io.File
import java.net.URI
import java.time.temporal.ChronoUnit
import java.util.*

class ArticleManagementControllerIntegrationTest() : BaseArticleTest() {

    @Autowired
    lateinit var localFileStorageProperties: LocalFileStorageProperties

    @Test fun `create works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val req = CreateArticleRequest(
            locale = Locale.ENGLISH,
            title = "New Title",
            summary = "Cool Summary",
            content = "Cool Content"
        )

        val res = webTestClient.post()
            .uri("/api/content/articles")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(req.title, res.title)
        assertEquals("new-title", res.key)
        assertEquals(req.summary, res.summary)
        assertEquals(req.content, res.content)

        val article = articleService.findByKey(res.key).getOrThrow()
        assertEquals(AccessType.PRIVATE, article.access.visibility)
        assertEquals(owner.id, article.access.ownerId)
        assertTrue(article.access.users.isEmpty())
        assertTrue(article.access.groups.isEmpty())
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(req.title, translation.title)
        assertEquals(req.content, translation.content)
        assertEquals(req.summary, translation.summary)
    }
    @Test fun `create works with another locale`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val req = CreateArticleRequest(
            locale = Locale.GERMAN,
            title = "New Title",
            summary = "Cool Summary",
            content = "Cool Content"
        )

        val res = webTestClient.post()
            .uri("/api/content/articles?locale=de")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(req.title, res.title)
        assertEquals("new-title", res.key)
        assertEquals(req.summary, res.summary)
        assertEquals(req.content, res.content)

        val article = articleService.findByKey(res.key).getOrThrow()
        assertEquals(AccessType.PRIVATE, article.access.visibility)
        assertEquals(owner.id, article.access.ownerId)
        assertTrue(article.access.users.isEmpty())
        assertTrue(article.access.groups.isEmpty())
        val translation = requireNotNull(article.translations[Locale.GERMAN])
        assertEquals(req.title, translation.title)
        assertEquals(req.content, translation.content)
        assertEquals(req.summary, translation.summary)
    }
    @Test fun `create works with same title`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val req = CreateArticleRequest(
            locale = Locale.ENGLISH,
            title = "New Title",
            summary = "Cool Summary",
            content = "Cool Content"
        )

        val res = webTestClient.post()
            .uri("/api/content/articles")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(req.title, res.title)
        assertEquals("new-title", res.key)
        assertEquals(req.summary, res.summary)
        assertEquals(req.content, res.content)

        val article = articleService.findByKey(res.key).getOrThrow()
        assertEquals(AccessType.PRIVATE, article.access.visibility)
        assertEquals(owner.id, article.access.ownerId)
        assertTrue(article.access.users.isEmpty())
        assertTrue(article.access.groups.isEmpty())
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(req.title, translation.title)
        assertEquals(req.content, translation.content)
        assertEquals(req.summary, translation.summary)

        val res1 = webTestClient.post()
            .uri("/api/content/articles")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(req.title, res1.title)
        assertTrue(res1.key.contains("new-title"))
        assertNotEquals(res1.key, res.key)
        assertEquals(req.summary, res1.summary)
        assertEquals(req.content, res1.content)

        val article1 = articleService.findByKey(res.key).getOrThrow()
        assertEquals(AccessType.PRIVATE, article1.access.visibility)
        assertEquals(owner.id, article1.access.ownerId)
        assertTrue(article1.access.users.isEmpty())
        assertTrue(article1.access.groups.isEmpty())
        val translation1 = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(req.title, translation1.title)
        assertEquals(req.content, translation1.content)
        assertEquals(req.summary, translation1.summary)

        assertEquals(2, articleService.findAll().getOrThrow().count())
    }
    @Test fun `create requires contributor group`() = runTest {
        val owner = registerUser()
        val req = CreateArticleRequest(
            locale = Locale.ENGLISH,
            title = "New Title",
            summary = "Cool Summary",
            content = "Cool Content"
        )

        webTestClient.post()
            .uri("/api/content/articles")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertTrue(articleService.findAll().getOrThrow().toList().isEmpty())
    }
    @Test fun `create requires authentication`() = runTest {
        val req = CreateArticleRequest(
            locale = Locale.ENGLISH,
            title = "New Title",
            summary = "Cool Summary",
            content = "Cool Content"
        )

        webTestClient.post()
            .uri("/api/content/articles")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        assertTrue(articleService.findAll().getOrThrow().toList().isEmpty())
    }
    @Test fun `create requires unempty title`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val req = CreateArticleRequest(
            locale = Locale.ENGLISH,
            title = "  ",
            summary = "Cool Summary",
            content = "Cool Content"
        )

        webTestClient.post()
            .uri("/api/content/articles")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        assertTrue(articleService.findAll().getOrThrow().toList().isEmpty())
    }
    @Test fun `create requires body`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))

        webTestClient.post()
            .uri("/api/content/articles")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        assertTrue(articleService.findAll().getOrThrow().toList().isEmpty())
    }

    @Test fun `update works with title`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = "New Title",
            summary = null,
            content = null,
            colors = null,
            tags = null,
            locale = appProperties.locale
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertNotEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        assertEquals(req.title, res.title)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(req.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(req.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update works with summary`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = "New Summary",
            content = null,
            colors = null,
            tags = null,
            locale = appProperties.locale
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(req.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(req.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update works with content`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = null,
            content = "New content",
            colors = null,
            tags = null,
            locale = appProperties.locale
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(req.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(req.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update works with text color`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = null,
            content = null,
            colors = ArticleColors(text = "another"),
            tags = null,
            locale = appProperties.locale
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(req.colors?.text, res.colors.text)
        assertEquals(article.colors.background, res.colors.background)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(req.colors?.text, updatedArticle.colors.text)
        assertEquals(article.colors.background, updatedArticle.colors.background)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update works with background color`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = null,
            content = null,
            colors = ArticleColors(background = "another"),
            tags = null,
            locale = appProperties.locale
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(req.colors?.background, res.colors.background)
        assertEquals(article.colors.text, res.colors.text)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(req.colors?.background, updatedArticle.colors.background)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update works with tags`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = null,
            content = null,
            colors = null,
            tags = mutableSetOf("test"),
            locale = appProperties.locale
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        val tag = tagService.findByKey("test").getOrThrow()
        val tagTranslation = requireNotNull(tag.translations[Locale.ENGLISH])
        assertEquals(req.tags, res.tags.map { it.key }.toMutableSet())
        assertEquals(1, res.tags.size)
        val resTag = res.tags.first()
        assertEquals(tagTranslation.name, resTag.name)
        assertEquals(tagTranslation.description, resTag.description)

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(req.tags?.toSet(), updatedArticle.tags)
    }
    @Test fun `update works with another locale`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = "New Title",
            summary = "New Summary",
            content = "New Content",
            colors = null,
            tags = null,
            locale = Locale.GERMAN
        )

        val res = webTestClient.patch()
            .uri("/api/content/articles/${article.key}?locale=de")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.GERMAN, res.locale)
        assertEquals(req.title, res.title)
        assertNull(article.translations[Locale.GERMAN])
        assertEquals(req.title, res.title)
        assertEquals(req.summary, res.summary)
        assertEquals(req.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.GERMAN])
        assertEquals(req.title, updatedTranslation.title)
        assertEquals(req.summary, updatedTranslation.summary)
        assertEquals(req.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update with another locale requires title`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = "New Summary",
            content = "New Content",
            colors = null,
            tags = null,
            locale = Locale.GERMAN
        )

        webTestClient.patch()
            .uri("/api/content/articles/${article.key}?locale=de")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        assertNothingChanged(article)
    }
    @Test fun `update requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = UpdateArticleRequest(
            title = null,
            summary = "New Summary",
            content = "New Content",
            colors = null,
            tags = null,
            locale = Locale.ENGLISH
        )

        webTestClient.patch()
            .uri("/api/content/articles/${article.key}?locale=de")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        assertNothingChanged(article)
    }
    @Test fun `update requires editor role`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val anotherUser = registerUser()
        val article = saveArticle(owner = owner)
        article.access.users.viewer.add(anotherUser.id.toString())
        articleService.save(article)

        val req = UpdateArticleRequest(
            title = null,
            summary = "New Summary",
            content = "New Content",
            colors = null,
            tags = null,
            locale = Locale.ENGLISH
        )

        webTestClient.patch()
            .uri("/api/content/articles/${article.key}?locale=de")
            .accessTokenCookie(anotherUser.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isForbidden

        assertNothingChanged(article)
    }
    @Test fun `update requires body`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        UpdateArticleRequest(
            title = null,
            summary = "New Summary",
            content = "New Content",
            colors = null,
            tags = null,
            locale = Locale.ENGLISH
        )

        webTestClient.patch()
            .uri("/api/content/articles/${article.key}?locale=de")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        assertNothingChanged(article)
    }

    @Test fun `update state works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = ChangeArticleStateRequest(ArticleState.PUBLISHED)

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/state")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(req.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(req.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update state requires editor`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()
        article.access.users.viewer.add(anotherUser.id.toString())
        articleService.save(article)

        val req = ChangeArticleStateRequest(ArticleState.PUBLISHED)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/state")
            .bodyValue(req)
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertNothingChanged(article)
    }
    @Test fun `update state requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = ChangeArticleStateRequest(ArticleState.PUBLISHED)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/state")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        assertNothingChanged(article)
    }
    @Test fun `update state requires article`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val req = ChangeArticleStateRequest(ArticleState.PUBLISHED)

        webTestClient.put()
            .uri("/api/content/articles/not-there/state")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }

    @Test fun `update image works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val file = ClassPathResource("files/test-image.jpg")

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.IMAGE_JPEG,
                    )
                }.build()
            )
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertNotEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertNotEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)

        val imageRenditions = requireNotNull(res.image).renditions

        // Small
        val small = requireNotNull(imageRenditions[ImageProperties::small.name])
        val smallFile = File(localFileStorageProperties.fileDirectory, small.key)
        assertTrue(smallFile.exists())
        webTestClient.get()
            .uri(URI.create(small.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.parseMediaType("image/webp"))
            .expectHeader().contentLength(smallFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(smallFile.readBytes())
            }
        // Medium
        val medium = requireNotNull(imageRenditions[ImageProperties::medium.name])
        val mediumFile = File(localFileStorageProperties.fileDirectory, medium.key)
        assertTrue(mediumFile.exists())
        webTestClient.get()
            .uri(URI.create(medium.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.parseMediaType("image/webp"))
            .expectHeader().contentLength(mediumFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(mediumFile.readBytes())
            }

        // Large
        val large = requireNotNull(imageRenditions[ImageProperties::large.name])
        val largeFile = File(localFileStorageProperties.fileDirectory, large.key)
        assertTrue(largeFile.exists())
        webTestClient.get()
            .uri(URI.create(large.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.parseMediaType("image/webp"))
            .expectHeader().contentLength(largeFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(largeFile.readBytes())
            }

        // Original
        val original = requireNotNull(imageRenditions[FileMetadataDocument.ORIGINAL_RENDITION])
        val originalFile = File(localFileStorageProperties.fileDirectory, original.key)
        assertTrue(largeFile.exists())
        webTestClient.get()
            .uri(URI.create(original.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(originalFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(file.file.readBytes())
            }
    }
    @Test fun `update image removes old images`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val file = ClassPathResource("files/test-image.jpg")

        val res1 = webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.IMAGE_JPEG,
                    )
                }.build()
            )
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.IMAGE_JPEG,
                    )
                }.build()
            )
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertNotEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertNotEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)

        val imageRenditions = requireNotNull(res.image).renditions

        // Small
        val small = requireNotNull(imageRenditions[ImageProperties::small.name])
        val smallFile = File(localFileStorageProperties.fileDirectory, small.key)
        assertTrue(smallFile.exists())
        webTestClient.get()
            .uri(URI.create(small.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.parseMediaType("image/webp"))
            .expectHeader().contentLength(smallFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(smallFile.readBytes())
            }
        // Medium
        val medium = requireNotNull(imageRenditions[ImageProperties::medium.name])
        val mediumFile = File(localFileStorageProperties.fileDirectory, medium.key)
        assertTrue(mediumFile.exists())
        webTestClient.get()
            .uri(URI.create(medium.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.parseMediaType("image/webp"))
            .expectHeader().contentLength(mediumFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(mediumFile.readBytes())
            }

        // Large
        val large = requireNotNull(imageRenditions[ImageProperties::large.name])
        val largeFile = File(localFileStorageProperties.fileDirectory, large.key)
        assertTrue(largeFile.exists())
        webTestClient.get()
            .uri(URI.create(large.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.parseMediaType("image/webp"))
            .expectHeader().contentLength(largeFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(largeFile.readBytes())
            }

        // Original
        val original = requireNotNull(imageRenditions[FileMetadataDocument.ORIGINAL_RENDITION])
        val originalFile = File(localFileStorageProperties.fileDirectory, original.key)
        assertTrue(originalFile.exists())
        webTestClient.get()
            .uri(URI.create(original.url).path)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(originalFile.length())
            .expectBody()
            .consumeWith {
                assertThat(it.responseBody).isEqualTo(file.file.readBytes())
            }

        val imageRenditionsOld = requireNotNull(res1.image).renditions

        // Small
        val small1 = requireNotNull(imageRenditionsOld[ImageProperties::small.name])
        val smallFile1 = File(localFileStorageProperties.fileDirectory, small1.key)
        assertFalse(smallFile1.exists())
        // Medium
        val medium1 = requireNotNull(imageRenditionsOld[ImageProperties::medium.name])
        val mediumFile1 = File(localFileStorageProperties.fileDirectory, medium1.key)
        assertFalse(mediumFile1.exists())

        // Large
        val large1 = requireNotNull(imageRenditionsOld[ImageProperties::large.name])
        val largeFile1 = File(localFileStorageProperties.fileDirectory, large1.key)
        assertFalse(largeFile1.exists())

        // Original
        val original1 = requireNotNull(imageRenditionsOld[FileMetadataDocument.ORIGINAL_RENDITION])
        val originalFile1 = File(localFileStorageProperties.fileDirectory, original1.key)
        assertFalse(originalFile1.exists())
    }
    @Test fun `update image requires editor`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val file = ClassPathResource("files/test-image.jpg")
        val anotherUser = registerUser()
        article.access.users.viewer.add(anotherUser.id.toString())
        articleService.save(article)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.IMAGE_JPEG,
                    )
                }.build()
            )
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertNothingChanged(article)
    }
    @Test fun `update image requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val file = ClassPathResource("files/test-image.jpg")

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.IMAGE_JPEG,
                    )
                }.build()
            )
            .exchange()
            .expectStatus().isUnauthorized

        assertNothingChanged(article)
    }
    @Test fun `update image requires image`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val file = ClassPathResource("files/test-image.jpg")

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.APPLICATION_JSON,
                    )
                }.build()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE)

        assertNothingChanged(article)
    }

    @Test fun `get access details works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()
        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val res1 = webTestClient.get()
            .uri("/api/content/articles/${article.key}/access")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(owner.id, res1.ownerId)
        assertEquals(req.accessType, res1.visibility)
        assertEquals(3, res1.users.size)
        assertEquals(3, res1.groups.size)
        assertTrue(res1.users.any { it.user.id == maintainer.id && it.role == ContentAccessRole.MAINTAINER })
        assertTrue(res1.users.any { it.user.id == editor.id && it.role == ContentAccessRole.EDITOR })
        assertTrue(res1.users.any { it.user.id == viewer.id && it.role == ContentAccessRole.VIEWER })
        assertTrue(res1.groups[maintainerGroup.key] == ContentAccessRole.MAINTAINER)
        assertTrue(res1.groups[editorGroup.key] == ContentAccessRole.EDITOR)
        assertTrue(res1.groups[viewerGroup.key] == ContentAccessRole.VIEWER)

        val req2 = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req2)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val res2 = webTestClient.get()
            .uri("/api/content/articles/${article.key}/access")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirst()

        assertEquals(owner.id, res2.ownerId)
        assertEquals(req.accessType, res2.visibility)
        assertEquals(0, res2.users.size)
        assertEquals(0, res2.groups.size)
    }
    @Test fun `get access details requires maintainer`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()
        article.access.users.editor.add(anotherUser.id.toString())
        articleService.save(article)

        webTestClient.get()
            .uri("/api/content/articles/${article.key}/access")
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }
    @Test fun `get access details requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        webTestClient.get()
            .uri("/api/content/articles/${article.key}/access")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `get access details requires article`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        saveArticle(owner = owner)

        webTestClient.get()
            .uri("/api/content/articles/oops/access")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `update access works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.access.ownerId)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertNotEquals(article.access, updatedArticle.access)
        assertEquals(article.access.ownerId, updatedArticle.access.ownerId)
        assertEquals(req.accessType, updatedArticle.access.visibility)
        assertTrue(updatedArticle.access.users.maintainer.contains(maintainer.id.toString()))
        assertTrue(updatedArticle.access.users.editor.contains(editor.id.toString()))
        assertTrue(updatedArticle.access.users.viewer.contains(viewer.id.toString()))
        assertTrue(updatedArticle.access.groups.maintainer.contains(maintainerGroup.key))
        assertTrue(updatedArticle.access.groups.editor.contains(editorGroup.key))
        assertTrue(updatedArticle.access.groups.viewer.contains(viewerGroup.key))
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update access overrides everything`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()
        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val req2 = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC
        )

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req2)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.access.ownerId)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access.ownerId, updatedArticle.access.ownerId)
        assertEquals(req2.accessType, updatedArticle.access.visibility)
        assertTrue(updatedArticle.access.users.maintainer.isEmpty())
        assertTrue(updatedArticle.access.users.editor.isEmpty())
        assertTrue(updatedArticle.access.users.viewer.isEmpty())
        assertTrue(updatedArticle.access.groups.maintainer.isEmpty())
        assertTrue(updatedArticle.access.groups.editor.isEmpty())
        assertTrue(updatedArticle.access.groups.viewer.isEmpty())
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update access deletes everybody when private`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()
        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val req2 = UpdateContentAccessRequest(
            accessType = AccessType.PRIVATE,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req2)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.access.ownerId)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access.ownerId, updatedArticle.access.ownerId)
        assertEquals(req2.accessType, updatedArticle.access.visibility)
        assertTrue(updatedArticle.access.users.maintainer.isEmpty())
        assertTrue(updatedArticle.access.users.editor.isEmpty())
        assertTrue(updatedArticle.access.users.viewer.isEmpty())
        assertTrue(updatedArticle.access.groups.maintainer.isEmpty())
        assertTrue(updatedArticle.access.groups.editor.isEmpty())
        assertTrue(updatedArticle.access.groups.viewer.isEmpty())
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update access will stay private when somebody is in there`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PRIVATE,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.access.ownerId)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access.ownerId, updatedArticle.access.ownerId)
        assertEquals(AccessType.PRIVATE, updatedArticle.access.visibility)
        assertTrue(updatedArticle.access.users.maintainer.isEmpty())
        assertTrue(updatedArticle.access.users.editor.isEmpty())
        assertTrue(updatedArticle.access.users.viewer.isEmpty())
        assertTrue(updatedArticle.access.groups.maintainer.isEmpty())
        assertTrue(updatedArticle.access.groups.editor.isEmpty())
        assertTrue(updatedArticle.access.groups.viewer.isEmpty())
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update access changes shared to private when nobody is in there`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()
        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val req2 = UpdateContentAccessRequest(
            accessType = AccessType.SHARED
        )

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req2)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.access.ownerId)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access.ownerId, updatedArticle.access.ownerId)
        assertEquals(AccessType.PRIVATE, updatedArticle.access.visibility)
        assertTrue(updatedArticle.access.users.maintainer.isEmpty())
        assertTrue(updatedArticle.access.users.editor.isEmpty())
        assertTrue(updatedArticle.access.users.viewer.isEmpty())
        assertTrue(updatedArticle.access.groups.maintainer.isEmpty())
        assertTrue(updatedArticle.access.groups.editor.isEmpty())
        assertTrue(updatedArticle.access.groups.viewer.isEmpty())
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update access requires group`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                "maintainer" to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }
    @Test fun `update access requires user`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val maintainer = registerUser()
        userService.deleteById(maintainer.id).getOrThrow()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }
    @Test fun `update access requires maintainer`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()
        article.access.users.viewer.add(anotherUser.id.toString())
        articleService.save(article)

        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertNothingChanged(article)
    }
    @Test fun `update access requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/access")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        assertNothingChanged(article)
    }
    @Test fun `update access requires article`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val maintainer = registerUser()
        val editor = registerUser()
        val viewer = registerUser()
        val maintainerGroup = groupService.save(Group(
            key = "maintainer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Maintainer"))
        )).getOrThrow()
        val editorGroup = groupService.save(Group(
            key = "editor",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Editor"))
        )).getOrThrow()
        val viewerGroup = groupService.save(Group(
            key = "viewer",
            translations = mutableMapOf(Locale.ENGLISH to GroupTranslation("Viewer"))
        )).getOrThrow()

        val req = UpdateContentAccessRequest(
            accessType = AccessType.PUBLIC,
            sharedUsers = mapOf(
                maintainer.id to ContentAccessRole.MAINTAINER,
                editor.id to ContentAccessRole.EDITOR,
                viewer.id to ContentAccessRole.VIEWER
            ),
            sharedGroups = mapOf(
                maintainerGroup.key to ContentAccessRole.MAINTAINER,
                editorGroup.key to ContentAccessRole.EDITOR,
                viewerGroup.key to ContentAccessRole.VIEWER
            )
        )

        webTestClient.put()
            .uri("/api/content/articles/oops/access")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }

    @Test fun `update owner works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val newOwner = registerUser()

        val req = UpdateOwnerRequest(newOwner.id.toString())
        assertTrue(userService.existsById(newOwner.id).getOrThrow())

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/owner")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(req.newOwnerId, res.owner?.id.toString())
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(article.trusted, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(req.newOwnerId, updatedArticle.access.ownerId.toString())
        assertTrue(updatedArticle.access.users.maintainer.contains(owner.id.toString()))
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `update owner requires owner`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()
        article.access.users.maintainer.add(anotherUser.id.toString())
        articleService.save(article)
        val newOwner = registerUser()

        val req = UpdateOwnerRequest(newOwner.id.toString())

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/owner")
            .bodyValue(req)
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertNothingChanged(article)
    }
    @Test fun `update owner requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val newOwner = registerUser()

        val req = UpdateOwnerRequest(newOwner.id.toString())

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/owner")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        assertNothingChanged(article)
    }
    @Test fun `update owner requires article`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val newOwner = registerUser()

        val req = UpdateOwnerRequest(newOwner.id.toString())

        webTestClient.put()
            .uri("/api/content/articles/not-there/owner")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }
    @Test fun `update owner requires user`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val newOwner = registerUser()
        userService.deleteById(newOwner.id).getOrThrow()

        val req = UpdateOwnerRequest(newOwner.id.toString())

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/owner")
            .accessTokenCookie(owner.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }

    @Test fun `setTrusted works`() = runTest {
        val user = registerUser(roles = listOf(Role.User.USER, Role.User.ADMIN), groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)

        assertFalse(articleService.findByKey(article.key).getOrThrow().trusted)

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=true")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        assertTrue(articleService.findByKey(article.key).getOrThrow().trusted)

        val key = requireNotNull(res).key
        assertEquals(article.key, res.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), res.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), res.publishedAt)
        assertTrue(article.updatedAt.isBefore(res.updatedAt))
        assertEquals(article.access.ownerId, res.owner?.id)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            res.path
        )
        assertEquals(article.state, res.state)
        assertEquals(article.colors, res.colors)
        assertEquals(article.imageKey, res.image?.key)
        assertEquals(Locale.ENGLISH, res.locale)
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        assertEquals(translation.title, res.title)
        assertEquals(translation.summary, res.summary)
        assertEquals(translation.content, res.content)
        assertEquals(true, res.trusted)
        assertEquals(article.tags, res.tags.map { it.key }.toSet())

        val updatedArticle = articleService.findByKey(key).getOrThrow()
        assertEquals(res.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertTrue(article.updatedAt.isBefore(updatedArticle.updatedAt))
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.colors.text, updatedArticle.colors.text)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(true, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
    @Test fun `setTrusted can only be called by an admin`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=false")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus()
            .isForbidden

        assertFalse(articleService.findByKey(article.key).getOrThrow().trusted)

        assertNothingChanged(article)
    }
    @Test fun `setTrusted requires authentication`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = user)

        webTestClient.put()
            .uri("/api/content/articles/${article.key}/trusted?trusted=false")
            .exchange()
            .expectStatus()
            .isUnauthorized

        assertFalse(articleService.findByKey(article.key).getOrThrow().trusted)
        assertNothingChanged(article)
    }
    @Test fun `setTrusted requires article`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR), roles = listOf(Role.User.USER, Role.User.ADMIN))
        val article = saveArticle(owner = user)

        webTestClient.put()
            .uri("/api/content/articles/not-existing/trusted?trusted=false")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus()
            .isNotFound

        assertFalse(articleService.findByKey(article.key).getOrThrow().trusted)
        assertNothingChanged(article)
    }

    @Test fun `delete works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        webTestClient.delete()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(articleService.findAll().getOrThrow().toList().isEmpty())
    }
    @Test fun `delete deletes image`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        val file = ClassPathResource("files/test-image.jpg")

        val res = webTestClient.put()
            .uri("/api/content/articles/${article.key}/image")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part(
                        "file",
                        file,
                        MediaType.IMAGE_JPEG,
                    )
                }.build()
            )
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        webTestClient.delete()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(articleService.findAll().getOrThrow().toList().isEmpty())

        val imageRenditionsOld = requireNotNull(res.image).renditions

        // Small
        val small1 = requireNotNull(imageRenditionsOld[ImageProperties::small.name])
        val smallFile1 = File(localFileStorageProperties.fileDirectory, small1.key)
        assertFalse(smallFile1.exists())
        // Medium
        val medium1 = requireNotNull(imageRenditionsOld[ImageProperties::medium.name])
        val mediumFile1 = File(localFileStorageProperties.fileDirectory, medium1.key)
        assertFalse(mediumFile1.exists())

        // Large
        val large1 = requireNotNull(imageRenditionsOld[ImageProperties::large.name])
        val largeFile1 = File(localFileStorageProperties.fileDirectory, large1.key)
        assertFalse(largeFile1.exists())

        // Original
        val original1 = requireNotNull(imageRenditionsOld[FileMetadataDocument.ORIGINAL_RENDITION])
        val originalFile1 = File(localFileStorageProperties.fileDirectory, original1.key)
        assertFalse(originalFile1.exists())
    }
    @Test fun `delete requires maintainer`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)
        val anotherUser = registerUser()
        article.access.users.editor.add(anotherUser.id.toString())
        articleService.save(article)

        webTestClient.delete()
            .uri("/api/content/articles/${article.key}")
            .accessTokenCookie(anotherUser.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertNothingChanged(article)
    }
    @Test fun `delete requires authentication`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        webTestClient.delete()
            .uri("/api/content/articles/${article.key}")
            .exchange()
            .expectStatus().isUnauthorized

        assertNothingChanged(article)
    }
    @Test fun `delete requires article`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        webTestClient.delete()
            .uri("/api/content/articles/oops")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound

        assertNothingChanged(article)
    }

    suspend fun assertNothingChanged(article: Article) {
        val translation = requireNotNull(article.translations[Locale.ENGLISH])
        val updatedArticle = articleService.findByKey(article.key).getOrThrow()
        assertEquals(article.key, updatedArticle.key)
        assertEquals(article.createdAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.createdAt)
        assertEquals(article.publishedAt?.truncatedTo(ChronoUnit.MILLIS), updatedArticle.publishedAt)
        assertEquals(article.updatedAt.truncatedTo(ChronoUnit.MILLIS), updatedArticle.updatedAt)
        assertEquals(article.access, updatedArticle.access)
        assertEquals(contentProperties.contentUri.substringAfter(uiProperties.baseUri)
            .replace("{contentType}", Article.CONTENT_TYPE)
            .replace("{contentKey}", article.key),
            updatedArticle.path
        )
        assertEquals(article.state, updatedArticle.state)
        assertEquals(article.colors, updatedArticle.colors)
        assertEquals(article.imageKey, updatedArticle.imageKey)
        val updatedTranslation = requireNotNull(updatedArticle.translations[Locale.ENGLISH])
        assertEquals(translation.title, updatedTranslation.title)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(translation.summary, updatedTranslation.summary)
        assertEquals(translation.content, updatedTranslation.content)
        assertEquals(article.trusted, updatedArticle.trusted)
        assertEquals(article.tags, updatedArticle.tags)
    }
}
