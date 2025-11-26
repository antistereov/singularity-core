package io.stereov.singularity.user.core.repository

import io.stereov.singularity.database.encryption.repository.SensitiveCrudRepository
import io.stereov.singularity.user.core.model.encrypted.EncryptedGuest

interface GuestRepository : SensitiveCrudRepository<EncryptedGuest>
