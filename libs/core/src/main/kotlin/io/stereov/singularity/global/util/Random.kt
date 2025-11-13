package io.stereov.singularity.global.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import java.security.SecureRandom
import kotlin.math.pow
import kotlin.random.Random

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
        fun generateString(length: Int = 10): Result<String, IllegalArgumentException> {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

            return runCatching {
                (1..length)
                    .map { chars[random.nextInt(chars.length)] }
                    .joinToString("")
            }.mapError { ex -> IllegalArgumentException("Bound must be positive", ex) }
        }

        fun generateInt(length: Int = 6): String {
            val upperBound = 10.0.pow(length).toInt()

            val number = Random.nextInt(upperBound)
            return String.format("%06d", number)
        }
    }

}
