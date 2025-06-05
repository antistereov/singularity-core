package io.stereov.singularity.core.invitation.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.database.service.SensitiveCrudService
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.global.service.encryption.service.EncryptionService
import io.stereov.singularity.core.global.service.mail.MailService
import io.stereov.singularity.core.secrets.service.EncryptionSecretService
import io.stereov.singularity.core.global.service.template.TemplateBuilder
import io.stereov.singularity.core.global.service.template.TemplateUtil
import io.stereov.singularity.core.global.service.translate.model.TranslateKey
import io.stereov.singularity.core.global.service.translate.service.TranslateService
import io.stereov.singularity.core.invitation.exception.model.InvalidInvitationException
import io.stereov.singularity.core.invitation.model.EncryptedInvitationDocument
import io.stereov.singularity.core.invitation.model.InvitationDocument
import io.stereov.singularity.core.invitation.model.SensitiveInvitationData
import io.stereov.singularity.core.invitation.repository.InvitationRepository
import io.stereov.singularity.core.properties.UiProperties
import io.stereov.singularity.core.user.service.UserService
import kotlinx.coroutines.reactive.awaitLast
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class InvitationService(
    repository: InvitationRepository,
    encryptionService: EncryptionService,
    encryptionSecretService: EncryptionSecretService,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val invitationTokenService: InvitationTokenService,
    private val templateUtil: TemplateUtil,
    private val mailService: MailService,
    private val translateService: TranslateService,
    private val userService: UserService,
    private val uiProperties: UiProperties,
) : SensitiveCrudService<SensitiveInvitationData, InvitationDocument, EncryptedInvitationDocument>(
    repository,
    encryptionSecretService,
    encryptionService
) {

    override val clazz: Class<SensitiveInvitationData>
        get() = SensitiveInvitationData::class.java

    private val logger: KLogger
        get() = KotlinLogging.logger {}

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
            .replacePlaceholders(templateUtil.getPlaceholders(placeholders))
            .build()

        // TODO: Register user when invited
        if (userService.existsByEmail(email)) mailService.sendEmail(email, subject, template, true)

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
