package io.stereov.singularity.translate.exception

import io.stereov.singularity.global.exception.SingularityException

/**
 * Base class for exceptions related to translation operations.
 *
 * This sealed class serves as the parent for all exceptions that occur during translation
 * processes. It provides a standardized way to represent translation-related errors.
 *
 * @param msg The detail message explaining the exception.
 * @param code The error code associated with the exception.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class TranslateException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Exception indicating that no translations are available for a given request.
     *
     * This exception is thrown when a translation process fails to find any suitable
     * translations for the requested key, locale, or language.
     *
     * @param msg The detail message explaining the cause of the exception.
     * @param cause The optional cause of the exception, providing additional context about the error.
     */
    class NoTranslations(msg: String, cause: Throwable? = null) : TranslateException(msg, CODE, cause) {
        companion object { const val CODE = "NO_TRANSLATIONS" }
    }
}