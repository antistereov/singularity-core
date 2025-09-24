package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.model.EncryptedSensitiveDocument
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "invitations")
data class EncryptedInvitationDocument(
    override val _id: ObjectId? = null,
    val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant,
    override val sensitive: Encrypted<SensitiveInvitationData>
) : EncryptedSensitiveDocument<SensitiveInvitationData>
