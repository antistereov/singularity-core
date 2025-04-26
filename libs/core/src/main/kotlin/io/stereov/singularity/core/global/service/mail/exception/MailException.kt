package io.stereov.singularity.core.global.service.mail.exception

import io.stereov.singularity.core.global.exception.BaseWebException

/**
 * # Mail exception.
 *
 * This exception is thrown when there is an error related to mail operations.
 * It extends the [BaseWebException] class.
 *
 * @param message The error message.
 * @param cause The cause of the exception (optional).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
open class MailException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
