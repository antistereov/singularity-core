package io.stereov.singularity.invitation.repository

import io.stereov.singularity.global.database.repository.SensitiveCrudRepository
import io.stereov.singularity.invitation.model.EncryptedInvitationDocument
import org.springframework.stereotype.Repository

@Repository
interface InvitationRepository : SensitiveCrudRepository<EncryptedInvitationDocument>
