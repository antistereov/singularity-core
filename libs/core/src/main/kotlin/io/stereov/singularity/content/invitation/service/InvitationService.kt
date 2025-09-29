package io.stereov.singularity.content.invitation.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.content.invitation.exception.model.InvalidInvitationException
import io.stereov.singularity.content.invitation.model.EncryptedInvitationDocument
import io.stereov.singularity.content.invitation.model.InvitationDocument
import io.stereov.singularity.content.invitation.model.SensitiveInvitationData
import io.stereov.singularity.content.invitation.properties.InvitationProperties
import io.stereov.singularity.content.invitation.repository.InvitationRepository
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactive.awaitLast
import org.bson.types.ObjectId
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class InvitationService(
    override val repository: InvitationRepository,
    override val encryptionService: EncryptionService,
    override val encryptionSecretService: EncryptionSecretService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val invitationTokenService: InvitationTokenService,
    private val templateService: TemplateService,
    private val emailService: EmailService,
    private val translateService: TranslateService,
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val invitationProperties: InvitationProperties,
    private val applicationContext: ApplicationContext,
) : SensitiveCrudService<SensitiveInvitationData, InvitationDocument, EncryptedInvitationDocument>() {

    override val encryptedDocumentClazz = EncryptedInvitationDocument::class.java
    override val sensitiveClazz = SensitiveInvitationData::class.java
    override val logger = KotlinLogging.logger {}

    override suspend fun doDecrypt(
        encrypted: EncryptedInvitationDocument,
        decryptedSensitive: SensitiveInvitationData
    ): InvitationDocument {
        return InvitationDocument(
            encrypted._id,
            encrypted.issuedAt,
            encrypted.expiresAt,
            decryptedSensitive
        )
    }

    override suspend fun doEncrypt(
        document: InvitationDocument,
        encryptedSensitive: Encrypted<SensitiveInvitationData>
    ): EncryptedInvitationDocument {
        return EncryptedInvitationDocument(
            document._id,
            document.issuedAt,
            document.expiresAt,
            encryptedSensitive
        )
    }

    suspend fun invite(
        contentType: String,
        contentKey: String,
        email: String,
        inviterName: String,
        invitedTo: String,
        claims: Map<String, Any>,
        issuedAt: Instant = Instant.now(),
        expiresInSeconds: Long = 60 * 60 * 24 * 7, // 1 week
        locale : Locale?,
    ): InvitationDocument {
        logger.debug { "Inviting \"$email\" with claims: $claims" }

        val acceptUrl = invitationProperties.acceptUrl
            .replace("{contentType}", contentType)
            .replace("{contentKey}", contentKey)

        val actualLocale = locale ?: appProperties.locale

        val claimsWithContentType = claims.toMutableMap()
        claimsWithContentType["contentType"] = contentType

        val invitation = save(email, claimsWithContentType, issuedAt, expiresInSeconds)
        val token = invitationTokenService.create(invitation)
        val subject = translateService.translateResourceKey(TranslateKey("invitation.subject"), "i18n/core/email", locale)

        val placeholders = mapOf(
            "inviter_name" to inviterName,
            "invited_to" to invitedTo,
            "expiration_days" to (expiresInSeconds / 60 / 60 / 24).toInt(),
            "accept_url" to acceptUrl,
            "accept_token" to token.value
        )
        val template = TemplateBuilder
            .fromResource("templates/email/invitation.html")
            .translate("i18n/core/email", actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(placeholders))
            .build()

        if (userService.existsByEmail(email) || invitationProperties.allowUnregisteredUsers) {
            emailService.sendEmail(email, subject, template, actualLocale)
        }

        return invitation
    }

    suspend fun accept(tokenValue: String): InvitationDocument {
        logger.debug { "Validating invitation" }

        val token = try {
            invitationTokenService.extract(tokenValue)
        } catch (e: Exception) {
            throw InvalidInvitationException(cause = e)
        }

        val invitation = findByIdOrNull(token.invitationId)
            ?: throw DocumentNotFoundException("No invitation with ID ${token.invitationId} found")

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

    suspend fun <T: ContentDocument<T>> deleteInvitationById(id: ObjectId) {
        val invitation = findById(id)
        val contentType = invitation.sensitive.claims["contentType"] as? String
            ?: throw InvalidDocumentException("No content type found in invitation")
        val key = invitation.sensitive.claims["key"] as? String
            ?: throw InvalidDocumentException("No key found in invitation")
        applicationContext.findContentManagementService(contentType)
            .deleteInvitation(key, id)
        deleteById(id)
    }

    private suspend fun removeExpired() {
        logger.debug { "Removing expired invitations" }

        val query = Query(Criteria.where(EncryptedInvitationDocument::expiresAt.name).lte(Instant.now()))

        reactiveMongoTemplate.remove(query, EncryptedInvitationDocument::class.java).awaitLast()
    }

    @Scheduled(cron = "0 */15 * * * *")
    private suspend fun scheduledCleanup() {
        removeExpired()
    }
}
