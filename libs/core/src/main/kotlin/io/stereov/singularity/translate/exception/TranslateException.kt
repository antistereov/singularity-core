package io.stereov.singularity.translate.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a custom exception used for errors related to the translation process.
 *
 * This is a sealed class, extending [SingularityException], which provides additional context
 * specific to translation failures such as error codes, HTTP statuses, and detailed descriptions.
 *
 * @param msg The detailed error message explaining the exception.
 * @param code A unique code representing the specific type of translation error.
 * @param status The associated HTTP status indicating the error category.
 * @param description A detailed description providing additional context about the error.
 * @param cause The underlying cause of the exception, if applicable.
 */
sealed class TranslateException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception indicating that no translations are available for a given request.
     *
     * This exception is thrown when a translation process fails to find any suitable
     * translations for the requested key, locale, or language.
     *
     * Extends [TranslateException].
     *
     * @param msg The detail message explaining the cause of the exception.
     * @param cause The optional cause of the exception, providing additional context about the error.
     *
     * @property code `NO_TRANSLATIONS`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class NoTranslations(msg: String, cause: Throwable? = null) : TranslateException(
        msg,
        CODE,
        STATUS,
        DESCRIPTION,
        cause
    ) {
        companion object {
            const val CODE = "NO_TRANSLATIONS"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
            const val DESCRIPTION = "Exception indicating that no translations are available for a given request."
        }

    }
}