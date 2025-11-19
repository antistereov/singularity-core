package io.stereov.singularity.email.core.service

import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.email.core.exception.EmailException
import jakarta.mail.internet.MimeMessage
import java.util.*

interface EmailService {

    val logger: KLogger

    suspend fun sendEmail(to: String, subject: String, content: String, locale: Locale): Result<MimeMessage, EmailException>
}
