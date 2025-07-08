package io.stereov.singularity.encryption.model

data class Encrypted<T>(
    val secretKey: String,
    val ciphertext: String
)
