package io.stereov.singularity.principal.core.repository

import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import io.stereov.singularity.principal.core.model.encrypted.EncryptedGuest

interface GuestRepository : SensitiveCrudRepository<EncryptedGuest>
