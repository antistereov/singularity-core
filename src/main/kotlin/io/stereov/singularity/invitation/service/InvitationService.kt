package io.stereov.singularity.invitation.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.core.service.SensitiveCrudService
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.invitation.exception.model.InvalidInvitationException
import io.stereov.singularity.invitation.model.EncryptedInvitationDocument
import io.stereov.singularity.invitation.model.InvitationDocument
import io.stereov.singularity.invitation.model.SensitiveInvitationData
import io.stereov.singularity.invitation.repository.InvitationRepository
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.template.util.TemplateBuilder
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.translate.model.TranslateKey
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.user.service.UserService
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class InvitationService(
    override val repository: InvitationRepository,
    override val encryptionService: EncryptionService,
    override val encryptionSecretService: EncryptionSecretService,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val invitationTokenService: InvitationTokenService,
    private val templateService: TemplateService,
    private val mailService: MailService,
    private val translateService: TranslateService,
    private val userService: UserService,
    private val uiProperties: UiProperties,
) : SensitiveCrudService<SensitiveInvitationData, InvitationDocument, EncryptedInvitationDocument> {

    override val clazz = SensitiveInvitationData::class.java
    override val logger = KotlinLogging.logger {}

    suspend fun invite(
        email: String,
        inviterName: String,
        invitedTo: String,
        acceptPath: String,
        claims: Map<String, Any>,
        issuedAt: Instant = Instant.now(),
        expiresInSeconds: Long = 60 * 60 * 24 * 7, // 1 week
        lang : Language,
    ): InvitationDocument {
        logger.debug { "Inviting \"$email\" with claims: $claims" }

        val invitation = save(email, claims, issuedAt, expiresInSeconds)
        val token = invitationTokenService.createInvitationToken(invitation)
        val subject = translateService.translate(TranslateKey("invitation.subject"), "i18n/core/mail", lang)

        val placeholders = mapOf(
            "inviter_name" to inviterName,
            "invited_to" to invitedTo,
            "expiration_days" to (expiresInSeconds / 60 / 60 / 24).toInt(),
            "accept_url" to uiProperties.baseUrl.removeSuffix("/") + "/" + acceptPath.removePrefix("/"),
            "accept_token" to token
        )
        val template = TemplateBuilder
            .fromResource("templates/mail/invitation.html")
            .translate("i18n/core/mail", lang)
            .replacePlaceholders(templateService.getPlaceholders(placeholders))
            .build()

        // TODO: Register user when invited
        if (userService.existsByEmail(email)) mailService.sendEmail(email, subject, template, lang)

        return invitation
    }

    suspend fun accept(token: String): InvitationDocument {
        logger.debug { "Validating invitation" }

        val id = try {
            invitationTokenService.validateInvitationTokenAndGetId(token)
        } catch (e: Exception) {
            throw InvalidInvitationException(cause = e)
        }

        val invitation = findByIdOrNull(id)
            ?: throw InvalidInvitationException()

        return invitation
    }

    suspend fun save(
        email: String,
        claims: Map<String, Any>,
        issuedAt: Instant,
        expiresInSeconds: Long,
    ): InvitationDocument {

        val expiresAt = issuedAt.plusSeconds(expiresInSeconds)
        val data = SensitiveInvitationData(
            email = email,
            claims = claims
        )
        return save(
            InvitationDocument(
                issuedAt = issuedAt,
                expiresAt = expiresAt,
                sensitive = data,
            )
        )
    }

    private suspend fun removeExpired() {
        logger.debug { "Removing expired invitations" }

        val query = Query(Criteria.where(EncryptedInvitationDocument::expiresAt.name).lte(Instant.now()))

        reactiveMongoTemplate.remove(query).awaitLast()
    }

    @Scheduled(cron = "0 */15 * * * *")
    private suspend fun scheduledCleanup() {
        removeExpired()
    }
}
