package io.stereov.singularity.content.invitation.repository

import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import io.stereov.singularity.content.invitation.model.EncryptedInvitationDocument
import org.springframework.stereotype.Repository

@Repository
interface InvitationRepository : SensitiveCrudRepository<EncryptedInvitationDocument>
