package io.stereov.singularity.content.invitation.controller

import io.mockk.verify
import io.stereov.singularity.auth.group.model.KnownGroups
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.test.BaseArticleTest
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.returnResult

class InvitationControllerNoUnregisteredTest : BaseArticleTest() {

    @Test fun `it should not send email`() = runTest {
        val owner = registerUser(groups = listOf(KnownGroups.CONTRIBUTOR))
        val article = saveArticle(owner = owner)

        webTestClient.post()
            .uri("/api/content/articles/invitations/${article.key}")
            .bodyValue(InviteUserToContentRequest(email = "no@email.com", role = ContentAccessRole.MAINTAINER))
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


        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.content.invitations.allow-unregistered-users") { false }
        }
    }
}