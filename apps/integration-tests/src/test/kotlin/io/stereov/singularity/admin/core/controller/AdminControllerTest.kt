package io.stereov.singularity.admin.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AdminControllerTest : BaseIntegrationTest() {

    @Test
    fun `grantAdminPermissions works`() = runTest {
        val admin = createAdmin()
        val user = registerUser()

        val res = webTestClient.post()
            .uri("/api/admins/${user.info.id.getOrThrow()}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(res.id, user.info.id)
        Assertions.assertTrue(res.roles.size == 2)
        Assertions.assertTrue(res.roles.containsAll(setOf(Role.User.ADMIN, Role.User.USER)))

        val savedUser = userService.findById(user.info.id.getOrThrow()).getOrThrow()

        Assertions.assertEquals(savedUser.id, user.info.id)
        Assertions.assertTrue(savedUser.roles.size == 2)
        Assertions.assertTrue(savedUser.roles.containsAll(setOf(Role.User.ADMIN, Role.User.USER)))
    }
    @Test fun `grantAdminPermissions changes nothing for admins`() = runTest {
        val admin = createAdmin()

        val res = webTestClient.post()
            .uri("/api/admins/${admin.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(res.id, admin.info.id)
        Assertions.assertTrue(res.roles.size == 2)
        Assertions.assertTrue(res.roles.containsAll(setOf(Role.User.ADMIN, Role.User.USER)))

        val savedUser = userService.findById(admin.info.id.getOrThrow()).getOrThrow()

        Assertions.assertEquals(savedUser.id, admin.info.id)
        Assertions.assertTrue(savedUser.roles.size == 2)
        Assertions.assertTrue(savedUser.roles.containsAll(setOf(Role.User.ADMIN, Role.User.USER)))
    }
    @Test fun `grantAdminPermissions requires authentication`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/admins/${user.info.id.getOrThrow()}")
            .exchange()
            .expectStatus().isUnauthorized

        val savedUser = userService.findById(user.info.id.getOrThrow()).getOrThrow()

        Assertions.assertTrue(savedUser.roles.size == 1)
        Assertions.assertTrue(savedUser.roles.contains(Role.User.USER))
    }
    @Test fun `grantAdminPermissions requires admin permissions`() = runTest {
        val user = registerUser()

        webTestClient.post()
            .uri("/api/admins/${user.info.id.getOrThrow()}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val savedUser = userService.findById(user.info.id.getOrThrow()).getOrThrow()

        Assertions.assertTrue(savedUser.roles.size == 1)
        Assertions.assertTrue(savedUser.roles.contains(Role.User.USER))
    }
    @Test fun `grantAdminPermissions requires does not work for guests`() = runTest {
        val admin = createAdmin()
        val guest = createGuest()

        webTestClient.post()
            .uri("/api/admins/${guest.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        val savedUser = principalService.findById(guest.info.id.getOrThrow()).getOrThrow()

        Assertions.assertTrue(savedUser.roles.size == 1)
        Assertions.assertTrue(savedUser.roles.contains(Role.Guest.GUEST))
    }
    @Test fun `grantAdminPermissions requires valid id`() = runTest {
        val admin = createAdmin()

        webTestClient.post()
            .uri("/api/admins/not-valid")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `revokeAdminPermissions works`() = runTest {
        val admin = createAdmin()
        val anotherAdmin = createAdmin("another@admin.com")

        val res = webTestClient.delete()
            .uri("/api/admins/${anotherAdmin.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(res.id, anotherAdmin.info.id)
        Assertions.assertTrue(res.roles.size == 1)
        Assertions.assertTrue(res.roles.contains(Role.User.USER))

        val savedUser = userService.findById(anotherAdmin.info.id.getOrThrow()).getOrThrow()

        Assertions.assertEquals(savedUser.id, anotherAdmin.info.id)
        Assertions.assertTrue(res.roles.size == 1)
        Assertions.assertTrue(res.roles.contains(Role.User.USER))
    }
    @Test fun `revokeAdminPermissions requires authentication`() = runTest {
        val anotherAdmin = createAdmin("another@admin.com")

        webTestClient.delete()
            .uri("/api/admins/${anotherAdmin.info.id}")
            .exchange()
            .expectStatus().isUnauthorized

        val savedUser = userService.findById(anotherAdmin.info.id.getOrThrow()).getOrThrow()

        Assertions.assertTrue(savedUser.roles.size == 2)
        Assertions.assertTrue(savedUser.roles.containsAll(setOf(Role.User.USER, Role.User.ADMIN)))
    }
    @Test fun `revokeAdminPermissions requires admin permissions`() = runTest {
        val user = registerUser()
        val anotherAdmin = createAdmin()

        webTestClient.delete()
            .uri("/api/admins/${anotherAdmin.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val savedUser = userService.findById(anotherAdmin.info.id.getOrThrow()).getOrThrow()

        Assertions.assertTrue(savedUser.roles.size == 2)
        Assertions.assertTrue(savedUser.roles.containsAll(setOf(Role.User.USER, Role.User.ADMIN)))
    }
    @Test fun `revokeAdminPermissions requires valid id`() = runTest {
        val admin = createAdmin()

        webTestClient.post()
            .uri("/api/admins/not-valid")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `revokeAdminPermissions for last admin account is conflict`() = runTest {
        val admin = createAdmin()

        webTestClient.delete()
            .uri("/api/admins/${admin.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
}
