package io.stereov.singularity.content.tag.controller

import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.content.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.returnResult
import java.util.*

class TagControllerTest() : BaseIntegrationTest() {

    @Autowired
    lateinit var tagMapper: TagMapper

    @Test fun `create works`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))

        val res = webTestClient.post()
            .uri("/api/content/tags")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CreateTagRequest("test", name = "Test", description = "Test", locale = null))
            .exchange()
            .expectStatus().isOk
            .expectBody(TagResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        val foundTag = tagService.findByKey("test")
        assertTrue(foundTag.translations.keys.contains(Locale.ENGLISH))
        assertThat(foundTag.translations.keys.size).isEqualTo(1)

        assertThat(tagMapper.createTagResponse(foundTag, null)).isEqualTo(res)
    }
    @Test fun `create works with another locale`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))

        val res = webTestClient.post()
            .uri("/api/content/tags")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CreateTagRequest("test", name = "Test", description = "Test", locale = Locale.GERMAN))
            .exchange()
            .expectStatus().isOk
            .expectBody(TagResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        val foundTag = tagService.findByKey("test")
        assertTrue(foundTag.translations.keys.contains(Locale.GERMAN))
        assertThat(foundTag.translations.keys.size).isEqualTo(1)

        assertThat(tagMapper.createTagResponse(foundTag, null)).isEqualTo(res)
    }
    @Test fun `create requires new key`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))

        tagService.create(CreateTagRequest("test", name = "Test", description = "Test", locale = null))

        val res = webTestClient.post()
            .uri("/api/content/tags")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CreateTagRequest("test", name = "Test", description = "Test", locale = null))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)

        assertEquals(1, tagService.findAll().count())
        val foundTag = tagService.findByKey("test")
        assertTrue(foundTag.translations.keys.contains(Locale.ENGLISH))
        assertThat(foundTag.translations.keys.size).isEqualTo(1)
    }
    @Test fun `create requires authentication`() = runTest {

        val res = webTestClient.post()
            .uri("/api/content/tags")
            .bodyValue(CreateTagRequest("test", name = "Test", description = "Test", locale = null))
            .exchange()
            .expectStatus().isUnauthorized

        requireNotNull(res)

        assertTrue(tagService.findAll().toList().isEmpty())
    }
    @Test fun `create requires editor role`() = runTest {
        val user = registerUser()

        val res = webTestClient.post()
            .uri("/api/content/tags")
            .accessTokenCookie(user.accessToken)
            .bodyValue(CreateTagRequest("test", name = "Test", description = "Test", locale = null))
            .exchange()
            .expectStatus().isForbidden

        requireNotNull(res)

        assertTrue(tagService.findAll().toList().isEmpty())
    }
    @Test fun `create requires body`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))

        val res = webTestClient.post()
            .uri("/api/content/tags")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        requireNotNull(res)

        assertTrue(tagService.findAll().toList().isEmpty())
    }

    @Test fun `findByKey works`() = runTest {
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val res = webTestClient.get()
            .uri("/api/content/tags/${tag.key}")
            .exchange()
            .expectStatus().isOk
            .expectBody(TagResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(tagMapper.createTagResponse(tag, null), res)
        assertEquals("Test", res.name)
    }
    @Test fun `findByKey works with another locale`() = runTest {
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val res = webTestClient.get()
            .uri("/api/content/tags/${tag.key}?locale=de")
            .exchange()
            .expectStatus().isOk
            .expectBody(TagResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(tagMapper.createTagResponse(tag, Locale.GERMAN), res)
        assertEquals("TestDeutsch", res.name)
    }
    @Test fun `findByKey needs tag`() = runTest {

        val res = webTestClient.get()
            .uri("/api/content/tags/tag")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `find works`() = runTest {
        val tag1 = tagService.save(TagDocument(
            key = "test1",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test1", "Desc"),
                Locale.GERMAN to TagTranslation("TestDeutsch1")
            )
        ))
        val tag2 = tagService.save(TagDocument(
            key = "test2",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test2"),
                Locale.GERMAN to TagTranslation("TestDeutsch2")
            )
        ))

        val res = webTestClient.get()
            .uri("/api/content/tags?name=1")
            .exchange()
            .expectStatus().isOk
            .returnResult<PageImpl<TagResponse>>()
            .responseBody
            .awaitFirstOrNull()

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(tag1.key, res.content.first().key)
    }

    @Test fun `update works`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val req = UpdateTagRequest(mapOf(
            Locale.ENGLISH to UpdateTagRequest.TagTranslationUpdateRequest(null, "New"),
            Locale.JAPANESE to UpdateTagRequest.TagTranslationUpdateRequest("TagJap", "TagJ")
        ))

        val res = webTestClient.patch()
            .uri("/api/content/tags/${tag.key}")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(TagResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        val updatedTag = tagService.findById(tag.id)
        assertEquals(tagMapper.createTagResponse(updatedTag, null), res)
        assertEquals("Test", res.name)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("New", updatedTag.translations[Locale.ENGLISH]?.description)
        assertEquals("TagJap", updatedTag.translations[Locale.JAPANESE]?.name)
        assertEquals("TagJ", updatedTag.translations[Locale.JAPANESE]?.description)
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)

    }
    @Test fun `update with new lang requires title`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val req = UpdateTagRequest(mapOf(
            Locale.ENGLISH to UpdateTagRequest.TagTranslationUpdateRequest(null, "New"),
            Locale.JAPANESE to UpdateTagRequest.TagTranslationUpdateRequest(null, "TagJ")
        ))

        webTestClient.patch()
            .uri("/api/content/tags/${tag.key}")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest


        val updatedTag = tagService.findById(tag.id)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("", updatedTag.translations[Locale.ENGLISH]?.description)
        assertNull(updatedTag.translations[Locale.JAPANESE])
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)
    }
    @Test fun `update works with another locale`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val req = UpdateTagRequest(mapOf(
            Locale.ENGLISH to UpdateTagRequest.TagTranslationUpdateRequest(null, "New"),
            Locale.JAPANESE to UpdateTagRequest.TagTranslationUpdateRequest("TagJap", "TagJ")
        ))

        val res = webTestClient.patch()
            .uri("/api/content/tags/${tag.key}?locale=de")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(TagResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        val updatedTag = tagService.findById(tag.id)
        assertEquals(tagMapper.createTagResponse(updatedTag, Locale.GERMAN), res)
        assertEquals("TestDeutsch", res.name)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("New", updatedTag.translations[Locale.ENGLISH]?.description)
        assertEquals("TagJap", updatedTag.translations[Locale.JAPANESE]?.name)
        assertEquals("TagJ", updatedTag.translations[Locale.JAPANESE]?.description)
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)

    }
    @Test fun `update requires authentication`() = runTest {
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val req = UpdateTagRequest(mapOf(
            Locale.ENGLISH to UpdateTagRequest.TagTranslationUpdateRequest(null, "New"),
            Locale.JAPANESE to UpdateTagRequest.TagTranslationUpdateRequest("TagJap", "TagJ")
        ))

        webTestClient.patch()
            .uri("/api/content/tags/${tag.key}")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized


        val updatedTag = tagService.findById(tag.id)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("", updatedTag.translations[Locale.ENGLISH]?.description)
        assertNull(updatedTag.translations[Locale.JAPANESE])
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)
    }
    @Test fun `update requires editor role`() = runTest {
        val user = registerUser()
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val req = UpdateTagRequest(mapOf(
            Locale.ENGLISH to UpdateTagRequest.TagTranslationUpdateRequest(null, "New"),
            Locale.JAPANESE to UpdateTagRequest.TagTranslationUpdateRequest("TagJap", "TagJ")
        ))

        webTestClient.patch()
            .uri("/api/content/tags/${tag.key}")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isForbidden


        val updatedTag = tagService.findById(tag.id)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("", updatedTag.translations[Locale.ENGLISH]?.description)
        assertNull(updatedTag.translations[Locale.JAPANESE])
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)
    }
    @Test fun `update requires body`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        webTestClient.patch()
            .uri("/api/content/tags/${tag.key}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest


        val updatedTag = tagService.findById(tag.id)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("", updatedTag.translations[Locale.ENGLISH]?.description)
        assertNull(updatedTag.translations[Locale.JAPANESE])
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)
    }
    @Test fun `update requires tag`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        val req = UpdateTagRequest(mapOf(
            Locale.ENGLISH to UpdateTagRequest.TagTranslationUpdateRequest(null, "New"),
            Locale.JAPANESE to UpdateTagRequest.TagTranslationUpdateRequest("TagJap", "TagJ")
        ))

        webTestClient.patch()
            .uri("/api/content/tags/tag")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotFound


        val updatedTag = tagService.findById(tag.id)
        assertEquals("Test", updatedTag.translations[Locale.ENGLISH]?.name)
        assertEquals("", updatedTag.translations[Locale.ENGLISH]?.description)
        assertNull(updatedTag.translations[Locale.JAPANESE])
        assertEquals("TestDeutsch", updatedTag.translations[Locale.GERMAN]?.name)
        assertEquals("", updatedTag.translations[Locale.GERMAN]?.description)
    }

    @Test fun `delete works`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        webTestClient.delete()
            .uri("/api/content/tags/${tag.key}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(tagService.findAll().toList().isEmpty())
    }
    @Test fun `delete requires authentication`() = runTest {
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        webTestClient.delete()
            .uri("/api/content/tags/${tag.key}")
            .exchange()
            .expectStatus().isUnauthorized

        assertFalse(tagService.findAll().toList().isEmpty())
    }
    @Test fun `delete requires editor role`() = runTest {
        val user = registerUser()
        val tag = tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        webTestClient.delete()
            .uri("/api/content/tags/${tag.key}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertFalse(tagService.findAll().toList().isEmpty())
    }
    @Test fun `delete requires tag`() = runTest {
        val user = registerUser(groups = listOf(KnownGroups.EDITOR))
        tagService.save(TagDocument(
            key = "test",
            translations = mutableMapOf(
                Locale.ENGLISH to TagTranslation("Test"),
                Locale.GERMAN to TagTranslation("TestDeutsch")
            )
        ))

        webTestClient.delete()
            .uri("/api/content/tags/tag")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotFound

        assertFalse(tagService.findAll().toList().isEmpty())
    }
}
