package io.stereov.singularity.content.invitation.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.content.invitation.exception.AcceptInvitationException
import io.stereov.singularity.content.invitation.exception.DeleteInvitationByIdException
import io.stereov.singularity.content.invitation.exception.InviteException
import io.stereov.singularity.content.invitation.model.EncryptedInvitation
import io.stereov.singularity.content.invitation.model.Invitation
import io.stereov.singularity.content.invitation.model.InvitationToken
import io.stereov.singularity.content.invitation.model.SensitiveInvitationData
import io.stereov.singularity.content.invitation.properties.InvitationProperties
import io.stereov.singularity.content.invitation.repository.InvitationRepository
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.database.encryption.exception.FindEncryptedDocumentByIdException
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.email.template.util.TemplateBuilder
import io.stereov.singularity.email.template.util.build
import io.stereov.singularity.email.template.util.replacePlaceholders
import io.stereov.singularity.email.template.util.translate
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.service.TranslateService
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
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
) : SensitiveCrudService<SensitiveInvitationData, Invitation, EncryptedInvitation>() {

    override val encryptedDocumentClazz = EncryptedInvitation::class.java
    override val sensitiveClazz = SensitiveInvitationData::class.java
    override val logger = KotlinLogging.logger {}

    @Scheduled(cron = "0 */15 * * * *")
    private fun scheduledCleanup()  {
        runBlocking {
            runSuspendCatching { removeExpired() }
                .onFailure { ex -> logger.error(ex) { "Failed to remove expired invitations"} }
        }
    }

    override suspend fun doDecrypt(
        encrypted: EncryptedInvitation,
        decryptedSensitive: SensitiveInvitationData
    ): Result<Invitation, EncryptionException> {
        return Ok(Invitation(
            encrypted._id,
            encrypted.issuedAt,
            encrypted.expiresAt,
            decryptedSensitive
        ))
    }

    override suspend fun doEncrypt(
        document: Invitation,
        encryptedSensitive: Encrypted<SensitiveInvitationData>
    ): Result<EncryptedInvitation, EncryptionException> {
        return Ok(EncryptedInvitation(
            document._id,
            document.issuedAt,
            document.expiresAt,
            encryptedSensitive
        ))
    }

    /**
     * Sends an invitation email to a specified recipient with the provided details and claims.
     *
     * @param contentType The type of content associated with the invitation (e.g., "project", "team").
     * @param contentKey The unique identifier for the specific content the invitation is linked to.
     * @param email The email address of the recipient to whom the invitation will be sent.
     * @param inviterName The name of the person sending the invitation.
     * @param invitedTo A description of the entity or context for which the recipient is invited (e.g., "team name").
     * @param claims A map representing additional metadata to be included with the invitation.
     * @param issuedAt The timestamp when the invitation is issued. Defaults to the current instant.
     * @param expiresInSeconds The duration in seconds for which the invitation remains valid. Defaults to 1 week (60 * 60 * 24 * 7).
     * @param locale The locale used for localization of the email content. If null, a default application locale is used.
     * @return A [Result] object containing the created [Invitation] on success or an [InviteException] on failure.
     */
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
    ): Result<Invitation, InviteException> = coroutineBinding {
        logger.debug { "Inviting \"$email\" with claims: $claims" }

        val acceptUri = invitationProperties.acceptUri
            .replace("{contentType}", contentType)
            .replace("{contentKey}", contentKey)

        val actualLocale = locale ?: appProperties.locale

        val claimsWithContentType = claims.toMutableMap()
        claimsWithContentType["contentType"] = contentType

        val invitation = save(email, claimsWithContentType, issuedAt, expiresInSeconds)
            .mapError { ex -> InviteException.Database("Failed to save invitation: ${ex.message}", ex) }
            .bind()
        val token = invitationTokenService.create(invitation)
            .mapError { ex -> InviteException.InvitationTokenCreation("Failed to create invitation token: ${ex.message}", ex) }
            .bind()

        val subject = translateService.translateResourceKey(TranslateKey("invitation.subject"), "i18n/core/email", locale)

        val placeholders = mapOf(
            "inviter_name" to inviterName,
            "invited_to" to invitedTo,
            "expiration_days" to (expiresInSeconds / 60 / 60 / 24).toInt(),
            "accept_uri" to acceptUri,
            "accept_token" to token.value
        )
        val template = TemplateBuilder
            .fromResource("templates/email/invitation.html")
            .translate("i18n/core/email", actualLocale)
            .replacePlaceholders(templateService.getPlaceholders(placeholders))
            .build()
            .mapError { ex -> InviteException.Template("Failed to create invitation email template: ${ex.message}", ex) }
            .bind()

        val userExistsByEmail = userService.existsByEmail(email)
            .mapError { ex -> InviteException.Database("Failed to check existence of user with email $email: ${ex.message}", ex) }
            .bind()

        if (userExistsByEmail || invitationProperties.allowUnregisteredUsers) {
            emailService.sendEmail(email, subject, template, actualLocale)
                .mapError { InviteException.from(it) }
                .bind()
        }

        invitation
    }

    /**
     * Processes the provided invitation token to retrieve and validate the corresponding invitation.
     *
     * @param token The invitation token used to locate and validate the associated invitation.
     * @return A [Result] containing the validated [Invitation] if successful, or an [AcceptInvitationException] if an error occurs.
     */
    suspend fun accept(
        token: InvitationToken,
    ): Result<Invitation, AcceptInvitationException> {
        logger.debug { "Validating invitation" }

        return findById(token.invitationId)
            .mapError { when (it) {
                is FindEncryptedDocumentByIdException.NotFound ->
                    AcceptInvitationException.InvitationNotFound("No invitation found for token.")
                else -> AcceptInvitationException.Database("Failed to get invitation from database: ${it.message}")
            } }
    }

    /**
     * Saves an invitation with the provided email, claims, issuance time, and expiration duration.
     *
     * @param email The email address associated with the invitation.
     * @param claims A map of claims to associate with the invitation.
     * @param issuedAt The time at which the invitation is issued.
     * @param expiresInSeconds The duration in seconds after which the invitation will expire.
     * @return A [Result] containing the saved [Invitation] if successful, or a [SaveEncryptedDocumentException] if an error occurs.
     */
    suspend fun save(
        email: String,
        claims: Map<String, Any>,
        issuedAt: Instant,
        expiresInSeconds: Long,
    ): Result<Invitation, SaveEncryptedDocumentException> {

        val expiresAt = issuedAt.plusSeconds(expiresInSeconds)
        val data = SensitiveInvitationData(
            email = email,
            claims = claims
        )
        return save(
            Invitation(
                issuedAt = issuedAt,
                expiresAt = expiresAt,
                sensitive = data,
            )
        )
    }

    /**
     * Deletes an invitation by its unique identifier.
     *
     * @param id the ObjectId of the invitation to be deleted
     * @return A [Result] containing Unit if the operation is successful, or a [DeleteInvitationByIdException] if an error occurs
     */
    suspend fun <T: ContentDocument<T>> deleteInvitationById(
        id: ObjectId,
        authenticationOutcome: AuthenticationOutcome,
    ): Result<Unit, DeleteInvitationByIdException> = coroutineBinding {
        val invitation = findById(id)
            .mapError { when (it) {
                is FindEncryptedDocumentByIdException.NotFound -> DeleteInvitationByIdException.InvitationNotFound("No invitation found for ID $id")
                else -> DeleteInvitationByIdException.Database("Failed to get invitation from database: ${it.message}")
            } }
            .bind()
        val contentType = (invitation.sensitive.claims["contentType"] as? String)
            .toResultOr { DeleteInvitationByIdException.InvalidInvitation("No content type found in invitation") }
            .bind()
        val key = (invitation.sensitive.claims["key"] as? String)
            .toResultOr { DeleteInvitationByIdException.InvalidInvitation("No key found in invitation") }
            .bind()
        applicationContext.findContentManagementService(contentType)
            .mapError { ex -> DeleteInvitationByIdException.ContentTypeNotFound("Failed to find content management service for content type $contentType: ${ex.message}", ex)}
            .bind()
            .deleteInvitation(key, id, authenticationOutcome)
            .mapError { DeleteInvitationByIdException.from(it) }
            .bind()

        deleteById(id)
            .mapError { ex -> DeleteInvitationByIdException.Database("Failed to delete invitation from database: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Removes expired invitations from the database.
     *
     * This method uses a query to identify invitations that have expired based on their expiration date
     * and removes them from the database. Any errors encountered during the operation will be captured
     * and wrapped in a DatabaseException.Database instance.
     *
     * @return A [Result] containing Unit if the operation is successful, or a [DatabaseException].Database
     * if an error occurs during the removal process.
     */
    private suspend fun removeExpired(): Result<Unit, DatabaseException.Database> {
        logger.debug { "Removing expired invitations" }

        val query = Query(Criteria.where(EncryptedInvitation::expiresAt.name).lte(Instant.now()))

        return runSuspendCatching {
            reactiveMongoTemplate.remove(query, EncryptedInvitation::class.java).awaitLast()
            Unit
        }.mapError { ex -> DatabaseException.Database("Failed to remove expired invitations: ${ex.message}", ex) }
    }
}
