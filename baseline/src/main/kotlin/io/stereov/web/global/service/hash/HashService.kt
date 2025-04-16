package io.stereov.web.global.service.hash

import io.stereov.web.global.service.hash.model.HashedField
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service

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
    fun checkBcrypt(input: String, hash: HashedField): Boolean {
        return BCrypt.checkpw(input, hash.data)
    }

    /**
     * Hashes the provided input using BCrypt.
     *
     * @param input The input string to hash.
     *
     * @return The hashed string.
     */
    fun hashBcrypt(input: String): HashedField {
        return HashedField(BCrypt.hashpw(input, BCrypt.gensalt(10)))
    }
}
