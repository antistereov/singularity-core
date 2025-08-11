package io.stereov.singularity.mail.core.exception.model

import io.stereov.singularity.mail.core.exception.MailException

class MailDisabledException : MailException(message = "Action cannot be performed: mail is disabled in configuration")
