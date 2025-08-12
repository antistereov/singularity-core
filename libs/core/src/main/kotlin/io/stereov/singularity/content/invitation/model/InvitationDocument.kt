package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.content.invitation.dto.InvitationResponse
import io.stereov.singularity.database.core.model.EncryptedSensitiveDocument
import io.stereov.singularity.database.core.model.SensitiveDocument
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.beans.Transient
import java.time.Instant

data class InvitationDocument(
    @Id private var _id: ObjectId? = null,
    val issuedAt: Instant = Instant.now(),
    val expiresAt: Instant,
    override val sensitive: SensitiveInvitationData,
) : SensitiveDocument<SensitiveInvitationData> {

    override fun toEncryptedDocument(
        encryptedSensitiveData: Encrypted<SensitiveInvitationData>,
        otherValues: List<Any>
    ): EncryptedSensitiveDocument<SensitiveInvitationData> {
        return EncryptedInvitationDocument(
            _id,
            issuedAt,
            expiresAt,
            encryptedSensitiveData
        )
    }

    fun toInvitationResponse(): InvitationResponse {
        return InvitationResponse(
            id,
            issuedAt,
            expiresAt,
            sensitive.email,
            sensitive.claims
        )
    }

    @get: Transient
    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("No ID found in InvitationDocument")
}
