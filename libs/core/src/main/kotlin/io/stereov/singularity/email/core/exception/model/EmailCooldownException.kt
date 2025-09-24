package io.stereov.singularity.email.core.exception.model

import io.stereov.singularity.email.core.exception.EmailException

/**
 * # Mail cooldown exception.
 *
 * This exception is thrown when a user tries to send an email before the cooldown period has expired.
 * It indicates that the user must wait for a specified amount of time before sending another email.
 * It extends the [EmailException] class.
 *
 * @param remainingCooldown The remaining cooldown time in seconds.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class EmailCooldownException(remainingCooldown: Long) : EmailException(
    message = "Please wait $remainingCooldown seconds before requesting another email")
