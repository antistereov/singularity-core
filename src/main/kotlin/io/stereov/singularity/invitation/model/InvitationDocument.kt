package io.stereov.singularity.invitation.model

import io.stereov.singularity.global.database.model.EncryptedSensitiveDocument
import io.stereov.singularity.global.database.model.SensitiveDocument
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.encryption.model.Encrypted
import io.stereov.singularity.invitation.dto.InvitationResponse
import org.bson.types.ObjectId
import java.beans.Transient
import java.time.Instant

data class InvitationDocument(
    private var _id: ObjectId? = null,
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
