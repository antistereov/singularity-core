package io.stereov.singularity.secrets.core.exception.model

import io.stereov.singularity.secrets.core.exception.SecretsException
import io.stereov.singularity.secrets.core.service.SecretService

class NoCurrentKeyException(clazz: Class<out SecretService>) : SecretsException(msg = "No current key set for ${clazz.simpleName}.")
