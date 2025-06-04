package io.stereov.singularity.core.invitation.repository

import io.stereov.singularity.core.global.database.repository.SensitiveCrudRepository
import io.stereov.singularity.core.invitation.model.EncryptedInvitationDocument
import org.springframework.stereotype.Repository

@Repository
interface InvitationRepository : SensitiveCrudRepository<EncryptedInvitationDocument>
