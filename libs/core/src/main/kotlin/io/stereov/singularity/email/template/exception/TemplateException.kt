package io.stereov.singularity.email.template.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

/**
 * Represents a base exception class for template-related errors in the application.
 *
 * This sealed class extends [SingularityException], providing a foundational structure
 * for more specific exceptions that occur when working with email templates. It includes
 * additional context like an error code, HTTP status, and a detailed description, enabling
 * systematic handling of template errors.
 *
 * @param msg The error message describing the exception.
 * @param code A unique code identifying the type of template error.
 * @param status The HTTP status associated with the error.
 * @param description A detailed description of the error.
 * @param cause The underlying cause of the exception, if available.
 */
sealed class TemplateException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Exception indicating that a required template resource could not be found.
     *
     * This exception extends [TemplateException].
     *
     * @param msg A message describing the details of the missing resource.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `TEMPLATE_RESOURCE_NOT_FOUND`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class ResourceNotFound(msg: String, cause: Throwable? = null) : TemplateException(
        msg,
        "TEMPLATE_RESOURCE_NOT_FOUND",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception indicating that a required template resource could not be found.",
        cause
    )

    /**
     * Exception indicating that a translation bundle could not be found.
     *
     * This exception extends [TemplateException].
     *
     * @param msg A message describing the details of the missing translation bundle.
     * @param cause The underlying cause of the exception, if any.
     *
     * @property code `TRANSLATION_BUNDLE_NOT_FOUND`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class TranslationBundleNotFound(msg: String, cause: Throwable? = null) : TemplateException(
        msg,
        "TRANSLATION_BUNDLE_NOT_FOUND",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Exception indicating that a translation bundle could not be found.",
        cause
    )
}
