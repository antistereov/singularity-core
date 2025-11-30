package io.stereov.singularity.principal.settings.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.settings.dto.request.ChangeEmailRequest
import io.stereov.singularity.principal.settings.dto.request.ChangePasswordRequest
import io.stereov.singularity.principal.settings.dto.request.ChangePrincipalRequest
import io.stereov.singularity.principal.settings.dto.response.ChangeEmailResponse
import io.stereov.singularity.test.BaseMailIntegrationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import java.io.File
import java.net.URI
import java.time.Instant
import java.util.*

class UserSettingsControllerTest() : BaseMailIntegrationTest() {

    @Autowired
    lateinit var localFileStorageProperties: LocalFileStorageProperties

    @Test fun `get returns user account`() = runTest {
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
    @Test fun `get needs authentication`() = runTest {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test fun `changeEmail changes email`() = runTest {
        val newEmail = "new@email.com"
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(ChangeEmailResponse::class.java)
            .returnResult()
            .responseBody

        val token = emailVerificationTokenService.create(user.id, newEmail, user.info.sensitive.security.email.verificationSecret)
            .getOrThrow()
        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findByEmail(newEmail).getOrThrow()
        assertEquals(user.id, foundUser.id)
    }
    @Test fun `changeEmail changes email when old is verified`() = runTest {
        val newEmail = "new@email.com"
        val user = registerUser()
        user.info.sensitive.security.email.verified = true
        userService.save(user.info)

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(ChangeEmailResponse::class.java)
            .returnResult()
            .responseBody

        val token = emailVerificationTokenService.create(user.id, newEmail, user.info.sensitive.security.email.verificationSecret)
            .getOrThrow()
        webTestClient.post()
            .uri("/api/auth/email/verification?token=$token")
            .exchange()
            .expectStatus().isOk

        val foundUser = userService.findByEmail(newEmail).getOrThrow()
        assertEquals(user.id, foundUser.id)
    }
    @Test fun `changeEmail requires authentication`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .bodyValue(ChangeEmailRequest(newEmail))
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires body`() = runTest {
        val oldEmail = "old@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password)

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `changeEmail requires step up`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same user`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, totpEnabled = true)
        val anotherUser = registerUser("ttest@email.com")

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(anotherUser.id, user.sessionId).getOrThrow().value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires step up token for same session`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.id, UUID.randomUUID()).getOrThrow().value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires unexpired step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(
                    user.id,
                    user.sessionId,
                    Instant.ofEpochSecond(0)
                ).getOrThrow().value
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires valid step up token`() = runTest {
        val oldEmail = "old@email.com"
        val newEmail = "new@email.com"
        val password = "Password$2"
        val user = registerUser(oldEmail, password, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                "wrong-token"
            )
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `changeEmail requires non-existing email`() = runTest {
        val user = registerUser()
        val anotherUser = registerUser()

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(anotherUser.email!!))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
    @Test fun `changeEmail does nothing without validation`() = runTest {
        val user = registerUser()
        val newEmail = "new@email.com"

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk
            .expectBody(ChangeEmailResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)
        assertTrue(res.verificationRequired)
        val foundUser = userService.findByEmail(user.email!!).getOrThrow()
        assertEquals(user.id, foundUser.id)
    }
    @Test fun `changeEmail requires valid email`() = runTest {
        val user = registerUser()
        val newEmail = "invalid"

        val res = webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isBadRequest

        requireNotNull(res)
        val foundUser = userService.findByEmail(user.email!!).getOrThrow()
        assertEquals(user.id, foundUser.id)
    }
    @Test fun `changeEmail throws cooldown`() = runTest {
        val newEmail = "new@email.com"
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isOk

        webTestClient.put()
            .uri("/api/users/me/email")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .bodyValue(ChangeEmailRequest(newEmail))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
    }

    @Test fun `changePassword works`() = runTest {
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser( password = oldPassword, totpEnabled = true)

        val res = webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.id, user.sessionId).getOrThrow().value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        webTestClient.post()
            .uri("/api/auth/login")
            .bodyValue(LoginRequest(user.email!!, newPassword, SessionInfoRequest("session")))
            .exchange()
            .expectStatus().isOk
    }
    @Test fun `changePassword requires authentication`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires capital letter`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newpassword$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires small letter`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "PASSWORD$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires at least 8 characters`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "Pas$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires a number`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "Password$"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires a special character`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "Password2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires body`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val user = registerUser(email, oldPassword)

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires correct password`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword)
        gAuth.getTotpPassword(user.totpSecret)

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .bodyValue(ChangePasswordRequest("wrong-password", newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires step up`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires step up token for same user`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, totpEnabled = true)
        val anotherUser = registerUser("another@email.com")

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(anotherUser.id, user.sessionId).getOrThrow().value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires step up token for same session`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(user.id, UUID.randomUUID()).getOrThrow().value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires unexpired step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .cookie(
                SessionTokenType.StepUp.cookieName,
                stepUpTokenService.create(
                    user.id,
                    user.sessionId,
                    Instant.ofEpochSecond(0)
                ).getOrThrow().value
            )
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }
    @Test fun `changePassword requires valid step up token`() = runTest {
        val email = "old@email.com"
        val oldPassword = "Password$2"
        val newPassword = "newPassword$2"
        val user = registerUser(email, oldPassword, totpEnabled = true)

        webTestClient.put()
            .uri("/api/users/me/password")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie("wrong-token")
            .bodyValue(ChangePasswordRequest(oldPassword, newPassword))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertTrue(hashService.checkBcrypt(oldPassword, foundUser.password.getOrThrow()).getOrThrow())
    }

