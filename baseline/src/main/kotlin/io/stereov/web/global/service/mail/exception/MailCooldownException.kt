package io.stereov.web.global.service.mail.exception

class MailCooldownException(remainingCooldown: Long, cause: Throwable? = null) : MailException(
    message = "Please wait $remainingCooldown seconds before requesting another email", cause)
