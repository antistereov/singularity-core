package io.stereov.singularity.user.core.controller

import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.web.util.UriComponentsBuilder

class UserControllerTest : BaseIntegrationTest() {

    @Test fun `getAccount returns user account`() = runTest {
        val user = registerUser()

        val responseBody = webTestClient.get()
            .uri("/api/users/me")
            .header(HttpHeaders.COOKIE, "${SessionTokenType.Access.cookieName}=${user.accessToken}")
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(responseBody) { "Response has empty body" }

        assertEquals(user.info.sensitive.email, responseBody.email)
    }
    @Test fun `getAccount needs authentication`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `checkAuthentication requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `checkAuthentication returns user`() = runTest {
        val user = registerUser()

        val response = webTestClient.get()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(response) { "Response body is empty" }

        assertEquals(user.info.id, response.id)
    }

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

    @Test fun `getUsers works`() = runTest {
        val user1 = registerUser(roles = listOf(Role.ADMIN, Role.USER))
        val user2 = registerUser()
        val user3 = registerUser()
        val user4 = registerUser()
        val user5 = registerUser()

        val uri = UriComponentsBuilder.fromUriString("/api/users")
            .queryParam("sort", "createdAt,asc")
            .queryParam("page", 1)
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

        assertEquals(5, res.totalElements)
        assertEquals(1, res.content.size)
        assertEquals(user5.info.id, res.content.first().id)
    }
}