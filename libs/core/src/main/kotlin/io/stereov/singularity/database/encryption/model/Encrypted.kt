package io.stereov.singularity.database.encryption.model

/**
 * Represents encrypted data comprising a ciphertext and a corresponding key used for decryption.
 *
 * This data class is generic and can handle various types of objects when encrypted. The `secretKey`
 * field holds the identifier for the specific key used during encryption. The `ciphertext` field contains
 * the Base64-encoded encrypted representation of the data.
 *
 * @param T The type of the original data being encrypted.
 * @property secretKey The identifier for the encryption key used for generating the ciphertext.
 * @property ciphertext The Base64-encoded encrypted representation of the data.
 */
data class Encrypted<T>(
    val secretKey: String,
    val ciphertext: String
)
