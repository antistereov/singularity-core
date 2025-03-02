package io.stereov.web.global.service.mail.exception

import io.stereov.web.global.exception.BaseWebException


open class MailException(message: String, cause: Throwable? = null) : BaseWebException(message, cause)
