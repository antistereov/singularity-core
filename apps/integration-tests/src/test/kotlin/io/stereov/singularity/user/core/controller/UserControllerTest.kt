package io.stereov.singularity.user.core.controller

import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.web.util.UriComponentsBuilder
import java.io.File
import java.time.Instant

class UserControllerTest() : BaseIntegrationTest() {

    @Autowired
    lateinit var localFileStorageProperties: LocalFileStorageProperties

    data class UserOverviewPage(
        val content: List<UserOverviewResponse> = emptyList(),
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

    @Test fun `getUserById works`() = runTest {
        val user = registerUser()

        val res = webTestClient.get()
            .uri("/api/users/${user.info.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(user.info.id, res.id)
        assertEquals(user.info.sensitive.name, res.name)
        assertEquals(user.info.sensitive.email, res.email)
    }
    @Test fun `getUserById needs user with id`() = runTest {
        val user = registerUser()
        userService.deleteById(user.info.id)

        webTestClient.get()
            .uri("/api/users/${user.info.id}")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `deleteById works`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN))

        webTestClient.delete()
            .uri("/api/users/${user.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(userService.findAll().toList().isEmpty())

    }
    @Test fun `deleteById deletes avatar`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN))

        val file = ClassPathResource("files/test-image.jpg")
        val res = webTestClient.put()
            .uri("/api/users/me/avatar")
            .accessTokenCookie(user.accessToken)
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
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        webTestClient.delete()
            .uri("/api/users/${user.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(userService.findAll().toList().isEmpty())

        val imageRenditionsOld = requireNotNull(res?.avatar).renditions

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
    @Test fun `deleteById needs user with id`() = runTest {
        val user = registerUser(roles = listOf(Role.USER, Role.ADMIN))
        val anotherUser = registerUser()
        userService.deleteById(anotherUser.info.id)

        webTestClient.delete()
            .uri("/api/users/${anotherUser.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isNotFound

        assertTrue(userService.existsById(user.info.id))
    }
    @Test fun `deleteById needs authentication`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/${user.info.id}")
            .exchange()
            .expectStatus().isUnauthorized

        assertTrue(userService.existsById(user.info.id))
    }
    @Test fun `deleteById needs admin role`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/${user.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertTrue(userService.existsById(user.info.id))
    }
    @Test fun `deleteById needs admin role not guest`() = runTest {
        val user = registerUser(roles = listOf(Role.GUEST))

        webTestClient.delete()
            .uri("/api/users/${user.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        assertTrue(userService.existsById(user.info.id))
    }

    @Test fun `getUsers works with page, sort and size`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER))
        val user2 = registerUser()
        val user3 = registerUser()
        val user4 = registerUser()
        val user5 = registerUser()

        println(userRepository.count())

        val uri1 = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("roles", "user")
            .build()
            .toUri()

        val res1 = webTestClient.get()
            .uri("${uri1.path}?${uri1.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res1)

        assertEquals(5, res1.totalElements)
        assertEquals(4, res1.content.size)
        assertEquals(user1.info.id, res1.content.first().id)
        assertEquals(user2.info.id, res1.content[1].id)
        assertEquals(user3.info.id, res1.content[2].id)
        assertEquals(user4.info.id, res1.content[3].id)

        val uri2 = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 1)
            .queryParam("size", 4)
            .queryParam("roles", "user")
            .build()
            .toUri()

        val res2 = webTestClient.get()
            .uri("${uri2.path}?${uri2.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res2)

        assertEquals(5, res2.totalElements)
        assertEquals(1, res2.content.size)
        assertEquals(user5.info.id, res2.content.first().id)
    }
    @Test fun `getUsers works with everything`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), groups = listOf("test"))
        val user5 = registerOAuth2()
        userService.save(user5.info.copy(lastActive = Instant.ofEpochSecond(0), createdAt = Instant.ofEpochSecond(0)))

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("roles", "admin")
            .queryParam("groups", "test")
            .queryParam("identities", IdentityProvider.PASSWORD)
            .queryParam("createdAtAfter", Instant.now().minusSeconds(10))
            .queryParam("createdAtBefore", Instant.now().plusSeconds(10))
            .queryParam("lastActiveAfter", Instant.now().minusSeconds(10))
            .queryParam("lastActiveBefore", Instant.now().plusSeconds(10))
            .queryParam("page", 0)
            .queryParam("size", 4)
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with email`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER))
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("email", user1.email!!)
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with email not existing`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER))
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("email", "no@email.com")
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(0, res.totalElements)
        assertEquals(0, res.content.size)
    }
    @Test fun `getUsers works with roles`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER))
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("roles", Role.ADMIN)
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with multiple roles`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER))
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("roles", "admin,guest")
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with groups`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), groups = listOf("test"))
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("groups", "test")
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with multiple groups`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), groups = listOf("test"))
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("groups", "test", "another")
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with identities`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), )
        registerOAuth2()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("identities", IdentityProvider.PASSWORD)
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with multiple identities`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), )
        registerOAuth2()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("identities", "${IdentityProvider.PASSWORD},another")
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with createdAtAfter`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), )
        val user2 = registerUser()
        userService.save(user2.info.copy(createdAt = Instant.ofEpochSecond(0)))

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("createdAtAfter", Instant.ofEpochSecond(100))
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with createdAtBefore`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), )
        val user2 = registerUser()
        userService.save(user2.info.copy(createdAt = Instant.now().plusSeconds(1000)))

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("createdAtBefore", Instant.now().plusSeconds(1))
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with lastActiveAfter`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), )
        val user2 = registerUser()
        userService.save(user2.info.copy(lastActive = Instant.ofEpochSecond(0)))

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("lastActiveAfter", Instant.ofEpochSecond(100))
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works with lastActiveBefore`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER), )
        val user2 = registerUser()
        userService.save(user2.info.copy(lastActive = Instant.now().plusSeconds(1000)))

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 0)
            .queryParam("size", 4)
            .queryParam("lastActiveBefore", Instant.now().plusSeconds(1))
            .build()
            .toUri()

        val res = webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user1.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserOverviewPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(1, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user1.info.id, res.content.first().id)
    }
    @Test fun `getUsers works needs authentication`() = runTest {
        registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .build()
            .toUri()

        webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `getUsers works needs admin role`() = runTest {
        val user = registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .build()
            .toUri()

        webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }
    @Test fun `getUsers works needs admin role not guest`() = runTest {
        val user = registerUser(roles = listOf(Role.GUEST))

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .build()
            .toUri()

        webTestClient.get()
            .uri("${uri.path}?${uri.query}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }
}
