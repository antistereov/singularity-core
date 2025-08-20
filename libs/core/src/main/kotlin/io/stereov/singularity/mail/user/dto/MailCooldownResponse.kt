package io.stereov.singularity.mail.user.dto

/**
 * # Mail cooldown response.
 *
 * This data class represents the response for a mail cooldown request.
 * It contains the remaining time in seconds until the next mail can be sent.
 *
 * @property remaining The remaining time in seconds until the next mail can be sent.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class MailCooldownResponse(
    val remaining: Long,
)
