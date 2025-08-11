package io.stereov.singularity.database.encryption.model

data class Encrypted<T>(
    val secretKey: String,
    val ciphertext: String
)
