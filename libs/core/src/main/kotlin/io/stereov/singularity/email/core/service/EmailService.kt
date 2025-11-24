package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.email.core.exception.EmailException
import jakarta.mail.internet.MimeMessage
import java.util.*

/**
 * Defines the contract for sending emails in the application.
 *
 * This interface provides a mechanism to send emails with specific content, subjects, and localization support.
 *
 * The service is conditionally loaded based on the `singularity.email.enable` property in the
 * application configuration. When this property is set to `true`, the service [EnabledEmailService] will be used and ready
 * to handle email operations. If set to `false`, the service is not loaded and [FailingEmailService] will be used.
 * If email is disabled in the application, [sendEmail] will always fail with [EmailException.Disabled].
 */
sealed interface EmailService {

    val logger: KLogger

    /**
     * Sends an email with the specified parameters.
     *
     * This method sends an email to a specific recipient with the provided subject, content,
     * and localization settings. Depending on the application configuration, it may either send
     * the email or throw an exception if email functionality is disabled or encounters an error.
     *
     * @param to The email address of the recipient.
     * @param subject The subject of the email.
     * @param content The content of the email body.
     * @param locale The locale used for translating and formatting the email content.
     * @return A [Result] containing the successfully sent [MimeMessage], or an [EmailException]
     *         indicating the specific error encountered during the operation.
     */
    suspend fun sendEmail(to: String, subject: String, content: String, locale: Locale): Result<MimeMessage, EmailException>
}
