package io.stereov.web.global.service.mail.exception

class MailVerificationCooldownException(remainingCooldown: Long, cause: Throwable? = null) : MailException(
    message = "Please wait $remainingCooldown seconds before requesting another verification email", cause)
