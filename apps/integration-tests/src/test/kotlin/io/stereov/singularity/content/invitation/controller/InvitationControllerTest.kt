package io.stereov.singularity.content.invitation.controller

import io.mockk.verify
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.util.MockFilePart
import io.stereov.singularity.test.BaseArticleTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.test.web.reactive.server.returnResult
import java.time.Instant

class InvitationControllerTest() : BaseArticleTest() {

    // Invite

    @Test fun `invite needs content type`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        webTestClient.post()
            .uri("/api/content/oops/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `invite works with article and owner`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(1, updatedArticle.access.invitations.size)
        val invitationId = updatedArticle.access.invitations.first()
        assertNotNull(invitationService.findById(invitationId))
        assertEquals(1, invitationService.findAll().count())

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite works with article and maintainer`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val article = saveArticle()
        article.access.users.maintainer.add(owner.info.id.toString())
        articleService.save(article)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(1, updatedArticle.access.invitations.size)
        val invitationId = updatedArticle.access.invitations.first()
        assertNotNull(invitationService.findById(invitationId))
        assertEquals(1, invitationService.findAll().count())

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to article works with unregistered`() = runTest {
        val owner = registerUser()
        val article = saveArticle()
        article.access.users.maintainer.add(owner.info.id.toString())
        articleService.save(article)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = "another@email.com", role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(1, updatedArticle.access.invitations.size)
        val invitationId = updatedArticle.access.invitations.first()
        assertNotNull(invitationService.findById(invitationId))
        assertEquals(1, invitationService.findAll().count())

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to article requires maintainer`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val article = saveArticle()
        article.access.users.editor.add(owner.info.id.toString())
        articleService.save(article)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(0, updatedArticle.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to article requires authentication`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val article = saveArticle()
        article.access.users.editor.add(owner.info.id.toString())
        articleService.save(article)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .exchange()
            .expectStatus().isUnauthorized

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(0, updatedArticle.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to article requires article`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val article = saveArticle()
        article.access.users.editor.add(owner.info.id.toString())
        articleService.save(article)

        webTestClient.post()
            .uri("/api/content/articles/invitations/not-found")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .exchange()
            .expectStatus().isNotFound

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(0, updatedArticle.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to article requires body`() = runTest {
        val owner = registerUser()
        val article = saveArticle()
        article.access.users.editor.add(owner.info.id.toString())
        articleService.save(article)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .exchange()
            .expectStatus().isBadRequest

        val updatedArticle = articleService.findByKey(article.key)
        assertEquals(0, updatedArticle.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test fun `invite works with file metadata and owner`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedFile = fileMetadataService.findByKey(key.key)
        assertEquals(1, updatedFile.access.invitations.size)
        val invitationId = updatedFile.access.invitations.first()
        val invitation = invitationService.findById(invitationId)

        assertEquals(invited.email, invitation.sensitive.email)
        assertEquals(1, invitationService.findAll().count())

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite works with file metadata and maintainer`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)
        metadata.access.users.maintainer.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updated = fileMetadataService.findByKey(metadata.key)
        assertEquals(1, updated.access.invitations.size)
        val invitationId = updated.access.invitations.first()
        assertNotNull(invitationService.findById(invitationId))
        assertEquals(1, invitationService.findAll().count())

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to file works with unregistered`() = runTest {
        val owner = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)
        metadata.access.users.maintainer.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = "another@email.com", role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updated = fileMetadataService.findByKey(metadata.key)
        assertEquals(1, updated.access.invitations.size)
        val invitationId = updated.access.invitations.first()
        assertNotNull(invitationService.findById(invitationId))
        assertEquals(1, invitationService.findAll().count())

        verify { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to file metadata requires maintainer`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, invited.info.id, true)
        metadata.access.users.editor.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updated = fileMetadataService.findByKey(key.key)
        assertEquals(0, updated.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to file metadata requires authentication`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)
        metadata.access.users.maintainer.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .exchange()
            .expectStatus().isUnauthorized

        val updated = fileMetadataService.findByKey(key.key)
        assertEquals(0, updated.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to file metadata requires file`() = runTest {
        val owner = registerUser()
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)
        metadata.access.users.maintainer.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/not-this")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound

        val updated = fileMetadataService.findByKey(key.key)
        assertEquals(0, updated.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }
    @Test fun `invite to file metadata requires body`() = runTest {
        val owner = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)
        metadata.access.users.maintainer.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isBadRequest

        val updated = fileMetadataService.findByKey(key.key)
        assertEquals(0, updated.access.invitations.size)
        assertEquals(0, invitationService.findAll().count())

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    // Accept

    @Test fun `accept needs content type`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = articleManagementService.contentType,
            contentKey = article.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to article.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null
        )

        articleService.save(article.addInvitation(invitation))
        val token = invitationTokenService.create(invitation)

        webTestClient.post()
            .uri("/api/content/oops/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isNotFound
    }
    @Test fun `accept needs correct content type`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = fileMetadataManagementService.contentType,
            contentKey = article.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to article.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null
        )

        articleService.save(article.addInvitation(invitation))
        val token = invitationTokenService.create(invitation)

         webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isUnauthorized

        val updated = articleService.findByKey(article.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }

    @Test fun `accept invite to article works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = articleManagementService.contentType,
            contentKey = article.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to article.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null
        )

        articleService.save(article.addInvitation(invitation))
        val token = invitationTokenService.create(invitation)

        val res = webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isOk
            .returnResult<FullArticleResponse>()
            .responseBody
            .awaitFirst()

        val updated = articleService.findByKey(article.key)

        assertEquals(article.key, res.key)
        assertTrue(updated.access.invitations.isEmpty())
        assertTrue(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to article needs valid token`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = articleManagementService.contentType,
            contentKey = article.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to article.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null
        )

        articleService.save(article.addInvitation(invitation))

        webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest("invalid"))
            .exchange()
            .expectStatus().isUnauthorized

        val updated = articleService.findByKey(article.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to article needs unexpired token`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = articleManagementService.contentType,
            contentKey = article.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to article.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null,
            issuedAt = Instant.ofEpochSecond(0)
        )
        val token = invitationTokenService.create(invitation)
        articleService.save(article.addInvitation(invitation))

        webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isUnauthorized

        val updated = articleService.findByKey(article.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to article needs article`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = articleManagementService.contentType,
            contentKey = article.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to "no-key", "role" to ContentAccessRole.MAINTAINER),
            locale = null,
        )
        val token = invitationTokenService.create(invitation)
        articleService.save(article.addInvitation(invitation))

        webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isNotFound

        val updated = articleService.findByKey(article.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to article needs user`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val article = saveArticle(owner)

        val invitation = invitationService.invite(
            contentType = articleManagementService.contentType,
            contentKey = article.key,
            email = "no@email.com",
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to article.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null,
        )
        val token = invitationTokenService.create(invitation)
        articleService.save(article.addInvitation(invitation))

        webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isNotFound

        val updated = articleService.findByKey(article.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }

    @Test fun `accept invite to file works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        val invitation = invitationService.invite(
            contentType = fileMetadataManagementService.contentType,
            contentKey = metadata.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to metadata.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null
        )

        fileMetadataService.save(fileMetadataMapper.toDocument(metadata.addInvitation(invitation)))
        val token = invitationTokenService.create(invitation)

        val res = webTestClient.post()
            .uri("/api/content/files/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isOk
            .returnResult<FileMetadataResponse>()
            .responseBody
            .awaitFirst()

        val updated = fileMetadataService.findByKey(metadata.key)

        assertEquals(metadata.key, res.key)
        assertTrue(updated.access.invitations.isEmpty())
        assertTrue(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to file needs valid token`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        val invitation = invitationService.invite(
            contentType = fileMetadataManagementService.contentType,
            contentKey = metadata.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to metadata.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null
        )

        fileMetadataService.save(fileMetadataMapper.toDocument(metadata.addInvitation(invitation)))

        webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest("invalid"))
            .exchange()
            .expectStatus().isUnauthorized

        val updated = fileMetadataService.findByKey(metadata.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to file needs unexpired token`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        val invitation = invitationService.invite(
            contentType = fileMetadataManagementService.contentType,
            contentKey = metadata.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to metadata.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null,
            issuedAt = Instant.ofEpochSecond(0)
        )
        val token = invitationTokenService.create(invitation)

        fileMetadataService.save(fileMetadataMapper.toDocument(metadata.addInvitation(invitation)))

        webTestClient.post()
            .uri("/api/content/articles/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isUnauthorized

        val updated = fileMetadataService.findByKey(metadata.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to file needs file`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        val invitation = invitationService.invite(
            contentType = fileMetadataManagementService.contentType,
            contentKey = metadata.key,
            email = invited.email!!,
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf(
                "key" to "no-key",
                "role" to ContentAccessRole.MAINTAINER),
            locale = null,
        )
        val token = invitationTokenService.create(invitation)
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata.addInvitation(invitation)))

        webTestClient.post()
            .uri("/api/content/files/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isNotFound

        val updated = fileMetadataService.findByKey(metadata.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `accept invite to file needs user`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        val invitation = invitationService.invite(
            contentType = fileMetadataManagementService.contentType,
            contentKey = metadata.key,
            email = "no@email.com",
            inviterName = owner.info.sensitive.name,
            invitedTo = "invitedTo",
            claims = mapOf("key" to metadata.key, "role" to ContentAccessRole.MAINTAINER),
            locale = null,
        )
        val token = invitationTokenService.create(invitation)
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata.addInvitation(invitation)))

        webTestClient.post()
            .uri("/api/content/files/invitations/accept")
            .bodyValue(AcceptInvitationToContentRequest(token.value))
            .exchange()
            .expectStatus().isNotFound

        val updated = fileMetadataService.findByKey(metadata.key)
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }

    // Delete

    @Test fun `delete works`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedFile = fileMetadataService.findByKey(key.key)
        assertEquals(1, updatedFile.access.invitations.size)
        val invitationId = updatedFile.access.invitations.first()

        webTestClient.delete()
            .uri("/api/content/invitations/${invitationId}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk

        val updated = fileMetadataService.findByKey(metadata.key)
        assertFalse(invitationService.existsById(invitationId))
        assertTrue(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `delete requires authorization`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedFile = fileMetadataService.findByKey(key.key)
        assertEquals(1, updatedFile.access.invitations.size)
        val invitationId = updatedFile.access.invitations.first()

        webTestClient.delete()
            .uri("/api/content/invitations/${invitationId}")
            .exchange()
            .expectStatus().isUnauthorized

        val updated = fileMetadataService.findByKey(metadata.key)
        assertTrue(invitationService.existsById(invitationId))
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `delete requires maintainer`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, invited.info.id, true)
        metadata.access.users.editor.add(owner.info.id.toString())
        fileMetadataService.save(fileMetadataMapper.toDocument(metadata))

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(invited.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedFile = fileMetadataService.findByKey(key.key)
        assertEquals(1, updatedFile.access.invitations.size)
        val invitationId = updatedFile.access.invitations.first()

        webTestClient.delete()
            .uri("/api/content/invitations/${invitationId}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updated = fileMetadataService.findByKey(metadata.key)
        assertTrue(invitationService.existsById(invitationId))
        assertFalse(updated.access.invitations.isEmpty())
        assertFalse(updated.access.users.maintainer.contains(invited.info.id.toString()))
    }
    @Test fun `delete requires file`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedFile = fileMetadataService.findByKey(key.key)
        assertEquals(1, updatedFile.access.invitations.size)
        val invitationId = updatedFile.access.invitations.first()
        fileStorage.remove(key)

        webTestClient.delete()
            .uri("/api/content/invitations/${invitationId}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }
    @Test fun `delete requires invitation`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val invited = registerUser()
        val file = ClassPathResource("files/test-image.jpg").file
        val filePart = MockFilePart(file)
        val key = FileKey("file1.jpg")
        val metadata = fileStorage.upload(key, filePart, owner.info.id, true)

        webTestClient.post()
            .uri("/api/content/files/invitations/${metadata.key}")
            .bodyValue(InviteUserToContentRequest(email = invited.email!!, role = ContentAccessRole.MAINTAINER))
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isOk
            .returnResult<ExtendedContentAccessDetailsResponse>()
            .responseBody
            .awaitFirstOrNull()

        val updatedFile = fileMetadataService.findByKey(key.key)
        assertEquals(1, updatedFile.access.invitations.size)
        val invitationId = updatedFile.access.invitations.first()
        invitationService.deleteById(invitationId)

        webTestClient.delete()
            .uri("/api/content/invitations/${invitationId}")
            .accessTokenCookie(owner.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

}