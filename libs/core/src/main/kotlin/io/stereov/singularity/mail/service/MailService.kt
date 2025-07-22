package io.stereov.singularity.mail.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.translate.model.Language

interface MailService {

    val logger: KLogger

    suspend fun sendEmail(to: String, subject: String, content: String, lang: Language)
}
