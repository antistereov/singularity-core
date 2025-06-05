package io.stereov.singularity.global.service.encryption.model

import java.util.*

data class Encrypted<T>(
    val secretId: UUID,
    val ciphertext: String
)
