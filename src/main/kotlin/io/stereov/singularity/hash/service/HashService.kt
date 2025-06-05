package io.stereov.singularity.hash.service

import io.stereov.singularity.hash.model.SearchableHash
import io.stereov.singularity.hash.model.SecureHash
import io.stereov.singularity.secrets.service.HashSecretService
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * # HashService
 *
 * This service provides methods for hashing and checking passwords using the BCrypt algorithm.
 *
 * It includes methods for checking a password against a hash and for hashing a password.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class HashService(
    private val hashSecretService: HashSecretService
) {

    /**
     * Checks if the provided input matches the given hash using BCrypt.
     *
     * @param input The input string to check.
     * @param hash The hash to check against.
     *
     * @return True if the input matches the hash, false otherwise.
     */
    fun checkBcrypt(input: String, hash: SecureHash): Boolean {
        return BCrypt.checkpw(input, hash.data)
    }

    /**
     * Hashes the provided input using BCrypt.
     *
     * @param input The input string to hash.
     *
     * @return The hashed string.
     */
    fun hashBcrypt(input: String): SecureHash {
        return SecureHash(BCrypt.hashpw(input, BCrypt.gensalt(10)))
    }

    suspend fun hashSearchableHmacSha256(input: String): SearchableHash {
        val normalized = input.trim().lowercase()

        val secret = hashSecretService.getCurrentSecret()
        val secretKeyBytes = Base64.getDecoder().decode(secret.value)

        val keySpec = SecretKeySpec(secretKeyBytes, "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)

        val hmac = mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(hmac)

        return SearchableHash(encoded)
    }
}