    @Test fun `changeUser works`() = runTest {
        val user = registerUser()
        val newName = "MyName"
        val accessToken = user.accessToken

        val res = webTestClient.put()
            .uri("/api/users/me")
            .accessTokenCookie(accessToken)
            .bodyValue(ChangePrincipalRequest(newName))
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        assertEquals(newName, res.name)
        assertEquals(newName, userService.findById(user.id).getOrThrow().sensitive.name)
    }
    @Test fun `changeUser requires authentication`() = runTest {
        val user = registerUser()
        val newName = "MyName"

        webTestClient.put()
            .uri("/api/users/me")
            .bodyValue(ChangePrincipalRequest(newName))
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertEquals(user.info.sensitive.name, foundUser.sensitive.name)
    }
    @Test fun `changeUser requires body`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken

        webTestClient.put()
            .uri("/api/users/me")
            .accessTokenCookie(accessToken)
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertEquals(user.info.sensitive.name, foundUser.sensitive.name)
    }

    @Test fun `setAvatar works`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken
        val file = ClassPathResource("files/test-image.jpg")

        val res = webTestClient.put()
            .uri("/api/users/me/avatar")
            .accessTokenCookie(accessToken)
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

        val imageRenditions = requireNotNull(res?.avatar).renditions

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
    @Test fun `setAvatar removes old avatar`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken
        val file = ClassPathResource("files/test-image.jpg")

        val res1 = webTestClient.put()
            .uri("/api/users/me/avatar")
            .accessTokenCookie(accessToken)
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

        val res = webTestClient.put()
            .uri("/api/users/me/avatar")
            .accessTokenCookie(accessToken)
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

        val imageRenditions = requireNotNull(res?.avatar).renditions

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

        val imageRenditionsOld = requireNotNull(res1?.avatar).renditions

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
    @Test fun `setAvatar requires authentication`() = runTest {
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/avatar")
            .bodyValue(
                MultipartBodyBuilder().apply {
                    part("file", ClassPathResource("files/test-image.jpg"))
                }.build()
            )
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertNull(foundUser.sensitive.avatarFileKey)
    }
    @Test fun `setAvatar requires body`() = runTest {
        val user = registerUser()

        webTestClient.put()
            .uri("/api/users/me/avatar")
            .exchange()
            .expectStatus().isBadRequest

        val foundUser = userService.findById(user.id).getOrThrow()
        assertNull(foundUser.sensitive.avatarFileKey)
    }

    @Test fun `deleteAvatar works`() = runTest {
        val user = registerUser()
        val accessToken = user.accessToken
        val file = ClassPathResource("files/test-image.jpg")

        val res = webTestClient.put()
            .uri("/api/users/me/avatar")
            .accessTokenCookie(accessToken)
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
            .uri("/api/users/me/avatar")
            .accessTokenCookie(accessToken)
            .exchange()
            .expectStatus().isOk

        val imageRenditions = requireNotNull(res?.avatar).renditions

        // Small
        val small = requireNotNull(imageRenditions[ImageProperties::small.name])
        val smallFile = File(localFileStorageProperties.fileDirectory, small.key)
        assertFalse(smallFile.exists())
        // Medium
        val medium = requireNotNull(imageRenditions[ImageProperties::medium.name])
        val mediumFile = File(localFileStorageProperties.fileDirectory, medium.key)
        assertFalse(mediumFile.exists())
        // Large
        val large = requireNotNull(imageRenditions[ImageProperties::large.name])
        val largeFile = File(localFileStorageProperties.fileDirectory, large.key)
        assertFalse(largeFile.exists())
        // Original
        val original = requireNotNull(imageRenditions[FileMetadataDocument.ORIGINAL_RENDITION])
        val originalFile = File(localFileStorageProperties.fileDirectory, original.key)
        assertFalse(originalFile.exists())

        val foundUser = userService.findById(user.id).getOrThrow()
        assertNull(foundUser.sensitive.avatarFileKey)
    }
    @Test fun `deleteAvatar requires authentication`() = runTest {
        val user = registerUser()
        user.info.sensitive.avatarFileKey = "avatar"
        userService.save(user.info)


        webTestClient.delete()
            .uri("/api/users/me/avatar")
            .exchange()
            .expectStatus().isUnauthorized

        val foundUser = userService.findById(user.id).getOrThrow()
        assertEquals("avatar", foundUser.sensitive.avatarFileKey)
    }

    @Test fun `delete works`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(userService.findAll().getOrThrow().toList().isEmpty())
    }
    @Test fun `delete deletes avatar`() = runTest {
        val user = registerUser()

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
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isOk

        assertTrue(userService.findAll().getOrThrow().toList().isEmpty())

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
    @Test fun `delete requires authentication`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/me")
            .stepUpTokenCookie(user.stepUpToken)
            .exchange()
            .expectStatus().isUnauthorized

        assertEquals(1, userService.findAll().getOrThrow().toList().size)
        val foundUser = userService.findById(user.id).getOrThrow()
        assertEquals(user.id, foundUser.id.getOrThrow())
    }
    @Test fun `delete requires stepUp`() = runTest {
        val user = registerUser()

        webTestClient.delete()
            .uri("/api/users/me")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isUnauthorized

        assertEquals(1, userService.findAll().getOrThrow().toList().size)
        val foundUser = userService.findById(user.id).getOrThrow()
        assertEquals(user.id, foundUser.id.getOrThrow())
    }
}
