package io.stereov.singularity.global.util

import java.security.SecureRandom

class Random {
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
