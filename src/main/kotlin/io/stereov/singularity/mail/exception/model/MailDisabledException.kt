package io.stereov.singularity.mail.exception.model

import io.stereov.singularity.mail.exception.MailException

class MailDisabledException : MailException(message = "Action cannot be performed: mail is disabled in configuration")
