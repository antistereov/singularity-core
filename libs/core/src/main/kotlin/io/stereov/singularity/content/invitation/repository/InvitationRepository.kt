package io.stereov.singularity.content.invitation.repository

import io.stereov.singularity.content.invitation.model.EncryptedInvitation
import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InvitationRepository : SensitiveCrudRepository<EncryptedInvitation>
