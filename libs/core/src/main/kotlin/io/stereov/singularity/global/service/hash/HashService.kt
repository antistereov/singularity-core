package io.stereov.singularity.global.service.hash

import io.stereov.singularity.global.service.hash.model.SearchableHash
import io.stereov.singularity.global.service.hash.model.SecureHash
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*

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
class HashService {

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

    fun hashSha256(input: String): SearchableHash {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return SearchableHash(Base64.getUrlEncoder().withoutPadding().encodeToString(hash))
    }
}
