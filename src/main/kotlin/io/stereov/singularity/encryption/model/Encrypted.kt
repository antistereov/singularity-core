package io.stereov.singularity.encryption.model

import java.util.*

data class Encrypted<T>(
    val secretId: UUID,
    val ciphertext: String
)
