package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.database.core.model.EncryptedSensitiveDocument
import io.stereov.singularity.database.core.model.SensitiveDocument
import io.stereov.singularity.database.encryption.model.Encrypted
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "invitations")
data class EncryptedInvitationDocument(
    override val _id: ObjectId? = null,
    val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant,
    override val sensitive: Encrypted<SensitiveInvitationData>
) : EncryptedSensitiveDocument<SensitiveInvitationData> {

    override fun toSensitiveDocument(
        decrypted: SensitiveInvitationData,
        otherValues: List<Any?>
    ): SensitiveDocument<SensitiveInvitationData> {
        return InvitationDocument(
            _id,
            issuedAt,
            expiresAt,
            decrypted
        )
    }
}
