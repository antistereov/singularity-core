package io.stereov.singularity.database.hash.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.stereov.singularity.database.hash.exception.HashException
import io.stereov.singularity.database.hash.model.Hash
import io.stereov.singularity.database.hash.model.SearchableHash
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * A service responsible for hashing and verifying data using different algorithms such as BCrypt and HMAC-SHA256.
 *
 * This service provides methods to generate secure hashes and verify input strings against those hashes.
 * It supports password hashing using BCrypt and searchable hashing using HMAC-SHA256.
 *
 * @property hashSecretService A service for managing secrets used in HMAC-based hashing.
 */
@Service
class HashService(
    private val hashSecretService: HashSecretService,
) {

    /**
     * Verifies the given input against a BCrypt hash.
     *
     * This method checks if the provided input string matches the stored BCrypt hash.
     *
     * @param input The input string to be checked against the hash.
     * @param hash The [Hash] object containing the hashed data to validate against.
     * @return A [Result] containing `true` if the input matches the hash, or a [HashException.Hashing]
     *         if an error occurs during the validation process.
     */
    fun checkBcrypt(input: String, hash: Hash): Result<Boolean, HashException.Hashing> {
        return runCatching { BCrypt.checkpw(input, hash.data) }
            .mapError { ex -> HashException.Hashing("Failed to check hash: ${ex.message}", ex) }
    }

    /**
     * Hashes the given input string using the BCrypt hashing algorithm.
     *
     * This method generates a secured hash for the provided input using the BCrypt algorithm
     * with the default salt strength of 10.
     * The resulting hash is encapsulated in a [Hash] object.
     *
     * @param input The input string to be hashed.
     * @return A [Result] containing the hashed data as a [Hash] object.
     *   If an error occurs during the hashing process, a [HashException.Hashing] is returned.
     */
    fun hashBcrypt(input: String): Result<Hash, HashException.Hashing> {
        return runCatching { Hash(BCrypt.hashpw(input, BCrypt.gensalt(10))) }
            .mapError { ex -> HashException.Hashing("Failed to create hash: ${ex.message}", ex) }
    }

    /**
     * Hashes the given input string using HMAC-SHA256 and returns a [SearchableHash] containing
     * the resulting hash and its associated secret identifier. The method ensures that the input
     * string is normalized (trimmed and converted to lowercase) before processing.
     *
     * @param input The input string to be hashed. This value will be normalized prior to hashing.
     * @return A [Result] containing a [SearchableHash] on success or a [HashException]
     *         in case of errors during secret retrieval, hashing, or encoding.
     */
    suspend fun hashSearchableHmacSha256(input: String): Result<SearchableHash, HashException> = coroutineBinding {
        val normalized = input.trim().lowercase()

        val secret = hashSecretService.getCurrentSecret()
            .mapError { ex -> HashException.Secret("Failed to generate current hash secret: ${ex.message}", ex) }
            .bind()
        val secretKeyBytes = runCatching { Base64.getDecoder().decode(secret.value) }
            .mapError { ex -> HashException.Encoding("Failed to decode hash secret: ${ex.message}", ex) }
            .bind()

        val mac = runCatching {
            val keySpec = SecretKeySpec(secretKeyBytes, "HmacSHA256")
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(keySpec)
            mac
        }
            .mapError { ex -> HashException.Hashing("Failed to initialize hashing algorithm: ${ex.message}", ex) }
            .bind()


        val hmac = runCatching { mac.doFinal(normalized.toByteArray(Charsets.UTF_8)) }
            .mapError { ex -> HashException.Hashing("Failed to hash input: ${ex.message}", ex) }
            .bind()
        val encoded = runCatching { Base64.getUrlEncoder().withoutPadding().encodeToString(hmac) }
            .mapError { ex -> HashException.Encoding("Failed to encode hash: ${ex.message}", ex) }
            .bind()

        SearchableHash(encoded, secret.id)
    }
}
