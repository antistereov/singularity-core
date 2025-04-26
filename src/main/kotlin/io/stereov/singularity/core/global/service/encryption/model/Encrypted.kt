package io.stereov.singularity.core.global.service.encryption.model

import java.util.*

data class Encrypted<T>(
    val secretId: UUID,
    val ciphertext: String
)
