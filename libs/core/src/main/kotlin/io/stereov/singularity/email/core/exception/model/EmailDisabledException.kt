package io.stereov.singularity.email.core.exception.model

import io.stereov.singularity.email.core.exception.EmailException

class EmailDisabledException : EmailException(message = "Action cannot be performed: email is disabled in configuration")
