package io.stereov.singularity.auth.oauth2.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.response.IdentityProviderResponse
import io.stereov.singularity.auth.oauth2.dto.request.AddPasswordAuthenticationRequest
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.exception.UserException
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdentityProviderControllerTest : BaseIntegrationTest() {

    @Test fun `get works`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider("id")
        userService.save(user.info)

        val res = webTestClient.get()
            .uri("/api/users/me/providers")
            .accessTokenCookie(user.accessToken)
            .exchange().expectStatus().isOk
            .expectBody(List::class.java)
            .returnResult()

        val body = requireNotNull(res.responseBody)

        Assertions.assertTrue( body.map { objectMapper.convertValue(it, IdentityProviderResponse::class.java) }
            .containsAll(
                listOf(
                    IdentityProviderResponse(UserIdentity.PASSWORD_IDENTITY),
                    IdentityProviderResponse("github"))
            ))
    }
    @Test fun `get does not works with guest`() = runTest {
        val user = createGuest()

        webTestClient.get()
            .uri("/api/users/me/providers")
            .accessTokenCookie(user.accessToken)
            .exchange().expectStatus().isNotFound
    }
    @Test fun `get requires access token`() = runTest {
        webTestClient.get()
            .uri("/api/users/me/providers")
            .exchange().expectStatus().isUnauthorized
    }

    @Test fun `addPassword works`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("Password$2")

        val res = webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()

        val body = requireNotNull(res.responseBody)

        Assertions.assertTrue(body.identityProviders.contains(UserIdentity.PASSWORD_IDENTITY))

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertTrue(updatedUser.sensitive.identities.password != null)
        Assertions.assertTrue(hashService.checkBcrypt(req.password, updatedUser.password.getOrThrow()).getOrThrow())

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, req.password))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `addPassword requires password of min 8 characters`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("Pas$2")

         webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
             .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword requires passwort with lower case letter`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("PASSWORD$2")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword requires password with upper-case letter`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("password$2")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword requires password with number`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("Password$")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword requires password with special character`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("Password2")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword needs body`() = runTest {
        val user = registerOAuth2()

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword needs access and step-up`() = runTest {
        val user = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("Password$2")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
    @Test fun `addPassword does not allow guests`() = runTest {
        val user = createGuest()
        val req = AddPasswordAuthenticationRequest("Password$2")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotFound
    }
    @Test fun `addPassword returns not modified if already set up`() = runTest {
        val oauth2User = registerOAuth2()
        val req = AddPasswordAuthenticationRequest("Password$2")

        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(oauth2User.accessToken)
            .stepUpTokenCookie(oauth2User.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(oauth2User.accessToken)
            .stepUpTokenCookie(oauth2User.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotModified

        val user = registerUser()
        webTestClient.post()
            .uri("/api/users/me/providers/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isNotModified
    }

    @Test fun `delete works`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider("id")
        userService.save(user.info)

        val res = webTestClient.delete()
            .uri("/api/users/me/providers/github")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()

        val body = requireNotNull(res.responseBody)

        Assertions.assertEquals(listOf(UserIdentity.PASSWORD_IDENTITY), body.identityProviders)

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertTrue(updatedUser.sensitive.identities.password != null)
        Assertions.assertTrue(hashService.checkBcrypt(user.password!!, updatedUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `delete requires existing identity`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider("id")
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/users/me/providers/git")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isNotFound

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(1, updatedUser.sensitive.identities.providers.size)
        Assertions.assertTrue(updatedUser.sensitive.identities.password != null)
        Assertions.assertTrue(updatedUser.sensitive.identities.providers.contains("github"))
        Assertions.assertTrue(hashService.checkBcrypt(user.password!!, updatedUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `delete requires access and step up`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider("id")
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/users/me/providers/github")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.delete()
            .uri("/api/users/me/providers/github")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        webTestClient.delete()
            .uri("/api/users/me/providers/github")
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(1, updatedUser.sensitive.identities.providers.size)
        Assertions.assertTrue(updatedUser.sensitive.identities.password != null)
        Assertions.assertTrue(updatedUser.sensitive.identities.providers.contains("github"))
        Assertions.assertTrue(hashService.checkBcrypt(user.password!!, updatedUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `delete cannot delete password`() = runTest {
        val user = registerUser()
        user.info.sensitive.identities.providers["github"] = UserIdentity.ofProvider("id")
        userService.save(user.info)

        webTestClient.delete()
            .uri("/api/users/me/providers/${UserIdentity.PASSWORD_IDENTITY}")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(1, updatedUser.sensitive.identities.providers.size)
        Assertions.assertTrue(updatedUser.sensitive.identities.password != null)
        Assertions.assertTrue(updatedUser.sensitive.identities.providers.contains("github"))
        Assertions.assertTrue(hashService.checkBcrypt(user.password!!, updatedUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `delete cannot delete last identity`() = runTest {
        val user = registerOAuth2()

        webTestClient.delete()
            .uri("/api/users/me/providers/github")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertFalse(updatedUser.sensitive.identities.password != null)
        assertThrows<UserException.NoPassword> { updatedUser.password.getOrThrow() }
    }
}
