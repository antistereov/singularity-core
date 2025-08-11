package io.stereov.singularity.mail.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.content.translate.model.Language

interface MailService {

    val logger: KLogger

    suspend fun sendEmail(to: String, subject: String, content: String, lang: Language)
}
