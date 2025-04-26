package io.stereov.singularity.core.global.service.random

import java.security.SecureRandom

/**
 * # Service for generating random codes.
 *
 * This service provides methods for generating random codes
 * for various purposes, such as recovery codes.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class RandomService {
    companion object {
        private val random = SecureRandom()

        /**
         * Generates a random code of the specified length.
         *
         * @param length The length of the recovery code to generate.
         *
         * @return A string representing the generated recovery code.
         */
        fun generateCode(length: Int = 10): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            return (1..length)
                .map { chars[random.nextInt(chars.length)] }
                .joinToString("")
        }
    }

}
