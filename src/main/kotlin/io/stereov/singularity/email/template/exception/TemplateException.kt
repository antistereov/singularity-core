package io.stereov.singularity.email.template.exception

import io.stereov.singularity.global.exception.SingularityException

/**
 * Represents a base class for template-related exceptions.
 *
 * This sealed class extends [SingularityException] and serves as the base for all exceptions related to
 * template processing or resource handling. It provides a unified way to handle various types
 * of errors that can occur during template management, such as missing resources or translation bundles.
 *
 * @param msg A message describing the details of the error.
 * @param code A code representing the specific type of the exception.
 * @param cause The underlying cause of the exception, if any.
 */
sealed class TemplateException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    /**
     * Exception indicating that a required template resource could not be found.
     *
     * This exception is typically used when a template resource specified for loading or processing
     * is not available or cannot be accessed. It extends the [TemplateException] class, providing
     * additional context specific to resource-related errors.
     *
     * @param msg A message describing the details of the missing resource.
     * @param cause The underlying cause of the exception, if any.
     */
    class ResourceNotFound(msg: String, cause: Throwable? = null) : TemplateException(msg, CODE, cause) {
        companion object { const val CODE = "TEMPLATE_RESOURCE_NOT_FOUND" }
    }

    /**
     * Exception indicating that a translation bundle could not be found.
     *
     * This exception is typically thrown when an attempt is made to load a translation resource bundle,
     * but the resource cannot be located. It extends the [TemplateException] class,
     * providing additional context specific to translation-related errors.
     *
     * @param msg A message describing the details of the missing translation bundle.
     * @param cause The underlying cause of the exception, if any.
     */
    class TranslationBundleNotFound(msg: String, cause: Throwable? = null) : TemplateException(msg, CODE, cause) {
        companion object { const val CODE = "TRANSLATION_BUNDLE_NOT_FOUND" }
    }
}
