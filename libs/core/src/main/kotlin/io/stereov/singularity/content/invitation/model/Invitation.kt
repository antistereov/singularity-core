package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.database.encryption.model.SensitiveDocument
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.time.Instant

/**
 * Represents an invitation document with associated sensitive data.
 *
 * This data class defines the structure of an invitation, including information about its issuance
 * and expiration, along with sensitive details that are securely handled. The sensitive data is
 * encapsulated in the form of [SensitiveInvitationData], which includes information like email and
 * claims associated with the invitation. The class implements the [SensitiveDocument] interface,
 * providing a standard structure for documents with sensitive content.
 *
 * @property _id The unique identifier for the invitation document in the database.
 * @property issuedAt The timestamp indicating when the invitation was issued. Defaults to the current time.
 * @property expiresAt The timestamp indicating when the invitation expires.
 * @property sensitive The sensitive data associated with the invitation, including the recipient's
 * email and related claims.
 */
data class Invitation(
    @Id override var _id: ObjectId? = null,
    val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant,
    override val sensitive: SensitiveInvitationData,
) : SensitiveDocument<SensitiveInvitationData>
