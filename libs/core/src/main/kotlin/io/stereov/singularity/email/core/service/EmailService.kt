package io.stereov.singularity.email.core.service

import io.github.oshai.kotlinlogging.KLogger
import java.util.*

interface EmailService {

    val logger: KLogger

    suspend fun sendEmail(to: String, subject: String, content: String, locale: Locale)
}
