package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.model.EncryptedSensitiveDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Represents an encrypted invitation document stored in the "invitations" collection.
 *
 * This data class captures the details of an invitation, including its issuance time, expiration time,
 * and sensitive data that is encrypted for security purposes. The sensitive content is represented as
 * an [Encrypted] object wrapping [SensitiveInvitationData]. This allows secure storage and transmission
 * of details such as the recipient's email address and associated claims.
 *
 * The class implements the [EncryptedSensitiveDocument] interface, which provides a structure for
 * documents containing encrypted sensitive information.
 *
 * @property _id The unique identifier for the invitation document in the database.
 * @property issuedAt The timestamp when the invitation was issued. Defaults to the current time.
 * @property expiresAt The timestamp when the invitation expires.
 * @property sensitive The encrypted sensitive data of the invitation, containing the recipient's email
 * and claims, stored securely as an `Encrypted<SensitiveInvitationData>`.
 */
@Document(collection = "invitations")
data class EncryptedInvitation(
    override val _id: ObjectId? = null,
    val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant,
    override val sensitive: Encrypted<SensitiveInvitationData>
) : EncryptedSensitiveDocument<SensitiveInvitationData>
